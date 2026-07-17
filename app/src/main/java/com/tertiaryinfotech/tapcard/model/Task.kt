package com.tertiaryinfotech.tapcard.model

import kotlinx.serialization.Serializable

/** A to-do — the Android twin of the web CRM's Task model, synced to the backend. */
@Serializable
data class Task(
    val id: String = "",
    val title: String = "",
    val type: String = "FOLLOW_UP", // FOLLOW_UP | REMINDER | MEETING
    val status: String = "TODO",    // TODO | IN_PROGRESS | DONE
    val dueAt: String? = null,      // ISO-8601 instant, or null
    val contactId: String? = null,
    val createdAt: String = "",
) {
    val isDone: Boolean get() = status == "DONE"
}

/** Task type options offered in the editor. */
val TaskTypes = listOf("FOLLOW_UP", "REMINDER", "MEETING")

/** "FOLLOW_UP" -> "Follow up". */
fun taskTypeLabel(type: String): String =
    type.split("_").joinToString(" ") { it.lowercase().replaceFirstChar(Char::uppercase) }
