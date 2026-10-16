package com.example.data.model

/**
 * One study card. A card can hold a flashcard, an MCQ, or a one-liner — they are all just
 * a front and a back. When marked wrong it comes back after 1, 3, 5 and 7 days
 * (repeatStage 1..4); once those are done it is mastered ([locked] = true).
 */
data class FlashcardItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    val front: String,
    val back: String,
    val locked: Boolean = false,
    /** 0 = new; 1..4 = waiting to repeat after 1/3/5/7 days; 5 = mastered. */
    val repeatStage: Int = 0,
    val nextDueMillis: Long? = null,
    /** Where the card came from (file / deck name), shown in the setup screen. */
    val deck: String = ""
)

/** Study Popups schedule. */
data class StudyPopupSettings(
    val enabled: Boolean = false,
    /** Minutes from midnight, e.g. 9:00 -> 540. */
    val startMinutes: Int = 9 * 60,
    val stopMinutes: Int = 21 * 60,
    /** Gap between two cards, in minutes. */
    val intervalMinutes: Int = 10
)
