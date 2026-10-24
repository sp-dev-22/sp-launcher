package com.example.util

import com.example.data.model.AlertItem
import com.example.data.model.FlashcardItem
import com.example.data.model.StudyCourse
import com.example.data.model.TaskItem
import com.example.data.model.allClassRefs
import java.util.Calendar

/** What a calendar dot represents. */
enum class CalendarEventType { DEADLINE, REMINDER, TASK_DUE, TASK_REMINDER, CARD }

/** One thing on a given day. [timeMillis] is null for all-day items (like a due date). */
data class CalendarEvent(
    val dayStartMillis: Long,
    val timeMillis: Long?,
    val type: CalendarEventType,
    val label: String,
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

    fun eventsFor(
        courses: List<StudyCourse>,
        tasks: List<TaskItem>,
        alerts: List<AlertItem>,
    ): List<CalendarEvent> {
        val out = mutableListOf<CalendarEvent>()

        for (course in courses) {
            for (ref in course.allClassRefs()) {
                ref.clazz.deadlineMillis?.let {
                    out.add(CalendarEvent(startOfDay(it), it, CalendarEventType.DEADLINE, ref.clazz.name))
                }
                ref.clazz.alertMillis?.let {
                    out.add(CalendarEvent(startOfDay(it), it, CalendarEventType.REMINDER, ref.clazz.name))
                }
            }
            for (subject in course.subjects) {
                for (topic in subject.topics) {
                    topic.deadlineMillis?.let {
                        out.add(CalendarEvent(startOfDay(it), it, CalendarEventType.DEADLINE, topic.name))
                    }
                    topic.alertMillis?.let {
                        out.add(CalendarEvent(startOfDay(it), it, CalendarEventType.REMINDER, topic.name))
                    }
                }
            }
        }

        for (task in tasks) {
            if (task.isCompleted) continue
            task.dueMillis?.let {
                out.add(CalendarEvent(startOfDay(it), null, CalendarEventType.TASK_DUE, task.text))
            }
            task.reminderMillis?.let {
                out.add(CalendarEvent(startOfDay(it), it, CalendarEventType.TASK_REMINDER, task.text))
            }
        }

        for (alert in alerts) {
            if (alert.archived) continue
            alert.reminderMillis?.let {
                out.add(CalendarEvent(startOfDay(it), it, CalendarEventType.REMINDER, alert.title))
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
