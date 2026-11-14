package com.example.util

import com.example.data.model.DigiBrainItem
import java.net.URI

/** How the Digi Brain mood face feels, from the days since it was last used. */
enum class DigiBrainMood { HAPPY, OK, SAD, VERY_SAD }

/**
 * Digi Brain's pure rules (v1.19.0) — no Android state in here, so the numbers
 * stay in one place: Trash warns at 25 days and auto-deletes at 30, the mood
 * face turns sad at 3 days and sadder at 7, and "Aaj ka ek" resurfaces a
 * public item unopened for 7+ days.
 */
object DigiBrain {

    const val DAY_MS: Long = 24L * 60L * 60L * 1000L

    /** Days in the Trash before the widget's trash icon turns neon red. */
    const val TRASH_WARN_DAYS: Int = 25

    /** Days in the Trash before the item is auto-deleted for good. */
    const val TRASH_EXPIRY_DAYS: Int = 30

    /** Days of not opening an item before it becomes eligible for "Aaj ka ek". */
    const val RESURFACE_AFTER_DAYS: Int = 7

    /** Days since last use before the mood face turns sad, then sadder. */
    const val MOOD_SAD_DAYS: Int = 3
    const val MOOD_VERY_SAD_DAYS: Int = 7

    /** Whole days between [fromMillis] and [nowMillis] (never negative). */
    fun daysBetween(fromMillis: Long, nowMillis: Long): Int {
        val diff = nowMillis - fromMillis
        if (diff <= 0L) return 0
        return (diff / DAY_MS).toInt()
    }

    /** Whole days this item has sat in the Trash (0 when live). */
    fun daysInTrash(item: DigiBrainItem, nowMillis: Long): Int {
        val deletedAt = item.deletedAtMillis ?: return 0
        return daysBetween(deletedAt, nowMillis)
    }

    /** True in the warning window: 25-29 days in the Trash. */
    fun isExpiringSoon(item: DigiBrainItem, nowMillis: Long): Boolean {
        val days = daysInTrash(item, nowMillis)
        return days in TRASH_WARN_DAYS until TRASH_EXPIRY_DAYS
    }

    /** True at 30+ days in the Trash — time to delete for good. */
    fun isExpired(item: DigiBrainItem, nowMillis: Long): Boolean =
        daysInTrash(item, nowMillis) >= TRASH_EXPIRY_DAYS

    /** Whole days left before this trashed item is auto-deleted (0 when live or expired). */
    fun daysLeftInTrash(item: DigiBrainItem, nowMillis: Long): Int {
        val deletedAt = item.deletedAtMillis ?: return 0
        val left = TRASH_EXPIRY_DAYS - daysBetween(deletedAt, nowMillis)
        return left.coerceIn(0, TRASH_EXPIRY_DAYS)
    }

    /**
     * The mood from the last use. Never used yet (0) reads as happy — a new
     * brain has nothing to miss. Used today: happy. 1-2 days: ok. 3-6: sad.
     * 7+: very sad.
     */
    fun moodFor(lastUseMillis: Long, nowMillis: Long): DigiBrainMood {
        if (lastUseMillis <= 0L) return DigiBrainMood.HAPPY
        return when (daysBetween(lastUseMillis, nowMillis)) {
            0 -> DigiBrainMood.HAPPY
            1, 2 -> DigiBrainMood.OK
            in 3 until MOOD_VERY_SAD_DAYS -> DigiBrainMood.SAD
            else -> DigiBrainMood.VERY_SAD
        }
    }

    /**
     * "Aaj ka ek": the stalest public, live item unopened for 7+ days — the
     * one most worth showing again. Never-opened items count from when they
     * were saved. Null when nothing qualifies. Deterministic for a given list,
     * so the pick stays put all day without stored state.
     */
    fun pickResurface(items: List<DigiBrainItem>, nowMillis: Long): DigiBrainItem? {
        return items
            .filter { it.deletedAtMillis == null && it.isPublic }
            .filter {
                val lastSeen = it.lastOpenedMillis ?: it.createdAtMillis
                daysBetween(lastSeen, nowMillis) >= RESURFACE_AFTER_DAYS
            }
            .minByOrNull { it.lastOpenedMillis ?: it.createdAtMillis }
    }

    /** The widget carousel order: pinned first, then newest first. Live public items only. */
    fun widgetOrder(items: List<DigiBrainItem>): List<DigiBrainItem> {
        return items
            .filter { it.deletedAtMillis == null && it.isPublic }
            .sortedWith(
                compareByDescending<DigiBrainItem> { it.pinned }
                    .thenByDescending { it.createdAtMillis }
            )
    }

    /** Normalises a URL so the same link saved twice is recognised as a duplicate. */
    fun normalizeUrl(raw: String): String {
        var url = raw.trim()
        if (!url.contains("://")) url = "https://$url"
        url = url.lowercase()
        while (url.endsWith("/")) url = url.dropLast(1)
        return url
    }

    /** The host of a URL for display ("example.com"), falling back to the raw text. */
    fun domainOf(raw: String): String {
        return try {
            val host = URI(normalizeUrl(raw)).host ?: return raw.trim()
            host.removePrefix("www.")
        } catch (_: Exception) {
            raw.trim()
        }
    }

    /** True when [text] looks like a bare URL (no scheme needed). */
    fun looksLikeUrl(text: String): Boolean {
        val t = text.trim()
        if (t.contains(" ") || t.contains("\n")) return false
        if (t.startsWith("http://") || t.startsWith("https://")) return t.length > "https://".length
        // Bare domain: something like example.com/path — a dot, no spaces, a TLD of 2+ letters.
        return Regex("^[A-Za-z0-9-]+(\\.[A-Za-z0-9-]+)+(:\\d+)?(/\\S*)?$").matches(t) &&
            t.substringAfterLast(".").substringBefore("/").length >= 2
    }

    /** Finds the first URL inside shared text, if any. */
    fun extractUrl(text: String): String? {
        val match = Regex("https?://\\S+").find(text) ?: return null
        return match.value.trimEnd('.', ',', ')', ']', '"', '\'')
    }

    /** An auto title from note text: the first line, trimmed to [maxLength]. */
    fun titleFromText(text: String, maxLength: Int = 60): String {
        val firstLine = text.trim().lineSequence().firstOrNull().orEmpty()
        return if (firstLine.length <= maxLength) firstLine
        else firstLine.take(maxLength).trimEnd() + "…"
    }

    /** The first [maxLines] lines of note text, for the widget card preview. */
    fun previewLines(text: String, maxLines: Int = 2): String {
        return text.trim().lineSequence().take(maxLines).joinToString("\n")
    }
}
