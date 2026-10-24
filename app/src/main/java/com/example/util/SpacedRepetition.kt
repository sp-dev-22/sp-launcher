package com.example.util

import com.example.data.model.FlashcardItem

/**
 * The single place that decides what happens to a card after it is answered.
 * Shared by the Study Popups notification flow and the on-screen Practice mode, so both
 * grade a card exactly the same way.
 *
 * Correct -> mastered (locked).
 * Wrong   -> comes back after 1, 3, 5 and 7 days (repeatStage 1..4); after the last stage
 *            it is mastered too.
 */
object SpacedRepetition {

    private const val DAY_MS = 86_400_000L
    val REPEAT_DAYS = listOf(1, 3, 5, 7)

    fun apply(card: FlashcardItem, correct: Boolean, now: Long = System.currentTimeMillis()): FlashcardItem {
        return if (correct) {
            card.copy(locked = true, masteredAtMillis = now)
        } else {
            val stage = card.repeatStage + 1
            if (stage > REPEAT_DAYS.size) {
                card.copy(locked = true, repeatStage = stage, nextDueMillis = null)
            } else {
                card.copy(
                    repeatStage = stage,
                    nextDueMillis = now + REPEAT_DAYS[stage - 1] * DAY_MS
                )
            }
        }
    }

    /** A card is due when it has no scheduled date, or that date has passed. */
    fun isDue(card: FlashcardItem, now: Long = System.currentTimeMillis()): Boolean =
        card.nextDueMillis == null || card.nextDueMillis <= now
}
