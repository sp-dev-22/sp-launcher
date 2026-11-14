package com.example.data.model

/** What a Digi Brain item holds: a text note, or a saved link (v1.19.0). */
enum class DigiBrainKind { NOTE, LINK }

/**
 * One saved thing in Digi Brain (v1.19.0): the launcher's small second brain.
 * Notes and links only — images and video were deliberately left out of v1.
 *
 * [deletedAtMillis] is null while the item is live; once set, the item is in
 * the Trash, where it warns at 25 days and is auto-deleted at 30 (see
 * [com.example.util.DigiBrain]). [lastOpenedMillis] drives the "Aaj ka ek"
 * resurface: an item unopened for 7+ days is picked to show again.
 */
data class DigiBrainItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    val kind: DigiBrainKind,
    /** Note: the first line, or the title the user gave it. Link: the shared title, or the domain. */
    val title: String,
    /** Note: the full text. Link: the URL. */
    val content: String,
    /** True means "show on widget" — and the only items SP Assistant search may show. Default off. */
    val isPublic: Boolean = false,
    /** Pinned items always come first in the widget carousel. */
    val pinned: Boolean = false,
    val createdAtMillis: Long = System.currentTimeMillis(),
    /** Last time the user opened this item. Null means never opened since saving. */
    val lastOpenedMillis: Long? = null,
    /** When the item was moved to the Trash. Null means live. */
    val deletedAtMillis: Long? = null
)
