package com.tertiaryinfotech.tapcard.util

import android.net.Uri
import com.tertiaryinfotech.tapcard.model.DigitalCard
import com.tertiaryinfotech.tapcard.net.ApiConfig

/**
 * Phone + link helpers shared by the QR code, the WhatsApp action and the vCard.
 *
 * wa.me needs a digits-only *international* number — "96983731" alone makes
 * WhatsApp reply "the phone number shared via url is invalid". People type
 * local numbers on their cards, so every link goes through [internationalDigits]
 * with the account's default country code (Settings → Country code, synced with
 * the web). Keep in sync with the web's `lib/whatsapp.ts`.
 */
object PhoneNumber {

    const val DEFAULT_COUNTRY_CODE = "65"

    /** Dialling codes offered in Settings — same list as the web's `lib/countries.ts`. */
    val countries: List<Triple<String, String, String>> = listOf(
        Triple("65", "Singapore", "🇸🇬"), Triple("60", "Malaysia", "🇲🇾"),
        Triple("62", "Indonesia", "🇮🇩"), Triple("66", "Thailand", "🇹🇭"),
        Triple("63", "Philippines", "🇵🇭"), Triple("84", "Vietnam", "🇻🇳"),
        Triple("852", "Hong Kong", "🇭🇰"), Triple("86", "China", "🇨🇳"),
        Triple("886", "Taiwan", "🇹🇼"), Triple("81", "Japan", "🇯🇵"),
        Triple("82", "South Korea", "🇰🇷"), Triple("91", "India", "🇮🇳"),
        Triple("971", "United Arab Emirates", "🇦🇪"), Triple("61", "Australia", "🇦🇺"),
        Triple("44", "United Kingdom", "🇬🇧"), Triple("1", "United States / Canada", "🇺🇸"),
    )

    fun countryLabel(code: String): String =
        countries.firstOrNull { it.first == code }?.let { "${it.third} ${it.second} (+${it.first})" } ?: "+$code"

    /** National significant-number lengths for the dialling codes people pick most. */
    private val nationalLengths: Map<String, IntRange> = mapOf(
        "1" to 10..10, "44" to 9..10, "60" to 8..10, "61" to 9..9, "62" to 8..12,
        "63" to 10..10, "65" to 8..8, "66" to 8..9, "81" to 9..10, "82" to 8..10,
        "84" to 9..10, "86" to 11..11, "91" to 10..10, "852" to 8..8, "853" to 8..8,
        "886" to 9..9, "971" to 8..9,
    )

    /**
     * Digits-only international form: "9698 3731" → "6596983731",
     * "+60 12-345 6789" → "60123456789", "012-345 6789" with cc 60 → "60123456789".
     */
    fun internationalDigits(raw: String, countryCode: String = DEFAULT_COUNTRY_CODE): String {
        val trimmed = raw.trim()
        val digits = trimmed.filter(Char::isDigit)
        if (digits.isEmpty()) return ""
        if (trimmed.startsWith("+")) return digits
        if (digits.startsWith("00")) return digits.drop(2)
        val cc = countryCode.filter(Char::isDigit).ifEmpty { DEFAULT_COUNTRY_CODE }
        if (digits.startsWith("0")) return cc + digits.trimStart('0')
        val range = nationalLengths[cc] ?: 7..12
        if (digits.startsWith(cc) && (digits.length - cc.length) in range && digits.length !in range) {
            return digits
        }
        return cc + digits
    }

    /** "+6596983731" — for tel: links and vCards, so a contact saved abroad still dials. */
    fun international(raw: String, countryCode: String = DEFAULT_COUNTRY_CODE): String =
        internationalDigits(raw, countryCode).let { if (it.isEmpty()) "" else "+$it" }
}

/** What a card's QR code opens. Server values match `User.qrMode` on the web. */
enum class QrMode(val serverValue: String, val label: String) {
    WHATSAPP("WHATSAPP", "WhatsApp"),
    LINKEDIN("LINKEDIN", "LinkedIn"),
    CARD("CARD", "Card"),

    /** Offline vCard — works without a published card or internet. App-only. */
    CONTACT("CONTACT", "Contact");

    companion object {
        fun fromServer(value: String?): QrMode =
            entries.firstOrNull { it.serverValue == value && it != CONTACT } ?: WHATSAPP

        /** Modes the account-wide default can be set to (synced with the web). */
        val defaults = listOf(WHATSAPP, LINKEDIN, CARD)
    }
}

object CardLinks {

    /** The number a card is reached on: WhatsApp field, else mobile, else office line. */
    fun whatsappNumber(card: DigitalCard, countryCode: String): String? =
        listOf(card.whatsapp, card.phone, card.officePhone)
            .firstOrNull { it.isNotBlank() }
            ?.let { PhoneNumber.internationalDigits(it, countryCode) }
            ?.takeIf { it.isNotEmpty() }

    /** Direct wa.me chat with the card's owner (used by the in-app WhatsApp button). */
    fun whatsappChat(card: DigitalCard, countryCode: String): String? =
        whatsappNumber(card, countryCode)?.let { "https://wa.me/$it" }

    /**
     * The card's LinkedIn profile as a clean https URL, or null. Accepts a bare
     * handle, `in/handle`, `linkedin.com/in/…` or a full URL, and rejects anything
     * that isn't a linkedin.com profile so a QR can't be pointed elsewhere.
     */
    fun linkedInUrl(card: DigitalCard): String? {
        val raw = card.linkedin.trim()
        if (raw.isEmpty()) return null
        val lower = raw.lowercase()
        val normalized = when {
            lower.startsWith("http://") || lower.startsWith("https://") -> raw
            lower.startsWith("linkedin.com/") || lower.startsWith("www.linkedin.com/") -> "https://$raw"
            else -> {
                val handle = raw.removePrefix("in/").trim('/')
                if (handle.isEmpty() || handle.any { it == '/' || it == '?' || it.isWhitespace() }) return null
                "https://www.linkedin.com/in/$handle"
            }
        }
        val uri = runCatching { Uri.parse(normalized) }.getOrNull() ?: return null
        val host = uri.host?.lowercase() ?: return null
        if (host != "linkedin.com" && !host.endsWith(".linkedin.com")) return null
        val path = uri.path.orEmpty()
        if (!path.startsWith("/in/") || path.length <= 4 || uri.userInfo != null) return null
        return "https://www.linkedin.com$path"
    }

    fun publicUrl(card: DigitalCard): String? =
        card.slug.takeIf { it.isNotBlank() }?.let { "${ApiConfig.PUBLIC_WEB_URL}/c/$it" }

    /**
     * Which modes this card can actually serve. WhatsApp and Card need a published
     * card (the QR routes through the card page); LinkedIn needs a profile; the
     * offline vCard always works.
     */
    fun availableModes(card: DigitalCard, countryCode: String): List<QrMode> = buildList {
        val published = publicUrl(card) != null
        if (published && whatsappNumber(card, countryCode) != null) add(QrMode.WHATSAPP)
        if (linkedInUrl(card) != null) add(QrMode.LINKEDIN)
        if (published) add(QrMode.CARD)
        add(QrMode.CONTACT)
    }

    /** The preferred mode if the card supports it, otherwise the best available one. */
    fun resolveMode(card: DigitalCard, preferred: QrMode, countryCode: String): QrMode {
        val modes = availableModes(card, countryCode)
        return if (preferred in modes) preferred else modes.first()
    }

    /**
     * What the QR encodes. WhatsApp routes through the card page with `?wa=1`,
     * which hands off to wa.me on arrival (with the owner's country code applied
     * server-side). Encoding wa.me directly would be one hop shorter, but a
     * scanner without WhatsApp would be bounced to an app store and lose the
     * contact — via the card page they always land on something useful.
     */
    fun qrPayload(card: DigitalCard, mode: QrMode, countryCode: String): String {
        val url = publicUrl(card)
        val vcard by lazy { VCard.build(card, countryCode) }
        return when (mode) {
            QrMode.WHATSAPP -> url?.let { "$it?wa=1&src=qr" } ?: vcard
            QrMode.CARD -> url?.let { "$it?src=qr" } ?: vcard
            QrMode.LINKEDIN -> linkedInUrl(card) ?: vcard
            QrMode.CONTACT -> vcard
        }
    }

    fun caption(mode: QrMode): String = when (mode) {
        QrMode.WHATSAPP -> "Scan to chat with me on WhatsApp"
        QrMode.LINKEDIN -> "Scan to connect on LinkedIn"
        QrMode.CARD -> "Scan to open my card"
        QrMode.CONTACT -> "Scan to save my contact"
    }
}
