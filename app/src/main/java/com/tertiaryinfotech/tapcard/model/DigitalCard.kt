package com.tertiaryinfotech.tapcard.model

import kotlinx.serialization.Serializable
import java.util.UUID

/**
 * A digital business card. Fields mirror the structured contact the iOS app
 * extracts from a scanned card. Stored locally as JSON (see CardStore).
 */
@Serializable
data class DigitalCard(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val title: String = "",
    val company: String = "",
    val phone: String = "",
    val email: String = "",
    val website: String = "",
    val address: String = "",
    /** ARGB accent for the card face; defaults to the Tapcard blue. */
    val accentColor: Long = 0xFF2563EB,
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
}
