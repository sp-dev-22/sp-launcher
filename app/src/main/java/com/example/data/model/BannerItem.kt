package com.example.data.model

data class BannerItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    val title: String,
    val content: String,
    val category: String = "Note", // "Note", "Reminder", "Goal"
)
