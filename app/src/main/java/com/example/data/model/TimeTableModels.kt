package com.example.data.model

/** Per-subject planning inputs: how long one class runs, the rest that follows it, and how
 *  many sessions of that subject the plan should hold each day (1 = single, 2 = double,
 *  3 = triple). */
data class SubjectPlanSettings(
    val durationMinutes: Int = 45,
    val restMinutes: Int = 10,
    /** Sessions per day for this subject: 1 = single, 2 = double, 3 = triple. */
    val sessionsPerDay: Int = 1
)

/** One generated session — a class placed in a time slot, like a calendar event. */
data class TimeTableEntry(
    /** 0 = today, 1 = tomorrow, ... */
    val day: Int,
    /** Midnight of the day this entry belongs to (used for grouping / labels). */
    val dateMillis: Long,
    val courseId: String,
    val subjectId: String,
    val subjectName: String,
    val topicId: String,
    val classId: String,
    val className: String,
    /** Reminder time (slot start). */
    val startMillis: Long,
    /** Deadline time (slot end). */
    val endMillis: Long,
    /** True when this is a "free" placeholder slot (subject had no classes left). */
    val isFree: Boolean = false
)

/** The whole Quick Time Table: its settings and the plan it last generated. */
data class TimeTableSettings(
    /** Minutes from midnight, e.g. 9:00 -> 540. */
    val windowStartMinutes: Int = 9 * 60,
    val windowEndMinutes: Int = 21 * 60,
    /** How many days the plan should cover: 1, 3 or 7 (kept short on purpose). */
    val days: Int = 7,
    /** subjectId -> its duration / rest / sessions per day. */
    val subjectSettings: Map<String, SubjectPlanSettings> = emptyMap(),
    val entries: List<TimeTableEntry> = emptyList(),
    /** Class ids whose Reminder/Deadline were set by the timetable (so we can clear only these). */
    val generatedClassIds: List<String> = emptyList()
)
