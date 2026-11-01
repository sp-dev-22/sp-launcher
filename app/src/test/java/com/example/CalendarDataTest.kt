package com.example

import com.example.data.model.StudyClass
import com.example.data.model.StudyCourse
import com.example.data.model.StudySubject
import com.example.data.model.StudyTopic
import com.example.data.model.TaskItem
import com.example.util.CalendarData
import com.example.util.CalendarEventKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The calendar feed: what gets listed, what stays hidden, and what a tap should open.
 */
class CalendarDataTest {

    private val day = CalendarData.startOfDay(1_700_000_000_000L)
    private val later = CalendarData.addDays(day, 2)

    private fun task(completed: Boolean, dueMillis: Long?, id: String = "t1") =
        TaskItem(id = id, text = "Revise chapter 4", isCompleted = completed, dueMillis = dueMillis)

    @Test
    fun `a completed task stays on the calendar`() {
        val events = CalendarData.eventsFor(emptyList(), listOf(task(true, day + 3_600_000L)), emptyList())

        assertEquals(1, events.size)
        assertTrue(events[0].isCompleted)
        assertEquals(CalendarEventKind.TASK, events[0].kind)
        assertEquals("Revise chapter 4", events[0].label)
    }

    @Test
    fun `a pending task is not marked complete`() {
        val events = CalendarData.eventsFor(emptyList(), listOf(task(false, day + 3_600_000L)), emptyList())
        assertEquals(1, events.size)
        assertFalse(events[0].isCompleted)
    }

    @Test
    fun `a cleared task disappears from that day`() {
        val item = task(true, day + 3_600_000L)
        val key = CalendarData.clearKey("task", item.id, day)

        val events = CalendarData.eventsFor(emptyList(), listOf(item), emptyList(), setOf(key))
        assertTrue(events.isEmpty())
    }

    @Test
    fun `a clear key is scoped to one day, so moving the date brings the row back`() {
        val item = task(true, later + 3_600_000L)
        // The user cleared the row on its old day.
        val staleKey = CalendarData.clearKey("task", item.id, day)

        val events = CalendarData.eventsFor(emptyList(), listOf(item), emptyList(), setOf(staleKey))
        assertEquals(1, events.size)
    }

    @Test
    fun `a pending study class appears and opens the study manager`() {
        val events = CalendarData.eventsFor(listOf(course(isCompleted = false, deadline = day + 7_200_000L)), emptyList(), emptyList())

        assertEquals(1, events.size)
        assertEquals(CalendarEventKind.STUDY, events[0].kind)
        assertEquals("Fundamental rights", events[0].label)
        assertFalse(events[0].isCompleted)
    }

    @Test
    fun `a completed study class has no deadline left, so it never appears`() {
        // Completing a class clears its deadline and reminder, so there is nothing to list.
        val events = CalendarData.eventsFor(listOf(course(isCompleted = true, deadline = null)), emptyList(), emptyList())
        assertTrue(events.isEmpty())
    }

    @Test
    fun `only checklist tasks are clearable`() {
        val taskEvents = CalendarData.eventsFor(emptyList(), listOf(task(true, day + 1000L)), emptyList())
        assertTrue(taskEvents[0].clearKey.isNotBlank())

        val studyEvents = CalendarData.eventsFor(listOf(course(isCompleted = false, deadline = day + 1000L)), emptyList(), emptyList())
        assertTrue(studyEvents[0].clearKey.isBlank())
    }

    private fun course(isCompleted: Boolean, deadline: Long?) = StudyCourse(
        name = "RRB",
        subjects = listOf(
            StudySubject(
                name = "GK",
                topics = listOf(
                    StudyTopic(
                        name = "Polity",
                        classes = listOf(
                            StudyClass(
                                name = "Fundamental rights",
                                isCompleted = isCompleted,
                                deadlineMillis = deadline
                            )
                        )
                    )
                )
            )
        )
    )

    @Test
    fun `day helpers stay consistent`() {
        assertEquals(day, CalendarData.startOfDay(day))
        assertEquals(day, CalendarData.addDays(day, 0))
        assertEquals(later, CalendarData.addDays(day, 2))
        assertTrue(CalendarData.daysInMonth(day) in 28..31)
    }
}
