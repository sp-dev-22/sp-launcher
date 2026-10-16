package com.example.util

import com.example.data.model.FlashcardItem
import org.json.JSONArray
import org.json.JSONObject

/**
 * Parses a card file (JSON) into cards.
 *
 * The file may be either a bare array of cards, or an object with a "cards" array:
 *
 *   { "deck": "RRB GK", "cards": [
 *       { "front": "Capital of India?", "back": "New Delhi" },
 *       { "front": "Largest planet in our solar system?",
 *         "options": ["Jupiter", "Saturn", "Earth", "Mars"], "correct": 1 }
 *   ] }
 *
 * Rules per card:
 *   - "front" (or "q"/"question") is the question — required.
 *   - "back"  (or "a"/"answer" when it is text) is the answer.
 *   - "options" (or "choices") is an optional list; it is shown on the front.
 *   - "correct" (1-based) picks the right option; the back then becomes "Answer: N) …".
 *   - With options + correct you can omit "back".
 */
object CardFileParser {

    fun parse(json: String, deckName: String): List<FlashcardItem> {
        val items = extractItems(json) ?: return emptyList()
        val out = mutableListOf<FlashcardItem>()
        for (i in 0 until items.length()) {
            val o = items.optJSONObject(i) ?: continue
            val card = toCard(o, deckName) ?: continue
            out.add(card)
        }
        return out
    }

    private fun extractItems(json: String): JSONArray? {
        val trimmed = json.trim()
        if (trimmed.isEmpty()) return null
        return try {
            when {
                trimmed.startsWith("[") -> JSONArray(trimmed)
                else -> {
                    val root = JSONObject(trimmed)
                    root.optJSONArray("cards")
                        ?: root.optJSONArray("items")
                        ?: root.optJSONArray("questions")
                }
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun toCard(o: JSONObject, deckName: String): FlashcardItem? {
        val question = firstString(o, "front", "q", "question")?.trim().orEmpty()
        if (question.isEmpty()) return null

        val options = firstArray(o, "options", "choices")
        val optionTexts = mutableListOf<String>()
        if (options != null) {
            for (i in 0 until options.length()) optionTexts.add(options.optString(i, "").trim())
        }

        // "correct" is 1-based; "answer" may be a number (index) or text.
        var correctIndex = -1
        o.optInt("correct", Int.MIN_VALUE).takeIf { it != Int.MIN_VALUE }?.let { correctIndex = it - 1 }
        if (correctIndex < 0 && o.has("answer")) {
            val a = o.opt("answer")
            if (a is Number) correctIndex = a.toInt() - 1
        }

        val backRaw = firstString(o, "back", "a")?.trim()
            ?: o.optString("answer", "").trim().takeIf { it.isNotEmpty() && o.opt("answer") !is Number }

        val front = buildString {
            append(question)
            if (optionTexts.isNotEmpty()) {
                append("\n")
                optionTexts.forEachIndexed { i, t -> append("\n${i + 1}) $t") }
            }
        }.trim()

        val back = when {
            optionTexts.isNotEmpty() && correctIndex in optionTexts.indices ->
                "Answer: ${correctIndex + 1}) ${optionTexts[correctIndex]}"
            !backRaw.isNullOrBlank() -> backRaw
            else -> return null
        }

        return FlashcardItem(front = front, back = back, deck = deckName)
    }

    private fun firstString(o: JSONObject, vararg keys: String): String? {
        for (k in keys) {
            if (o.has(k) && o.opt(k) is String) {
                val v = o.optString(k, "")
                if (v.isNotBlank()) return v
            }
        }
        return null
    }

    private fun firstArray(o: JSONObject, vararg keys: String): JSONArray? {
        for (k in keys) {
            o.optJSONArray(k)?.let { return it }
        }
        return null
    }
}
