package com.example.util

import com.example.data.model.StudyClass
import com.example.data.model.StudyCourse
import com.example.data.model.StudyTopic
import com.example.data.model.SubjectPlanSettings
import com.example.data.model.TimeTableEntry
import com.example.data.model.TimeTableSettings
import java.util.Calendar
import java.util.Random

/**
 * Builds the Quick Time Table.
 *
 * The plan covers exactly [TimeTableSettings.days] days (the user picks 1, 3 or 7 — a short
 * plan is easier to actually follow). For each day it walks the subjects (in a shuffled order,
 * so the plan feels like a real timetable rather than the same list every day) and gives each
 * subject [SubjectPlanSettings.sessionsPerDay] back-to-back sessions (single / double / triple),
 * each of its configured duration, followed by its configured rest. The class used for a subject
 * is that subject's next un-completed class — its current topic first, then the next topic —
 * advancing one class per session. Once a subject runs out of classes, a "free" class is created
 * and scheduled so a reminder can tell the user they are free.
 *
 * Before laying anything out it checks that the day's window can actually hold every subject's
 * sessions. If it cannot, nothing is generated and [Result.error] carries the exact calculation
 * (available vs. needed minutes) so the UI can tell the user what to change.
 *
 * Reminder = slot start, Deadline = slot end (like a calendar event). Only the classes this
 * timetable scheduled are cleared on a re-generate; deadlines set by hand are left alone.
 */
object TimeTablePlanner {

    data class Result(
        val courses: List<StudyCourse>,
        val entries: List<TimeTableEntry>,
        val generatedClassIds: List<String>,
        /** Non-null when the plan could not be built (e.g. not enough time in the window). */
        val error: String? = null
    )

    private const val MIN_MS = 60_000L

    fun generate(
        courses: List<StudyCourse>,
        previousClassIds: Set<String>,
        settings: TimeTableSettings,
        todayStartMillis: Long,
        random: Random = Random()
    ): Result {
        val days = settings.days.coerceIn(1, 7)

        // ---- 1. subjects and their ordered un-completed classes ----
        val subjects = mutableListOf<SubjectRef>()
        val lastTopic = HashMap<String, String>() // subjectId -> last topic id

        for (course in courses) {
            for (subject in course.subjects) {
                val pending = mutableListOf<Pending>()
                for (topic in subject.topics) {
                    lastTopic[subject.id] = topic.id
                    for (clazz in topic.classes) {
                        if (!clazz.isCompleted) pending.add(Pending(topic.id, clazz.id, clazz.name))
                    }
                }
                subjects.add(SubjectRef(course.id, subject.id, subject.name, pending))
            }
        }

        if (subjects.isEmpty()) {
            return Result(
                courses, emptyList(), emptyList(),
                "Can't build a time table — add a course and a subject in Study Manager first."
            )
        }

        // ---- 2. feasibility check: can the daily window hold every subject's sessions? ----
        val capacity = settings.windowEndMinutes - settings.windowStartMinutes
        if (capacity <= 0) {
            return Result(
                courses, emptyList(), emptyList(),
                "Can't build a time table — the study window is empty.\n" +
                    "Set a valid \"From\" and \"To\" time for each day."
            )
        }

        val lines = mutableListOf<String>()
        var required = 0
        for (subj in subjects) {
            val cfg = settings.subjectSettings[subj.subjectId] ?: SubjectPlanSettings()
            val dur = cfg.durationMinutes.coerceAtLeast(1)
            val rest = cfg.restMinutes.coerceAtLeast(0)
            val sessions = cfg.sessionsPerDay.coerceIn(1, 3)
            val need = sessions * (dur + rest)
            required += need
            lines.add("• ${subj.name}: $sessions × ($dur + $rest) = $need min")
        }
        if (required > capacity) {
            val short = required - capacity
            val message = buildString {
                append("Time table could not be generated — not enough time in a day.\n\n")
                append("Available each day: $capacity min\n")
                append("Needed each day: $required min\n")
                append("Short by: $short min\n\n")
                append("Calculation (sessions × (duration + rest)):\n")
                append(lines.joinToString("\n"))
                append("\n\nReduce a subject's duration or sessions, lower the rest, ")
                append("widen the study window, or plan fewer days.")
            }
            return Result(courses, emptyList(), emptyList(), message)
        }

        // ---- 3. lay out the sessions ----
        val entries = mutableListOf<TimeTableEntry>()
        val pointer = HashMap<String, Int>()               // subjectId -> next class index
        val freeClassId = HashMap<String, String>()        // subjectId -> created "free" class id
        val schedules = HashMap<String, Pair<Long, Long>>()// classId -> (reminder, deadline)
        val generatedIds = mutableListOf<String>()

        for (d in 0 until days) {
            val dayMidnight = addDays(todayStartMillis, d)
            val windowStart = atMinutes(dayMidnight, settings.windowStartMinutes)
            val windowEnd = atMinutes(dayMidnight, settings.windowEndMinutes)
            if (windowEnd <= windowStart) continue

            var cursor = windowStart
            val order = subjects.toMutableList().also { it.shuffle(random) }

            for (subj in order) {
                val cfg = settings.subjectSettings[subj.subjectId] ?: SubjectPlanSettings()
                val durMs = cfg.durationMinutes.coerceAtLeast(1) * MIN_MS
                val restMs = cfg.restMinutes.coerceAtLeast(0) * MIN_MS
                val sessions = cfg.sessionsPerDay.coerceIn(1, 3)

                for (session in 0 until sessions) {
                    // The feasibility check above guarantees this fits; the guard is just safety.
                    if (cursor + durMs > windowEnd) break

                    val idx = pointer.getOrDefault(subj.subjectId, 0)
                    val topicId: String
                    val classId: String
                    val className: String
                    val isFree: Boolean

                    if (idx < subj.pending.size) {
                        val p = subj.pending[idx]
                        pointer[subj.subjectId] = idx + 1
                        topicId = p.topicId; classId = p.classId; className = p.className; isFree = false
                    } else {
                        // No classes left — a "free" placeholder so a reminder can say you're free.
                        val existing = freeClassId[subj.subjectId]
                        if (existing != null) {
                            classId = existing
                        } else {
                            classId = "free-" + subj.subjectId + "-" + random.nextInt(1_000_000)
                            freeClassId[subj.subjectId] = classId
                        }
                        topicId = lastTopic[subj.subjectId] ?: ""
                        className = "free"
                        isFree = true
                    }

                    val start = cursor
                    val end = cursor + durMs
                    schedules[classId] = start to end
                    if (!generatedIds.contains(classId)) generatedIds.add(classId)
                    entries.add(
                        TimeTableEntry(
                            day = d,
                            dateMillis = dayMidnight,
                            courseId = subj.courseId,
                            subjectId = subj.subjectId,
                            subjectName = subj.name,
                            topicId = topicId,
                            classId = classId,
                            className = className,
                            startMillis = start,
                            endMillis = end,
                            isFree = isFree
                        )
                    )
                    cursor = end + restMs
                }
            }
        }

        // ---- 4. rebuild the tree: clear old timetable schedules, add free classes, apply new ----
        val newCourses = courses.map { course ->
            course.copy(subjects = course.subjects.map { subject ->
                var topics: List<StudyTopic> = subject.topics
                val freeId = freeClassId[subject.id]
                if (freeId != null) {
                    topics = if (topics.isEmpty()) {
                        listOf(StudyTopic(name = "Free", classes = listOf(StudyClass(id = freeId, name = "free"))))
                    } else {
                        val last = topics.last()
                        topics.dropLast(1) + last.copy(
                            classes = last.classes + StudyClass(id = freeId, name = "free")
                        )
                    }
                }
                subject.copy(topics = topics.map { topic ->
                    topic.copy(classes = topic.classes.map { clazz ->
                        val sched = schedules[clazz.id]
                        when {
                            sched != null -> clazz.copy(alertMillis = sched.first, deadlineMillis = sched.second)
                            previousClassIds.contains(clazz.id) -> clazz.copy(alertMillis = null, deadlineMillis = null)
                            else -> clazz
                        }
                    })
                })
            })
        }

        return Result(newCourses, entries, generatedIds, null)
    }

    private fun addDays(midnightMillis: Long, days: Int): Long =
        Calendar.getInstance().apply {
            timeInMillis = midnightMillis
            add(Calendar.DAY_OF_YEAR, days)
        }.timeInMillis

    private fun atMinutes(dayMidnightMillis: Long, minutesOfDay: Int): Long =
        Calendar.getInstance().apply {
            timeInMillis = dayMidnightMillis
            set(Calendar.HOUR_OF_DAY, minutesOfDay / 60)
            set(Calendar.MINUTE, minutesOfDay % 60)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
}

private data class Pending(val topicId: String, val classId: String, val className: String)

private data class SubjectRef(
    val courseId: String,
    val subjectId: String,
    val name: String,
    val pending: List<Pending>
)
