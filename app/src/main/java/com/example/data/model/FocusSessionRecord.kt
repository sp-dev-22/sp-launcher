package com.example.data.model

data class FocusSessionRecord(
    val id: String = java.util.UUID.randomUUID().toString(),
    val timestamp: Long = System.currentTimeMillis(),
    val durationMinutes: Int,
    val sessionType: String = "Focus Session",
    val dateFormatted: String,
    val timeFormatted: String
)
