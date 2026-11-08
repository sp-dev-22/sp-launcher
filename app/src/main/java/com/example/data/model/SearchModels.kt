package com.example.data.model

/** One row of the phone book: a name with one of its numbers (SP Assistant, v1.12). */
data class ContactEntry(
    val id: String,
    val name: String,
    /** The number exactly as saved, formatting and all. */
    val number: String
)

/** One file on the phone that matched a search (SP Assistant, v1.12). */
data class DeviceFile(
    val name: String,
    /** content:// uri as a string, ready to hand to an ACTION_VIEW intent. */
    val uri: String,
    val mimeType: String?,
    val sizeBytes: Long
)
