package com.example.data.model

/**
 * How a reminder repeats after it fires.
 *
 *  NONE   - one-shot: fires once and stays in the past.
 *  DAILY  - re-arms for the same time the next day.
 *  WEEKLY - re-arms for the same time the next week.
 */
enum class RepeatRule { NONE, DAILY, WEEKLY }
