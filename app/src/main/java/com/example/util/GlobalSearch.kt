package com.example.util

import com.example.data.model.AlertItem
import com.example.data.model.AppInfo
import com.example.data.model.ContactEntry
import com.example.data.model.DigiBrainItem
import com.example.data.model.FlashcardItem
import com.example.data.model.StudyCourse
import com.example.data.model.TaskItem
import com.example.data.model.allClassRefs

enum class SearchKind { APP, TASK, NOTE, CLASS, CARD, CONTACT, BRAIN }

data class SearchHit(
    val kind: SearchKind,
    val title: String,
    val subtitle: String,
    val refId: String,
)

/** One search box over everything the app knows: apps, tasks, notes, study classes, cards. */
object GlobalSearch {

    fun search(
        query: String,
        apps: List<AppInfo>,
        tasks: List<TaskItem>,
        alerts: List<AlertItem>,
        courses: List<StudyCourse>,
        cards: List<FlashcardItem>,
        contacts: List<ContactEntry> = emptyList(),
        digiBrain: List<DigiBrainItem> = emptyList(),
        perKind: Int = 25,
    ): List<SearchHit> {
        val q = query.trim()
        if (q.isBlank()) return emptyList()
        val out = mutableListOf<SearchHit>()

        apps.filter { it.label.contains(q, ignoreCase = true) }.take(perKind)
            .forEach { out.add(SearchHit(SearchKind.APP, it.label, "App", it.packageName)) }

        // Contacts match on the name, or on the number when the query is digits.
        val queryDigits = PhoneNumbers.digits(q)
        contacts.filter { entry ->
            entry.name.contains(q, ignoreCase = true) ||
                (queryDigits.length >= 3 && PhoneNumbers.digits(entry.number).contains(queryDigits))
        }.take(perKind).forEach { entry ->
            out.add(SearchHit(SearchKind.CONTACT, entry.name, entry.number, entry.id))
        }

        tasks.filter { it.text.contains(q, ignoreCase = true) }.take(perKind)
            .forEach { out.add(SearchHit(SearchKind.TASK, it.text, if (it.isCompleted) "Task - done" else "Task", it.id)) }

        alerts.filter { !it.archived && (it.title.contains(q, true) || it.content.contains(q, true)) }.take(perKind)
            .forEach { out.add(SearchHit(SearchKind.NOTE, it.title, "Note", it.id)) }

        courses.flatMap { it.allClassRefs() }
            .filter { it.clazz.name.contains(q, true) || it.pathLabel.contains(q, true) }
            .take(perKind)
            .forEach { out.add(SearchHit(SearchKind.CLASS, it.clazz.name, it.pathLabel, it.clazz.id)) }

        cards.filter { it.front.contains(q, true) || it.back.contains(q, true) }.take(perKind)
            .forEach { out.add(SearchHit(SearchKind.CARD, it.front, if (it.deck.isBlank()) "Card" else "Card - ${it.deck}", it.id)) }

        // Digi Brain (v1.19.0): PUBLIC items only, and never anything in the
        // Trash. Private items live only inside the Digi Brain screen — search
        // must not leak them, so the filter lives here, at the one place every
        // search passes through.
        digiBrain.filter {
            it.deletedAtMillis == null && it.isPublic &&
                (it.title.contains(q, true) || it.content.contains(q, true))
        }.take(perKind).forEach {
            out.add(
                SearchHit(
                    SearchKind.BRAIN,
                    it.title.ifBlank { it.content },
                    if (it.kind == com.example.data.model.DigiBrainKind.LINK) "Digi Brain - Link" else "Digi Brain - Note",
                    it.id
                )
            )
        }

        return out
    }
}
