package com.example.util

import com.example.data.model.FlashcardItem
import com.example.data.model.StudyCourse
import com.example.data.model.TaskItem
import com.example.data.model.completedCount
import com.example.data.model.totalCount
import java.util.Calendar

data class DayStat(val dayStartMillis: Long, val completed: Int)

data class StatsSummary(
    val streakDays: Int,
    val tasksCompletedThisWeek: Int,
    val cardsMasteredThisWeek: Int,
    val tasksCompletedTotal: Int,
    val cardsMasteredTotal: Int,
    val studyCompleted: Int,
    val studyTotal: Int,
    val last7Days: List<DayStat>,
)

/** Local-only numbers for the Study Stats screen. Nothing leaves the device. */
object StudyStats {

    fun startOfDay(millis: Long): Long = Calendar.getInstance().apply {
        timeInMillis = millis
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    private fun addDays(millis: Long, delta: Int): Long =
        Calendar.getInstance().apply { timeInMillis = millis; add(Calendar.DAY_OF_YEAR, delta) }.timeInMillis

    fun summarize(
        courses: List<StudyCourse>,
        tasks: List<TaskItem>,
        cards: List<FlashcardItem>,
        now: Long = System.currentTimeMillis(),
    ): StatsSummary {
        val today = startOfDay(now)
        val days = (6 downTo 0).map { addDays(today, -it) } // oldest .. today

        val taskByDay = tasks.mapNotNull { t -> if (t.isCompleted) t.completedAtMillis else null }
            .groupingBy { startOfDay(it) }.eachCount()
        val cardByDay = cards.mapNotNull { it.masteredAtMillis }
            .groupingBy { startOfDay(it) }.eachCount()

        val last7 = days.map { d -> DayStat(d, (taskByDay[d] ?: 0) + (cardByDay[d] ?: 0)) }
        val weekSet = days.toSet()

        // Streak: consecutive days with at least one task completed or card mastered.
        // Today not counting yet does not break it - yesterday still counts.
        val active = taskByDay.keys + cardByDay.keys
        var cursor = if (active.contains(today)) today else addDays(today, -1)
        var streak = 0
        while (active.contains(cursor)) {
            streak++
            cursor = addDays(cursor, -1)
        }

        return StatsSummary(
            streakDays = streak,
            tasksCompletedThisWeek = taskByDay.filterKeys { it in weekSet }.values.sum(),
            cardsMasteredThisWeek = cardByDay.filterKeys { it in weekSet }.values.sum(),
            tasksCompletedTotal = tasks.count { it.isCompleted },
            cardsMasteredTotal = cards.count { it.locked },
            studyCompleted = courses.sumOf { it.completedCount() },
            studyTotal = courses.sumOf { it.totalCount() },
            last7Days = last7,
        )
    }
}
