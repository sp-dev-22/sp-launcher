package com.example.util

import com.example.data.model.AlertItem
import com.example.data.model.FlashcardItem
import com.example.data.model.StudyCourse
import com.example.data.model.TaskItem
import com.example.data.model.allClassRefs
import java.util.Calendar

/** What a calendar dot represents. */
enum class CalendarEventType { DEADLINE, REMINDER, TASK_DUE, TASK_REMINDER, CARD }

/** Which screen a row belongs to, so tapping it can open the right place. */
enum class CalendarEventKind { STUDY, TASK, NOTE, CARD }

/**
 * One thing on a given day. [timeMillis] is null for all-day items (like a due date).
 *
 * [clearKey] is the stable identity used by "remove from calendar", and only checklist tasks
 * carry one: the row is hidden while its key sits in the user's cleared set. It is day-scoped,
 * so moving the task to another date brings it back instead of leaving it hidden forever.
 */
data class CalendarEvent(
    val dayStartMillis: Long,
    val timeMillis: Long?,
    val type: CalendarEventType,
    val label: String,
    val isCompleted: Boolean = false,
    val clearKey: String = "",
    val kind: CalendarEventKind = CalendarEventKind.STUDY,
)

/**
 * Turns everything already dated in the app (study deadlines, note reminders, task due dates
 * and reminders, card review dates) into a flat list of day events for the in-app calendar.
 * Purely local — this is not Google Calendar.
 */
object CalendarData {

    fun startOfDay(millis: Long): Long = Calendar.getInstance().apply {
        timeInMillis = millis
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    fun startOfMonth(millis: Long): Long = Calendar.getInstance().apply {
        timeInMillis = millis
        set(Calendar.DAY_OF_MONTH, 1)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    fun addDays(millis: Long, delta: Int): Long =
        Calendar.getInstance().apply { timeInMillis = millis; add(Calendar.DAY_OF_YEAR, delta) }.timeInMillis

    fun addMonths(millis: Long, delta: Int): Long =
        Calendar.getInstance().apply { timeInMillis = millis; add(Calendar.MONTH, delta) }.timeInMillis

    /** 1 = Sunday .. 7 = Saturday. */
    fun dayOfWeekIndex(millis: Long): Int =
        Calendar.getInstance().apply { timeInMillis = millis }.get(Calendar.DAY_OF_WEEK)

    fun daysInMonth(millis: Long): Int =
        Calendar.getInstance().apply { timeInMillis = millis }.getActualMaximum(Calendar.DAY_OF_MONTH)

    /** The identity a row is remembered by: "<kind>:<id>:<dayStartMillis>". */
    fun clearKey(kind: String, id: String, dayStartMillis: Long): String = "$kind:$id:$dayStartMillis"

    fun eventsFor(
        courses: List<StudyCourse>,
        tasks: List<TaskItem>,
        alerts: List<AlertItem>,
        clearedKeys: Set<String> = emptySet(),
    ): List<CalendarEvent> {
        val out = mutableListOf<CalendarEvent>()

        fun add(event: CalendarEvent) {
            if (event.clearKey.isNotBlank() && clearedKeys.contains(event.clearKey)) return
            out.add(event)
        }

        // Study Manager rows are plain deadlines/reminders. Completing a class clears its
        // deadline and reminder, so a completed class never reaches this list — there is
        // nothing to strike through and nothing to clear.
        for (course in courses) {
            for (ref in course.allClassRefs()) {
                ref.clazz.deadlineMillis?.let { at ->
                    add(
                        CalendarEvent(
                            dayStartMillis = startOfDay(at),
                            timeMillis = at,
                            type = CalendarEventType.DEADLINE,
                            label = ref.clazz.name,
                            kind = CalendarEventKind.STUDY
                        )
                    )
                }
                ref.clazz.alertMillis?.let { at ->
                    add(
                        CalendarEvent(
                            dayStartMillis = startOfDay(at),
                            timeMillis = at,
                            type = CalendarEventType.REMINDER,
                            label = ref.clazz.name,
                            kind = CalendarEventKind.STUDY
                        )
                    )
                }
            }

            for (subject in course.subjects) {
                for (topic in subject.topics) {
                    topic.deadlineMillis?.let { at ->
                        add(
                            CalendarEvent(
                                dayStartMillis = startOfDay(at),
                                timeMillis = at,
                                type = CalendarEventType.DEADLINE,
                                label = topic.name,
                                kind = CalendarEventKind.STUDY
                            )
                        )
                    }
                    topic.alertMillis?.let { at ->
                        add(
                            CalendarEvent(
                                dayStartMillis = startOfDay(at),
                                timeMillis = at,
                                type = CalendarEventType.REMINDER,
                                label = topic.name,
                                kind = CalendarEventKind.STUDY
                            )
                        )
                    }
                }
            }
        }

        for (task in tasks) {
            // Checklist tasks are the one place completed items stay: a ticked task keeps its
            // due date, so it is listed struck through until the user clears it by hand.
            task.dueMillis?.let { at ->
                add(
                    CalendarEvent(
                        dayStartMillis = startOfDay(at),
                        timeMillis = null,
                        type = CalendarEventType.TASK_DUE,
                        label = task.text,
                        isCompleted = task.isCompleted,
                        clearKey = clearKey("task", task.id, startOfDay(at)),
                        kind = CalendarEventKind.TASK
                    )
                )
            }
            task.reminderMillis?.let { at ->
                add(
                    CalendarEvent(
                        dayStartMillis = startOfDay(at),
                        timeMillis = at,
                        type = CalendarEventType.TASK_REMINDER,
                        label = task.text,
                        isCompleted = task.isCompleted,
                        clearKey = clearKey("task", task.id, startOfDay(at)),
                        kind = CalendarEventKind.TASK
                    )
                )
            }
        }

        for (alert in alerts) {
            if (alert.archived) continue
            alert.reminderMillis?.let { at ->
                add(
                    CalendarEvent(
                        dayStartMillis = startOfDay(at),
                        timeMillis = at,
                        type = CalendarEventType.REMINDER,
                        label = alert.title,
                        kind = CalendarEventKind.NOTE
                    )
                )
            }
        }

        return out
    }

    /** How many cards are up for review on each day (day-start millis -> count). */
    fun cardDueCounts(cards: List<FlashcardItem>, now: Long = System.currentTimeMillis()): Map<Long, Int> {
        val today = startOfDay(now)
        val counts = mutableMapOf<Long, Int>()
        for (card in cards) {
            if (card.locked) continue
            val day = card.nextDueMillis?.let { startOfDay(it) } ?: today
            counts[day] = (counts[day] ?: 0) + 1
        }
        return counts
    }
}
