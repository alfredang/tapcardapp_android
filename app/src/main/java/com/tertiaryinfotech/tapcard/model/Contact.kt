package com.tertiaryinfotech.tapcard.model

import kotlinx.serialization.Serializable

/**
 * A person you've saved — the Android twin of the web app's `Contact` model.
 * Populated either by hand ("Add contact") or by saving a captured [Lead].
 * Synced to the shared backend so it also appears in the web CRM.
 */
@Serializable
data class Contact(
    val id: String = "",
    val name: String = "",
    val company: String = "",
    val position: String = "",
    val email: String = "",
    val phone: String = "",
    val whatsapp: String = "",
    val address: String = "",
    val notes: String = "",
    val tags: String = "",
) {
    /** Best single-line label for list rows. */
    val displayName: String
        get() = name.ifBlank { company.ifBlank { email.ifBlank { "Unnamed contact" } } }

    /** Secondary line: "Position · Company" with whichever parts exist. */
    val subtitle: String
        get() = listOf(position, company).filter { it.isNotBlank() }.joinToString(" · ")
}
