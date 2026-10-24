package com.example.data.model

data class TaskItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    val text: String,
    val isCompleted: Boolean = false,
    /** Optional hard due date (epoch millis). Null means "no due date". */
    val dueMillis: Long? = null,
    /** Optional reminder notification time (epoch millis). Null means "no reminder". */
    val reminderMillis: Long? = null,
    /** How the reminder repeats once it fires. */
    val repeat: RepeatRule = RepeatRule.NONE,
    /** How important the task is - shown as a small chip on the Home widget. */
    val priority: TaskPriority = TaskPriority.NONE,
    /** When it was ticked complete. Drives the Study Stats screen. */
    val completedAtMillis: Long? = null,
)

/** Task importance. NONE hides the chip entirely. */
enum class TaskPriority { NONE, LOW, MEDIUM, HIGH }
