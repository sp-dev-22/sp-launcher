package com.example.data.model

import java.util.UUID

/**
 * Study Manager hierarchy: Course > Subject > Topic > Class.
 *
 * A [StudyClass] is the leaf and is itself the checkable todo item (tapping it
 * marks it complete). Both a [StudyTopic] and a [StudyClass] may optionally carry
 * a deadline and a reminder (alert) time.
 */
data class StudyClass(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val isCompleted: Boolean = false,
    /** Hard deadline. After this passes without completion the task shows as MISSED. */
    val deadlineMillis: Long? = null,
    /** Simple "you still have to complete this" reminder time. */
    val alertMillis: Long? = null,
    val completedAtMillis: Long? = null,
    /** Total focus minutes logged against this class (Study <-> Focus link). */
    val studiedMinutes: Int = 0
)

data class StudyTopic(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val classes: List<StudyClass> = emptyList(),
    val deadlineMillis: Long? = null,
    val alertMillis: Long? = null,
    /** Total focus minutes logged against this topic (and its classes). */
    val studiedMinutes: Int = 0
)

data class StudySubject(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val topics: List<StudyTopic> = emptyList()
)

data class StudyCourse(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val subjects: List<StudySubject> = emptyList()
)

/** Colour coding used across the tree and the progress graph. */
enum class StudyStatus { COMPLETED, MISSED, PENDING }

fun StudyClass.status(now: Long = System.currentTimeMillis()): StudyStatus = when {
    isCompleted -> StudyStatus.COMPLETED
    deadlineMillis != null && now > deadlineMillis -> StudyStatus.MISSED
    else -> StudyStatus.PENDING
}

/** A fully-qualified reference to a leaf class, handy for graphs, exports and reminders. */
data class StudyClassRef(
    val course: StudyCourse,
    val subject: StudySubject,
    val topic: StudyTopic,
    val clazz: StudyClass
) {
    val pathLabel: String get() = "${course.name} > ${subject.name} > ${topic.name} > ${clazz.name}"
}

fun StudyCourse.allClassRefs(): List<StudyClassRef> =
    subjects.flatMap { s -> s.topics.flatMap { t -> t.classes.map { c -> StudyClassRef(this, s, t, c) } } }

fun StudyTopic.totalCount(): Int = classes.size
fun StudyTopic.completedCount(): Int = classes.count { it.isCompleted }
fun StudySubject.totalCount(): Int = topics.sumOf { it.totalCount() }
fun StudySubject.completedCount(): Int = topics.sumOf { it.completedCount() }
fun StudyCourse.totalCount(): Int = subjects.sumOf { it.totalCount() }
fun StudyCourse.completedCount(): Int = subjects.sumOf { it.completedCount() }
fun StudyCourse.totalStudiedMinutes(): Int =
    subjects.sumOf { s -> s.topics.sumOf { it.studiedMinutes } }

/** A study class that is due today or already overdue (drives the Home "Due today" card). */
data class StudyDueItem(
    val label: String,
    val deadlineMillis: Long,
    val isOverdue: Boolean
)
