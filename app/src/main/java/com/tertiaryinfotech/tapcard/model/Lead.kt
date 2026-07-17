package com.tertiaryinfotech.tapcard.model

import kotlinx.serialization.Serializable

/**
 * A lead captured from one of your public web cards — someone filled in the
 * "share your details" form. The Android twin of the web app's `Lead` model.
 * Read-only in the app: you either save it as a [Contact] or dismiss it.
 */
@Serializable
data class Lead(
    val id: String = "",
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val company: String = "",
    val message: String = "",
    val source: String = "card",
    val createdAt: String = "",
) {
    val displayName: String
        get() = name.ifBlank { email.ifBlank { "New lead" } }

    /** Turns this lead into a savable contact. */
    fun toContact(): Contact = Contact(
        name = name,
        company = company,
        email = email,
        phone = phone,
        notes = message,
    )
}
