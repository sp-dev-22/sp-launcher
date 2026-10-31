package com.example.util

import com.example.data.model.GoalItem
import com.example.data.model.GoalType
import java.util.Calendar

/**
 * All the goal arithmetic, deliberately free of Android types so it can be unit tested.
 *
 * The rules, in one place:
 * - a LIMITED goal is done when [GoalItem.currentValue] reaches [GoalItem.targetValue];
 * - tapping adds [GoalItem.stepValue] and never pushes a LIMITED goal past its target;
 * - a goal with `resetDaily` starts each local day at zero, while its lifetime total keeps
 *   growing.
 */
object GoalProgress {

    private const val DAY_MS = 86_400_000L

    /** The local calendar day [millis] falls on, as a day number. */
    fun epochDay(millis: Long): Long {
        val cal = Calendar.getInstance()
        cal.timeInMillis = millis
        val offset = cal.get(Calendar.ZONE_OFFSET) + cal.get(Calendar.DST_OFFSET)
        return Math.floorDiv(millis + offset, DAY_MS)
    }

    /** Applies a pending daily reset. Returns the goal unchanged when nothing is due. */
    fun normalized(goal: GoalItem, now: Long = System.currentTimeMillis()): GoalItem {
        if (!goal.resetDaily) return goal
        val today = epochDay(now)
        if (goal.lastResetEpochDay == today) return goal
        return goal.copy(currentValue = 0.0, lastResetEpochDay = today)
    }

    /** One tap: adds [GoalItem.stepValue] to the period and to the lifetime total. */
    fun tick(goal: GoalItem, now: Long = System.currentTimeMillis()): GoalItem {
        val g = normalized(goal, now)
        if (isComplete(g)) return g
        val step = stepOf(g)
        val next = g.currentValue + step
        val value = if (g.type == GoalType.LIMITED && g.targetValue > 0.0) {
            minOf(next, g.targetValue)
        } else {
            next
        }
        return g.copy(currentValue = value, totalValue = g.totalValue + step)
    }

    /** Takes one tap back — for a mis-tap on the home widget. */
    fun undo(goal: GoalItem, now: Long = System.currentTimeMillis()): GoalItem {
        val g = normalized(goal, now)
        val step = stepOf(g)
        return g.copy(
            currentValue = (g.currentValue - step).coerceAtLeast(0.0),
            totalValue = (g.totalValue - step).coerceAtLeast(0.0)
        )
    }

    fun isComplete(goal: GoalItem): Boolean =
        goal.type == GoalType.LIMITED && goal.targetValue > 0.0 && goal.currentValue >= goal.targetValue

    /** 0f..1f for the progress bar; UNLIMITED goals have no bar. */
    fun fraction(goal: GoalItem): Float = when {
        goal.type != GoalType.LIMITED -> 0f
        goal.targetValue <= 0.0 -> 0f
        else -> (goal.currentValue / goal.targetValue).coerceIn(0.0, 1.0).toFloat()
    }

    /** "Beginner" while the goal is running, "Pro" once it is done. */
    fun label(goal: GoalItem): String = if (isComplete(goal)) goal.endText else goal.startText

    /** What the tap button says: the user's own text, or "+1" style shorthand. */
    fun buttonLabel(goal: GoalItem): String =
        goal.stepLabel.ifBlank { "+" + formatValue(stepOf(goal)) }

    /** "12" for whole numbers, "12.5" otherwise. */
    fun formatValue(value: Double): String {
        if (value.isNaN() || value.isInfinite()) return "0"
        return if (value == Math.floor(value)) value.toLong().toString() else String.format("%.1f", value)
    }

    private fun stepOf(goal: GoalItem): Double = if (goal.stepValue <= 0.0) 1.0 else goal.stepValue
}
