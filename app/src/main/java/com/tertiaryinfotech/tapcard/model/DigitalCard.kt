package com.tertiaryinfotech.tapcard.model

import kotlinx.serialization.Serializable
import java.util.UUID

/**
 * A digital business card. Fields mirror the web app's Card model so the two
 * stay in sync. Stored locally as JSON (see CardStore) and synced to the backend
 * when signed in.
 */
@Serializable
data class DigitalCard(
    val id: String = UUID.randomUUID().toString(),

    // Identity
    val name: String = "",        // -> fullName
    val title: String = "",       // -> jobTitle
    val company: String = "",
    val department: String = "",
    val tagline: String = "",
    val bio: String = "",
    val about: String = "",

    // Contact
    val phone: String = "",       // -> mobile
    val officePhone: String = "",
    val whatsapp: String = "",
    val email: String = "",
    val website: String = "",
    val address: String = "",

    // Social
    val linkedin: String = "",
    val facebook: String = "",
    val instagram: String = "",
    val tiktok: String = "",
    val twitter: String = "",
    val telegram: String = "",
    val youtube: String = "",

    // Media (URLs, matching the web app)
    val profilePhoto: String = "",
    val companyLogo: String = "",
    val coverBanner: String = "",

    // Design
    val theme: String = "CORPORATE",
    /** ARGB accent for the card face; defaults to the Corporate blue. */
    val accentColor: Long = 0xFF2563EB,

    /** Public web-card slug once synced to the backend (empty for local-only cards). */
    val slug: String = "",
    val createdAtEpoch: Long = 0L,
) {
    /** True when there's nothing meaningful to show/share. */
    val isBlank: Boolean
        get() = listOf(name, title, company, phone, email, website, address).all { it.isBlank() }

    /** Best single-line label for list rows. */
    val displayName: String
        get() = name.ifBlank { company.ifBlank { email.ifBlank { "Untitled card" } } }

    /** Secondary line: "Title · Company" with whichever parts exist. */
    val subtitle: String
        get() = listOf(title, company).filter { it.isNotBlank() }.joinToString(" · ")

    /** All populated social links as (label, url) pairs. */
    val socialLinks: List<Pair<String, String>>
        get() = listOf(
            "LinkedIn" to linkedin,
            "Instagram" to instagram,
            "X" to twitter,
            "Facebook" to facebook,
            "TikTok" to tiktok,
            "Telegram" to telegram,
            "YouTube" to youtube,
        ).filter { it.second.isNotBlank() }

    /**
     * Converts a scanned person's card into a saveable [Contact]. The Contact model
     * has no social fields, so website + socials fold into notes.
     */
    fun toContact(): Contact {
        val extras = buildList {
            if (website.isNotBlank()) add("Website: $website")
            socialLinks.forEach { (label, url) -> add("$label: $url") }
        }.joinToString("\n")
        return Contact(
            name = name,
            company = company,
            position = title,
            email = email,
            phone = phone,
            whatsapp = whatsapp,
            address = address,
            notes = extras,
        )
    }
}