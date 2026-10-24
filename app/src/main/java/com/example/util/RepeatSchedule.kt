package com.example.util

import com.example.data.model.RepeatRule
import java.util.Calendar

/** Computes the next fire time for a repeating reminder. */
object RepeatSchedule {

    /** One period after [fromMillis]; null for [RepeatRule.NONE]. */
    fun next(rule: RepeatRule, fromMillis: Long): Long? = when (rule) {
        RepeatRule.NONE -> null
        RepeatRule.DAILY -> add(fromMillis, Calendar.DAY_OF_YEAR, 1)
        RepeatRule.WEEKLY -> add(fromMillis, Calendar.DAY_OF_YEAR, 7)
    }

    private fun add(millis: Long, field: Int, amount: Int): Long =
        Calendar.getInstance().apply {
            timeInMillis = millis
            add(field, amount)
        }.timeInMillis
}
