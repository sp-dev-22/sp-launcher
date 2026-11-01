package com.example

import com.example.data.model.GoalItem
import com.example.data.model.GoalType
import com.example.util.GoalProgress
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The goal rules, checked without an emulator. [GoalProgress] is deliberately Android-free so
 * these run as plain JVM tests.
 */
class GoalProgressTest {

    private fun limited(
        current: Double = 0.0,
        target: Double = 100.0,
        step: Double = 10.0,
        resetDaily: Boolean = false,
        lastResetEpochDay: Long = 0L,
        total: Double = 0.0
    ) = GoalItem(
        title = "Water",
        type = GoalType.LIMITED,
        startText = "Beginner",
        endText = "Pro",
        stepValue = step,
        targetValue = target,
        currentValue = current,
        totalValue = total,
        resetDaily = resetDaily,
        lastResetEpochDay = lastResetEpochDay
    )

    private val now = 1_700_000_000_000L

    @Test
    fun `a tick adds one step to the value and the total`() {
        val goal = limited(current = 0.0, step = 10.0)
        val after = GoalProgress.tick(goal, now)
        assertEquals(10.0, after.currentValue, 0.001)
        assertEquals(10.0, after.totalValue, 0.001)
    }

    @Test
    fun `a limited goal never goes past its target`() {
        val goal = limited(current = 95.0, target = 100.0, step = 10.0)
        val after = GoalProgress.tick(goal, now)
        assertEquals(100.0, after.currentValue, 0.001)
        assertTrue(GoalProgress.isComplete(after))
    }

    @Test
    fun `the label switches from the start text to the end text`() {
        val running = limited(current = 50.0, target = 100.0)
        assertEquals("Beginner", GoalProgress.label(running))
        assertEquals("Pro", GoalProgress.label(limited(current = 100.0, target = 100.0)))
    }

    @Test
    fun `a finished goal ignores further ticks`() {
        val done = limited(current = 100.0, target = 100.0, total = 100.0)
        val after = GoalProgress.tick(done, now)
        assertEquals(done, after)
    }

    @Test
    fun `an unlimited goal keeps counting and has no bar`() {
        val goal = GoalItem(
            title = "Reading",
            type = GoalType.UNLIMITED,
            startText = "Beginner",
            stepValue = 1.0,
            targetValue = 0.0,
            currentValue = 5.0
        )
        val after = GoalProgress.tick(goal, now)
        assertEquals(6.0, after.currentValue, 0.001)
        assertFalse(GoalProgress.isComplete(after))
        assertEquals(0f, GoalProgress.fraction(after), 0.0001f)
    }

    @Test
    fun `a daily goal starts again on a new day but keeps its total`() {
        val yesterday = GoalProgress.epochDay(now) - 1
        val goal = limited(current = 30.0, total = 30.0, resetDaily = true, lastResetEpochDay = yesterday)

        val reset = GoalProgress.normalized(goal, now)
        assertEquals(0.0, reset.currentValue, 0.001)
        assertEquals(30.0, reset.totalValue, 0.001)

        val afterTick = GoalProgress.tick(goal, now)
        assertEquals(10.0, afterTick.currentValue, 0.001)
        assertEquals(40.0, afterTick.totalValue, 0.001)
    }

    @Test
    fun `a daily goal does not reset twice in the same day`() {
        val goal = limited(current = 30.0, resetDaily = true, lastResetEpochDay = GoalProgress.epochDay(now))
        assertEquals(30.0, GoalProgress.normalized(goal, now).currentValue, 0.001)
    }

    @Test
    fun `undo takes exactly one step back`() {
        val goal = limited(current = 30.0, total = 30.0, step = 10.0)
        val after = GoalProgress.undo(goal, now)
        assertEquals(20.0, after.currentValue, 0.001)
        assertEquals(20.0, after.totalValue, 0.001)
    }

    @Test
    fun `undo never goes below zero`() {
        val goal = limited(current = 0.0, total = 0.0, step = 10.0)
        val after = GoalProgress.undo(goal, now)
        assertEquals(0.0, after.currentValue, 0.001)
    }

    @Test
    fun `whole numbers print without a decimal point`() {
        assertEquals("12", GoalProgress.formatValue(12.0))
        assertEquals("12.5", GoalProgress.formatValue(12.5))
    }

    @Test
    fun `the button label falls back to the step value`() {
        assertEquals("+10", GoalProgress.buttonLabel(limited(step = 10.0)))
        assertEquals("100 Liter", GoalProgress.buttonLabel(limited(step = 10.0).copy(stepLabel = "100 Liter")))
    }
}
