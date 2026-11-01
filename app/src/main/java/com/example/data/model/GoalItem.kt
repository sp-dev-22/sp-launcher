package com.example.data.model

import java.util.UUID

/** How a goal measures itself. */
enum class GoalType { LIMITED, UNLIMITED }

/**
 * A regular activity the user wants to keep up — drink water, revise, walk.
 *
 * A [GoalType.LIMITED] goal has a target and finishes: the label walks from [startText] to
 * [endText] as the value fills up. A [GoalType.UNLIMITED] goal has no target; it just keeps
 * counting and only ever shows [startText].
 *
 * [currentValue] is the progress of the current period, and is reset every day when
 * [resetDaily] is on. [totalValue] is everything ever logged and never resets.
 */
data class GoalItem(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val type: GoalType = GoalType.LIMITED,
    val startText: String = "Beginner",
    val endText: String = "Pro",
    /** How much one tap adds. */
    val stepValue: Double = 1.0,
    /** Text on the tap button, e.g. "100 Liter". Blank means "+<stepValue>". */
    val stepLabel: String = "",
    /** Only meaningful for LIMITED goals. */
    val targetValue: Double = 100.0,
    val currentValue: Double = 0.0,
    val totalValue: Double = 0.0,
    val resetDaily: Boolean = false,
    /** Local day number of the last daily reset; 0 means never. */
    val lastResetEpochDay: Long = 0L,
    val colorIndex: Int = 0,
    val createdAtMillis: Long = System.currentTimeMillis()
)
