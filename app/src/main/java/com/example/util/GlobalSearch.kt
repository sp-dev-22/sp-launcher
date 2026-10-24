package com.example.util

import com.example.data.model.AlertItem
import com.example.data.model.AppInfo
import com.example.data.model.FlashcardItem
import com.example.data.model.StudyCourse
import com.example.data.model.TaskItem
import com.example.data.model.allClassRefs

enum class SearchKind { APP, TASK, NOTE, CLASS, CARD }

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
        perKind: Int = 25,
    ): List<SearchHit> {
        val q = query.trim()
        if (q.isBlank()) return emptyList()
        val out = mutableListOf<SearchHit>()

        apps.filter { it.label.contains(q, ignoreCase = true) }.take(perKind)
            .forEach { out.add(SearchHit(SearchKind.APP, it.label, "App", it.packageName)) }

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

        return out
    }
}
