package com.example.data.model

/**
 * A home-screen Alert: a short title, some details, an optional custom tag and an
 * optional reminder.
 *
 * When [reminderMillis] is set a notification fires at that time. There are no
 * built-in/default tags any more — [category] is whatever the user types (blank
 * means "no tag").
 */
data class AlertItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    val title: String,
    val content: String,
    /** Optional custom tag. Blank means "no tag". */
    val category: String = "",
    /** Optional reminder time (epoch millis). Null means no reminder. */
    val reminderMillis: Long? = null,
)
