package com.example.data.model

data class AppUsageLimit(
    val packageName: String,
    val limitMinutes: Int,
    val isEnabled: Boolean = true
)

data class AppUsageStat(
    val packageName: String,
    val minutesUsedToday: Int,
    val limitMinutes: Int = 0,
    val isLimitExceeded: Boolean = false
)
