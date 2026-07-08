package com.tertiaryinfotech.tapcard.util

import com.tertiaryinfotech.tapcard.model.DigitalCard
import com.tertiaryinfotech.tapcard.net.ApiConfig

/**
 * Builds a branded email signature for a card — both an HTML version (for pasting
 * into Gmail / Outlook signature settings so it renders formatted) and a plain-text
 * fallback. Only populated fields are included.
 */
object EmailSignature {

    /** Public web-card link, or null for a local-only (unpublished) card. */
    private fun cardUrl(card: DigitalCard): String? =
        card.slug.takeIf { it.isNotBlank() }?.let { "${ApiConfig.PUBLIC_WEB_URL}/c/$it" }

    fun html(card: DigitalCard): String {
        val rows = StringBuilder()
        rows.append("""<tr><td style="font-weight:bold;font-size:16px;color:#14141F;">${esc(card.displayName)}</td></tr>""")
        if (card.subtitle.isNotBlank()) {
            rows.append("""<tr><td style="color:#6B6B80;padding-top:2px;">${esc(card.subtitle)}</td></tr>""")
        }
        val contact = buildList {
            if (card.phone.isNotBlank()) add("Mobile: ${esc(card.phone)}")
            if (card.email.isNotBlank()) add("""Email: <a href="mailto:${esc(card.email)}" style="color:#14141F;text-decoration:none;">${esc(card.email)}</a>""")
            if (card.website.isNotBlank()) add("""Web: <a href="${normalize(card.website)}" style="color:#14141F;text-decoration:none;">${esc(card.website)}</a>""")
        }.joinToString(" &nbsp;•&nbsp; ")
        if (contact.isNotBlank()) {
            rows.append("""<tr><td style="padding-top:6px;color:#6B6B80;font-size:13px;">$contact</td></tr>""")
        }
        cardUrl(card)?.let {
            rows.append("""<tr><td style="padding-top:8px;"><a href="$it" style="color:#7C5CFF;font-weight:bold;text-decoration:none;">View my digital card →</a></td></tr>""")
        }
        return """<table style="font-family:Arial,Helvetica,sans-serif;font-size:14px;border-collapse:collapse;">$rows</table>"""
    }

    fun plain(card: DigitalCard): String = buildString {
        appendLine(card.displayName)
        if (card.subtitle.isNotBlank()) appendLine(card.subtitle)
        if (card.phone.isNotBlank()) appendLine("Mobile: ${card.phone}")
        if (card.email.isNotBlank()) appendLine("Email: ${card.email}")
        if (card.website.isNotBlank()) appendLine("Web: ${card.website}")
        cardUrl(card)?.let { appendLine("Card: $it") }
    }.trim()

    private fun esc(s: String): String =
        s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")

    private fun normalize(url: String): String =
        if (url.startsWith("http", ignoreCase = true)) url else "https://$url"
}