package com.tertiaryinfotech.tapcard.model

import kotlinx.serialization.Serializable

/**
 * A calendar appointment / booking — the Android twin of the web CRM's
 * Appointment model, synced to the backend.
 */
@Serializable
data class Appointment(
    val id: String = "",
    val name: String = "",
    val email: String? = null,
    val startAt: String = "", // ISO-8601 instant
    val endAt: String = "",   // ISO-8601 instant
    val status: String = "REQUESTED", // REQUESTED | CONFIRMED | CANCELLED | COMPLETED
    val notes: String? = null,
    val createdAt: String = "",
)

/** Appointment status options offered in the editor. */
val AppointmentStatuses = listOf("REQUESTED", "CONFIRMED", "CANCELLED", "COMPLETED")

/** "REQUESTED" -> "Requested". */
fun appointmentStatusLabel(status: String): String =
    status.lowercase().replaceFirstChar(Char::uppercase)
