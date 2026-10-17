package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.data.model.AlertItem
import com.example.data.model.FlashcardItem
import com.example.data.model.StudyPopupSettings
import com.example.data.model.SubjectPlanSettings
import com.example.data.model.TimeTableEntry
import com.example.data.model.TimeTableSettings
import com.example.data.model.StudyClass
import com.example.data.model.StudyCourse
import com.example.data.model.StudySubject
import com.example.data.model.StudyTopic
import com.example.data.model.TaskItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

class LauncherPreferences(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("focus_launcher_prefs", Context.MODE_PRIVATE)

    private val _isNightMode = MutableStateFlow(prefs.getBoolean(KEY_NIGHT_MODE, false))
    val isNightMode: StateFlow<Boolean> = _isNightMode.asStateFlow()

    // Selected colour theme key (see LauncherTheme): default / pink / green / saffron / seattle / ocean / lavender
    private val _themeKey = MutableStateFlow(prefs.getString(KEY_THEME, "default") ?: "default")
    val themeKey: StateFlow<String> = _themeKey.asStateFlow()

    private val _favorites = MutableStateFlow(loadFavorites())
    val favorites: StateFlow<List<String>> = _favorites.asStateFlow()

    private val _showTime = MutableStateFlow(prefs.getBoolean(KEY_SHOW_TIME, true))
    val showTime: StateFlow<Boolean> = _showTime.asStateFlow()

    private val _showDate = MutableStateFlow(prefs.getBoolean(KEY_SHOW_DATE, true))
    val showDate: StateFlow<Boolean> = _showDate.asStateFlow()

    private val _showAlerts = MutableStateFlow(prefs.getBoolean(KEY_SHOW_ALERTS, true))
    val showAlerts: StateFlow<Boolean> = _showAlerts.asStateFlow()

    private val _showTasks = MutableStateFlow(prefs.getBoolean(KEY_SHOW_TASKS, true))
    val showTasks: StateFlow<Boolean> = _showTasks.asStateFlow()

    private val _showStudyDue = MutableStateFlow(prefs.getBoolean(KEY_SHOW_STUDY_DUE, true))
    val showStudyDue: StateFlow<Boolean> = _showStudyDue.asStateFlow()

    private val _alerts = MutableStateFlow(loadAlerts())
    val alerts: StateFlow<List<AlertItem>> = _alerts.asStateFlow()

    private val _tasks = MutableStateFlow(loadTasks())
    val tasks: StateFlow<List<TaskItem>> = _tasks.asStateFlow()

    // Study Manager tree (Course > Subject > Topic > Class)
    private val _studyCourses = MutableStateFlow(loadStudyCourses())
    val studyCourses: StateFlow<List<StudyCourse>> = _studyCourses.asStateFlow()

    // Quick Time Table (settings + last generated plan)
    private val _timeTable = MutableStateFlow(loadTimeTable())
    val timeTable: StateFlow<TimeTableSettings> = _timeTable.asStateFlow()

    // Study Popups (cards + their schedule)
    private val _flashcardBank = MutableStateFlow(loadFlashcardBank())
    val flashcardBank: StateFlow<List<FlashcardItem>> = _flashcardBank.asStateFlow()

    private val _popupSettings = MutableStateFlow(loadPopupSettings())
    val popupSettings: StateFlow<StudyPopupSettings> = _popupSettings.asStateFlow()

    fun setNightMode(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_NIGHT_MODE, enabled).apply()
        _isNightMode.value = enabled
    }

    fun setShowTime(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SHOW_TIME, enabled).apply()
        _showTime.value = enabled
    }

    fun setShowDate(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SHOW_DATE, enabled).apply()
        _showDate.value = enabled
    }

    fun setShowAlerts(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SHOW_ALERTS, enabled).apply()
        _showAlerts.value = enabled
    }

    fun setShowTasks(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SHOW_TASKS, enabled).apply()
        _showTasks.value = enabled
    }

    fun setShowStudyDue(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SHOW_STUDY_DUE, enabled).apply()
        _showStudyDue.value = enabled
    }

    fun setTheme(key: String) {
        prefs.edit().putString(KEY_THEME, key).apply()
        _themeKey.value = key
    }

    fun saveFavorites(list: List<String>) {
        val array = JSONArray()
        list.forEach { array.put(it) }
        prefs.edit().putString(KEY_FAVORITES, array.toString()).apply()
        _favorites.value = list
    }

    fun addFavorite(packageName: String) {
        val current = _favorites.value.toMutableList()
        if (!current.contains(packageName)) {
            current.add(packageName)
            saveFavorites(current)
        }
    }

    fun removeFavorite(packageName: String) {
        val current = _favorites.value.toMutableList()
        if (current.remove(packageName)) {
            saveFavorites(current)
        }
    }

    fun toggleFavorite(packageName: String) {
        val current = _favorites.value.toMutableList()
        if (current.contains(packageName)) {
            current.remove(packageName)
        } else {
            current.add(packageName)
        }
        saveFavorites(current)
    }

    fun saveTasks(list: List<TaskItem>) {
        val array = JSONArray()
        for (item in list) {
            val obj = JSONObject().apply {
                put("id", item.id)
                put("text", item.text)
                put("isCompleted", item.isCompleted)
            }
            array.put(obj)
        }
        prefs.edit().putString(KEY_TASKS, array.toString()).apply()
        _tasks.value = list
    }

    fun addTask(text: String) {
        if (text.isBlank()) return
        val current = _tasks.value.toMutableList()
        current.add(TaskItem(text = text.trim()))
        saveTasks(current)
    }

    fun toggleTask(id: String) {
        val current = _tasks.value.map {
            if (it.id == id) it.copy(isCompleted = !it.isCompleted) else it
        }
        saveTasks(current)
    }

    fun removeTask(id: String) {
        val current = _tasks.value.filter { it.id != id }
        saveTasks(current)
    }

    fun updateTask(id: String, text: String) {
        if (text.isBlank()) return
        saveTasks(_tasks.value.map { if (it.id == id) it.copy(text = text.trim()) else it })
    }

    fun moveTask(id: String, direction: Int) {
        val list = _tasks.value.toMutableList()
        val index = list.indexOfFirst { it.id == id }
        val target = index + direction
        if (index < 0 || target !in list.indices) return
        list.add(target, list.removeAt(index))
        saveTasks(list)
    }

    fun saveAlerts(list: List<AlertItem>) {
        val array = JSONArray()
        for (item in list) {
            val obj = JSONObject().apply {
                put("id", item.id)
                put("title", item.title)
                put("content", item.content)
                put("category", item.category)
                item.reminderMillis?.let { put("reminderMillis", it) }
            }
            array.put(obj)
        }
        prefs.edit().putString(KEY_ALERTS, array.toString()).apply()
        _alerts.value = list
    }

    fun addAlert(alert: AlertItem) {
        val current = _alerts.value.toMutableList()
        current.add(alert)
        saveAlerts(current)
    }

    fun updateAlert(alert: AlertItem) {
        val current = _alerts.value.map { if (it.id == alert.id) alert else it }
        saveAlerts(current)
    }

    fun removeAlert(id: String) {
        val current = _alerts.value.filter { it.id != id }
        saveAlerts(current)
    }

    fun moveAlert(id: String, direction: Int) {
        val list = _alerts.value.toMutableList()
        val index = list.indexOfFirst { it.id == id }
        val target = index + direction
        if (index < 0 || target !in list.indices) return
        list.add(target, list.removeAt(index))
        saveAlerts(list)
    }

    private fun loadFavorites(): List<String> {
        val raw = prefs.getString(KEY_FAVORITES, null) ?: return emptyList()
        return try {
            val array = JSONArray(raw)
            val list = mutableListOf<String>()
            for (i in 0 until array.length()) {
                list.add(array.getString(i))
            }
            list
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun loadTasks(): List<TaskItem> {
        val raw = prefs.getString(KEY_TASKS, null)
        if (raw == null) {
            return emptyList()
        }
        return try {
            val array = JSONArray(raw)
            val list = mutableListOf<TaskItem>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    TaskItem(
                        id = obj.getString("id"),
                        text = obj.getString("text"),
                        isCompleted = obj.getBoolean("isCompleted")
                    )
                )
            }
            list
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun loadAlerts(): List<AlertItem> {
        val raw = prefs.getString(KEY_ALERTS, null)
        if (raw == null) {
            return emptyList()
        }
        return try {
            val array = JSONArray(raw)
            val list = mutableListOf<AlertItem>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    AlertItem(
                        id = obj.getString("id"),
                        title = obj.getString("title"),
                        content = obj.getString("content"),
                        category = obj.optString("category", ""),
                        reminderMillis = if (obj.has("reminderMillis")) obj.optLong("reminderMillis") else null
                    )
                )
            }
            list
        } catch (_: Exception) {
            emptyList()
        }
    }

    // ---------------------------------------------------------------------
    // Study Manager storage
    // ---------------------------------------------------------------------

    fun saveStudyCourses(list: List<StudyCourse>) {
        val array = JSONArray()
        for (course in list) array.put(studyCourseToJson(course))
        prefs.edit().putString(KEY_STUDY_DATA, array.toString()).apply()
        _studyCourses.value = list
    }

    fun addStudyCourse(name: String) {
        if (name.isBlank()) return
        saveStudyCourses(_studyCourses.value + StudyCourse(name = name.trim()))
    }

    fun addStudySubject(courseId: String, name: String) {
        if (name.isBlank()) return
        saveStudyCourses(_studyCourses.value.map { course ->
            if (course.id == courseId) course.copy(subjects = course.subjects + StudySubject(name = name.trim()))
            else course
        })
    }

    fun addStudyTopic(courseId: String, subjectId: String, name: String) {
        if (name.isBlank()) return
        saveStudyCourses(_studyCourses.value.map { course ->
            if (course.id != courseId) course else course.copy(subjects = course.subjects.map { subject ->
                if (subject.id == subjectId) subject.copy(topics = subject.topics + StudyTopic(name = name.trim()))
                else subject
            })
        })
    }

    fun addStudyClass(courseId: String, subjectId: String, topicId: String, name: String) {
        if (name.isBlank()) return
        saveStudyCourses(_studyCourses.value.map { course ->
            if (course.id != courseId) course else course.copy(subjects = course.subjects.map { subject ->
                if (subject.id != subjectId) subject else subject.copy(topics = subject.topics.map { topic ->
                    if (topic.id == topicId) topic.copy(classes = topic.classes + StudyClass(name = name.trim()))
                    else topic
                })
            })
        })
    }

    fun toggleStudyClass(courseId: String, subjectId: String, topicId: String, classId: String) {
        saveStudyCourses(_studyCourses.value.map { course ->
            if (course.id != courseId) course else course.copy(subjects = course.subjects.map { subject ->
                if (subject.id != subjectId) subject else subject.copy(topics = subject.topics.map { topic ->
                    if (topic.id != topicId) topic else topic.copy(classes = topic.classes.map { clazz ->
                        if (clazz.id == classId) {
                            val completed = !clazz.isCompleted
                            clazz.copy(
                                isCompleted = completed,
                                completedAtMillis = if (completed) System.currentTimeMillis() else null
                            )
                        } else clazz
                    })
                })
            })
        })
    }

    fun deleteStudyCourse(courseId: String) {
        saveStudyCourses(_studyCourses.value.filterNot { it.id == courseId })
    }

    fun deleteStudySubject(courseId: String, subjectId: String) {
        saveStudyCourses(_studyCourses.value.map { course ->
            if (course.id == courseId) course.copy(subjects = course.subjects.filterNot { it.id == subjectId })
            else course
        })
    }

    fun deleteStudyTopic(courseId: String, subjectId: String, topicId: String) {
        saveStudyCourses(_studyCourses.value.map { course ->
            if (course.id != courseId) course else course.copy(subjects = course.subjects.map { subject ->
                if (subject.id == subjectId) subject.copy(topics = subject.topics.filterNot { it.id == topicId })
                else subject
            })
        })
    }

    fun deleteStudyClass(courseId: String, subjectId: String, topicId: String, classId: String) {
        saveStudyCourses(_studyCourses.value.map { course ->
            if (course.id != courseId) course else course.copy(subjects = course.subjects.map { subject ->
                if (subject.id != subjectId) subject else subject.copy(topics = subject.topics.map { topic ->
                    if (topic.id == topicId) topic.copy(classes = topic.classes.filterNot { it.id == classId })
                    else topic
                })
            })
        })
    }

    fun setStudyTopicSchedule(
        courseId: String, subjectId: String, topicId: String,
        deadlineMillis: Long?, alertMillis: Long?
    ) {
        saveStudyCourses(_studyCourses.value.map { course ->
            if (course.id != courseId) course else course.copy(subjects = course.subjects.map { subject ->
                if (subject.id != subjectId) subject else subject.copy(topics = subject.topics.map { topic ->
                    if (topic.id == topicId) topic.copy(deadlineMillis = deadlineMillis, alertMillis = alertMillis)
                    else topic
                })
            })
        })
    }

    fun setStudyClassSchedule(
        courseId: String, subjectId: String, topicId: String, classId: String,
        deadlineMillis: Long?, alertMillis: Long?
    ) {
        saveStudyCourses(_studyCourses.value.map { course ->
            if (course.id != courseId) course else course.copy(subjects = course.subjects.map { subject ->
                if (subject.id != subjectId) subject else subject.copy(topics = subject.topics.map { topic ->
                    if (topic.id != topicId) topic else topic.copy(classes = topic.classes.map { clazz ->
                        if (clazz.id == classId) clazz.copy(deadlineMillis = deadlineMillis, alertMillis = alertMillis)
                        else clazz
                    })
                })
            })
        })
    }

    private fun studyCourseToJson(course: StudyCourse): JSONObject = JSONObject().apply {
        put("id", course.id)
        put("name", course.name)
        val subjects = JSONArray()
        for (subject in course.subjects) subjects.put(studySubjectToJson(subject))
        put("subjects", subjects)
    }

    private fun studySubjectToJson(subject: StudySubject): JSONObject = JSONObject().apply {
        put("id", subject.id)
        put("name", subject.name)
        val topics = JSONArray()
        for (topic in subject.topics) topics.put(studyTopicToJson(topic))
        put("topics", topics)
    }

    private fun studyTopicToJson(topic: StudyTopic): JSONObject = JSONObject().apply {
        put("id", topic.id)
        put("name", topic.name)
        topic.deadlineMillis?.let { put("deadlineMillis", it) }
        topic.alertMillis?.let { put("alertMillis", it) }
        val classes = JSONArray()
        for (clazz in topic.classes) classes.put(studyClassToJson(clazz))
        put("classes", classes)
    }

    private fun studyClassToJson(clazz: StudyClass): JSONObject = JSONObject().apply {
        put("id", clazz.id)
        put("name", clazz.name)
        put("isCompleted", clazz.isCompleted)
        clazz.deadlineMillis?.let { put("deadlineMillis", it) }
        clazz.alertMillis?.let { put("alertMillis", it) }
        clazz.completedAtMillis?.let { put("completedAtMillis", it) }
    }

    private fun loadStudyCourses(): List<StudyCourse> {
        val raw = prefs.getString(KEY_STUDY_DATA, null) ?: return emptyList()
        return try {
            val array = JSONArray(raw)
            val list = mutableListOf<StudyCourse>()
            for (i in 0 until array.length()) list.add(studyCourseFromJson(array.getJSONObject(i)))
            list
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun studyCourseFromJson(obj: JSONObject): StudyCourse {
        val subjectsArr = obj.optJSONArray("subjects") ?: JSONArray()
        val subjects = mutableListOf<StudySubject>()
        for (i in 0 until subjectsArr.length()) subjects.add(studySubjectFromJson(subjectsArr.getJSONObject(i)))
        return StudyCourse(
            id = obj.optString("id", java.util.UUID.randomUUID().toString()),
            name = obj.optString("name", "Course"),
            subjects = subjects
        )
    }

    private fun studySubjectFromJson(obj: JSONObject): StudySubject {
        val topicsArr = obj.optJSONArray("topics") ?: JSONArray()
        val topics = mutableListOf<StudyTopic>()
        for (i in 0 until topicsArr.length()) topics.add(studyTopicFromJson(topicsArr.getJSONObject(i)))
        return StudySubject(
            id = obj.optString("id", java.util.UUID.randomUUID().toString()),
            name = obj.optString("name", "Subject"),
            topics = topics
        )
    }

    private fun studyTopicFromJson(obj: JSONObject): StudyTopic {
        val classesArr = obj.optJSONArray("classes") ?: JSONArray()
        val classes = mutableListOf<StudyClass>()
        for (i in 0 until classesArr.length()) classes.add(studyClassFromJson(classesArr.getJSONObject(i)))
        return StudyTopic(
            id = obj.optString("id", java.util.UUID.randomUUID().toString()),
            name = obj.optString("name", "Topic"),
            classes = classes,
            deadlineMillis = if (obj.has("deadlineMillis")) obj.optLong("deadlineMillis") else null,
            alertMillis = if (obj.has("alertMillis")) obj.optLong("alertMillis") else null
        )
    }

    private fun studyClassFromJson(obj: JSONObject): StudyClass = StudyClass(
        id = obj.optString("id", java.util.UUID.randomUUID().toString()),
        name = obj.optString("name", "Class"),
        isCompleted = obj.optBoolean("isCompleted", false),
        deadlineMillis = if (obj.has("deadlineMillis")) obj.optLong("deadlineMillis") else null,
        alertMillis = if (obj.has("alertMillis")) obj.optLong("alertMillis") else null,
        completedAtMillis = if (obj.has("completedAtMillis")) obj.optLong("completedAtMillis") else null
    )

    fun renameStudyCourse(courseId: String, name: String) {
        if (name.isBlank()) return
        saveStudyCourses(_studyCourses.value.map { course ->
            if (course.id == courseId) course.copy(name = name.trim()) else course
        })
    }

    fun renameStudySubject(courseId: String, subjectId: String, name: String) {
        if (name.isBlank()) return
        saveStudyCourses(_studyCourses.value.map { course ->
            if (course.id != courseId) course else course.copy(subjects = course.subjects.map { subject ->
                if (subject.id == subjectId) subject.copy(name = name.trim()) else subject
            })
        })
    }

    fun renameStudyTopic(courseId: String, subjectId: String, topicId: String, name: String) {
        if (name.isBlank()) return
        saveStudyCourses(_studyCourses.value.map { course ->
            if (course.id != courseId) course else course.copy(subjects = course.subjects.map { subject ->
                if (subject.id != subjectId) subject else subject.copy(topics = subject.topics.map { topic ->
                    if (topic.id == topicId) topic.copy(name = name.trim()) else topic
                })
            })
        })
    }

    fun renameStudyClass(courseId: String, subjectId: String, topicId: String, classId: String, name: String) {
        if (name.isBlank()) return
        saveStudyCourses(_studyCourses.value.map { course ->
            if (course.id != courseId) course else course.copy(subjects = course.subjects.map { subject ->
                if (subject.id != subjectId) subject else subject.copy(topics = subject.topics.map { topic ->
                    if (topic.id != topicId) topic else topic.copy(classes = topic.classes.map { clazz ->
                        if (clazz.id == classId) clazz.copy(name = name.trim()) else clazz
                    })
                })
            })
        })
    }

    // ---------------------------------------------------------------------
    // Drag & drop reordering (moves an item among its siblings)
    // ---------------------------------------------------------------------

    fun moveStudyCourse(courseId: String, direction: Int) {
        val list = _studyCourses.value.toMutableList()
        val index = list.indexOfFirst { it.id == courseId }
        val target = index + direction
        if (index < 0 || target !in list.indices) return
        list.add(target, list.removeAt(index))
        saveStudyCourses(list)
    }

    fun moveStudySubject(courseId: String, subjectId: String, direction: Int) {
        saveStudyCourses(_studyCourses.value.map { course ->
            if (course.id != courseId) course else {
                val subjects = course.subjects.toMutableList()
                val index = subjects.indexOfFirst { it.id == subjectId }
                val target = index + direction
                if (index < 0 || target !in subjects.indices) course
                else {
                    subjects.add(target, subjects.removeAt(index))
                    course.copy(subjects = subjects)
                }
            }
        })
    }

    fun moveStudyTopic(courseId: String, subjectId: String, topicId: String, direction: Int) {
        saveStudyCourses(_studyCourses.value.map { course ->
            if (course.id != courseId) course else course.copy(subjects = course.subjects.map { subject ->
                if (subject.id != subjectId) subject else {
                    val topics = subject.topics.toMutableList()
                    val index = topics.indexOfFirst { it.id == topicId }
                    val target = index + direction
                    if (index < 0 || target !in topics.indices) subject
                    else {
                        topics.add(target, topics.removeAt(index))
                        subject.copy(topics = topics)
                    }
                }
            })
        })
    }

    fun moveStudyClass(courseId: String, subjectId: String, topicId: String, classId: String, direction: Int) {
        saveStudyCourses(_studyCourses.value.map { course ->
            if (course.id != courseId) course else course.copy(subjects = course.subjects.map { subject ->
                if (subject.id != subjectId) subject else subject.copy(topics = subject.topics.map { topic ->
                    if (topic.id != topicId) topic else {
                        val classes = topic.classes.toMutableList()
                        val index = classes.indexOfFirst { it.id == classId }
                        val target = index + direction
                        if (index < 0 || target !in classes.indices) topic
                        else {
                            classes.add(target, classes.removeAt(index))
                            topic.copy(classes = classes)
                        }
                    }
                })
            })
        })
    }

    // ---------------------------------------------------------------------
    // Quick Time Table
    // ---------------------------------------------------------------------

    fun saveTimeTable(settings: TimeTableSettings) {
        prefs.edit().putString(KEY_TIMETABLE, timeTableToJson(settings).toString()).apply()
        _timeTable.value = settings
    }

    private fun timeTableToJson(s: TimeTableSettings): JSONObject = JSONObject().apply {
        put("windowStart", s.windowStartMinutes)
        put("windowEnd", s.windowEndMinutes)
        val ss = JSONObject()
        for ((id, v) in s.subjectSettings) {
            ss.put(id, JSONObject().apply {
                put("duration", v.durationMinutes)
                put("rest", v.restMinutes)
            })
        }
        put("subjectSettings", ss)
        val entriesArr = JSONArray()
        for (e in s.entries) {
            entriesArr.put(JSONObject().apply {
                put("day", e.day)
                put("dateMillis", e.dateMillis)
                put("courseId", e.courseId)
                put("subjectId", e.subjectId)
                put("subjectName", e.subjectName)
                put("topicId", e.topicId)
                put("classId", e.classId)
                put("className", e.className)
                put("startMillis", e.startMillis)
                put("endMillis", e.endMillis)
                put("isFree", e.isFree)
            })
        }
        put("entries", entriesArr)
        val idsArr = JSONArray()
        for (id in s.generatedClassIds) idsArr.put(id)
        put("generatedClassIds", idsArr)
    }

    private fun loadTimeTable(): TimeTableSettings {
        val raw = prefs.getString(KEY_TIMETABLE, null) ?: return TimeTableSettings()
        return try {
            val obj = JSONObject(raw)
            val ssObj = obj.optJSONObject("subjectSettings")
            val subjectSettings = mutableMapOf<String, SubjectPlanSettings>()
            if (ssObj != null) {
                val keys = ssObj.keys()
                while (keys.hasNext()) {
                    val k = keys.next()
                    val v = ssObj.optJSONObject(k) ?: continue
                    subjectSettings[k] = SubjectPlanSettings(
                        durationMinutes = v.optInt("duration", 45),
                        restMinutes = v.optInt("rest", 10)
                    )
                }
            }
            val entriesArr = obj.optJSONArray("entries") ?: JSONArray()
            val entries = mutableListOf<TimeTableEntry>()
            for (i in 0 until entriesArr.length()) {
                val e = entriesArr.getJSONObject(i)
                entries.add(
                    TimeTableEntry(
                        day = e.optInt("day", 0),
                        dateMillis = e.optLong("dateMillis", 0L),
                        courseId = e.optString("courseId", ""),
                        subjectId = e.optString("subjectId", ""),
                        subjectName = e.optString("subjectName", ""),
                        topicId = e.optString("topicId", ""),
                        classId = e.optString("classId", ""),
                        className = e.optString("className", ""),
                        startMillis = e.optLong("startMillis", 0L),
                        endMillis = e.optLong("endMillis", 0L),
                        isFree = e.optBoolean("isFree", false)
                    )
                )
            }
            val idsArr = obj.optJSONArray("generatedClassIds") ?: JSONArray()
            val ids = mutableListOf<String>()
            for (i in 0 until idsArr.length()) ids.add(idsArr.getString(i))
            TimeTableSettings(
                windowStartMinutes = obj.optInt("windowStart", 9 * 60),
                windowEndMinutes = obj.optInt("windowEnd", 21 * 60),
                subjectSettings = subjectSettings,
                entries = entries,
                generatedClassIds = ids
            )
        } catch (_: Exception) {
            TimeTableSettings()
        }
    }

    // ---------------------------------------------------------------------
    // Study Popups (MCQ + flashcard banks and their schedule)
    // ---------------------------------------------------------------------

    fun saveFlashcardBank(list: List<FlashcardItem>) {
        val arr = JSONArray()
        for (c in list) {
            arr.put(JSONObject().apply {
                put("id", c.id)
                put("front", c.front)
                put("back", c.back)
                put("locked", c.locked)
                put("repeatStage", c.repeatStage)
                c.nextDueMillis?.let { put("nextDueMillis", it) }
                put("deck", c.deck)
                c.masteredAtMillis?.let { put("masteredAtMillis", it) }
            })
        }
        prefs.edit().putString(KEY_FLASHCARD_BANK, arr.toString()).apply()
        _flashcardBank.value = list
    }

    fun savePopupSettings(s: StudyPopupSettings) {
        prefs.edit().putString(KEY_POPUP_SETTINGS, JSONObject().apply {
            put("enabled", s.enabled)
            put("start", s.startMinutes)
            put("stop", s.stopMinutes)
            put("interval", s.intervalMinutes)
            val decks = JSONArray(); s.disabledDecks.forEach { decks.put(it) }
            put("disabledDecks", decks)
        }.toString()).apply()
        _popupSettings.value = s
    }

    fun updateCard(card: FlashcardItem) {
        saveFlashcardBank(_flashcardBank.value.map { if (it.id == card.id) card else it })
    }

    fun deleteCard(id: String) {
        saveFlashcardBank(_flashcardBank.value.filterNot { it.id == id })
    }

    private fun loadFlashcardBank(): List<FlashcardItem> {
        val raw = prefs.getString(KEY_FLASHCARD_BANK, null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            val list = mutableListOf<FlashcardItem>()
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                list.add(
                    FlashcardItem(
                        id = o.optString("id", java.util.UUID.randomUUID().toString()),
                        front = o.optString("front", ""),
                        back = o.optString("back", ""),
                        locked = o.optBoolean("locked", false),
                        repeatStage = o.optInt("repeatStage", 0),
                        nextDueMillis = if (o.has("nextDueMillis")) o.optLong("nextDueMillis") else null,
                        deck = o.optString("deck", ""),
                        masteredAtMillis = if (o.has("masteredAtMillis")) o.optLong("masteredAtMillis") else null
                    )
                )
            }
            list
        } catch (_: Exception) { emptyList() }
    }

    private fun loadPopupSettings(): StudyPopupSettings {
        val raw = prefs.getString(KEY_POPUP_SETTINGS, null) ?: return StudyPopupSettings()
        return try {
            val o = JSONObject(raw)
            val decksArr = o.optJSONArray("disabledDecks") ?: JSONArray()
            val decks = mutableListOf<String>()
            for (i in 0 until decksArr.length()) decks.add(decksArr.getString(i))
            StudyPopupSettings(
                enabled = o.optBoolean("enabled", false),
                startMinutes = o.optInt("start", 9 * 60),
                stopMinutes = o.optInt("stop", 21 * 60),
                intervalMinutes = o.optInt("interval", 10),
                disabledDecks = decks
            )
        } catch (_: Exception) { StudyPopupSettings() }
    }

    // ---------------------------------------------------------------------
    // Backup & Restore (whole-app export / import)
    // ---------------------------------------------------------------------

    /**
     * Serialises every stored preference (settings + alerts + tasks + study data ...).
     *
     * Format v2: the App-use/limits keys and the old screen-time flag are gone, the
     * notes data now lives under [KEY_ALERTS] and every alert may carry a reminder.
     */
    fun exportAllData(): String {
        val data = JSONObject()
        for ((key, value) in prefs.all) {
            when (value) {
                is Boolean, is Int, is Long, is Float, is String -> data.put(key, value)
                is Set<*> -> {
                    val array = JSONArray()
                    value.forEach { array.put(it) }
                    data.put(key, array)
                }
                else -> Unit
            }
        }
        val root = JSONObject()
        root.put("app", "SP Launcher")
        root.put("formatVersion", 2)
        root.put("exportedAt", System.currentTimeMillis())
        root.put("prefs", data)
        return root.toString(2)
    }

    /** Restores a backup produced by [exportAllData]. Returns true on success. */
    fun importAllData(json: String): Boolean {
        return try {
            val root = JSONObject(json)
            val data = root.optJSONObject("prefs") ?: return false
            val editor = prefs.edit().clear()
            val keys = data.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                when (val value = data.get(key)) {
                    is Boolean -> editor.putBoolean(key, value)
                    is Int -> editor.putInt(key, value)
                    is Long -> editor.putLong(key, value)
                    is Double -> editor.putFloat(key, value.toFloat())
                    is String -> editor.putString(key, value)
                    is JSONArray -> {
                        val set = mutableSetOf<String>()
                        for (i in 0 until value.length()) set.add(value.getString(i))
                        editor.putStringSet(key, set)
                    }
                    else -> Unit
                }
            }
            editor.apply()
            reloadAll()
            true
        } catch (_: Exception) {
            false
        }
    }

    /** Re-reads every preference into its StateFlow after a bulk import. */
    private fun reloadAll() {
        _isNightMode.value = prefs.getBoolean(KEY_NIGHT_MODE, false)
        _favorites.value = loadFavorites()
        _showTime.value = prefs.getBoolean(KEY_SHOW_TIME, true)
        _showDate.value = prefs.getBoolean(KEY_SHOW_DATE, true)
        _showAlerts.value = prefs.getBoolean(KEY_SHOW_ALERTS, true)
        _showTasks.value = prefs.getBoolean(KEY_SHOW_TASKS, true)
        _showStudyDue.value = prefs.getBoolean(KEY_SHOW_STUDY_DUE, true)
        _themeKey.value = prefs.getString(KEY_THEME, "default") ?: "default"
        _alerts.value = loadAlerts()
        _tasks.value = loadTasks()
        _studyCourses.value = loadStudyCourses()
        _timeTable.value = loadTimeTable()
        _flashcardBank.value = loadFlashcardBank()
        _popupSettings.value = loadPopupSettings()
    }

    companion object {
        private const val KEY_NIGHT_MODE = "is_night_mode"
        private const val KEY_FAVORITES = "favorite_packages"
        private const val KEY_SHOW_TIME = "show_time"
        private const val KEY_SHOW_DATE = "show_date"
        private const val KEY_SHOW_ALERTS = "show_alerts"
        private const val KEY_SHOW_TASKS = "show_tasks"
        private const val KEY_ALERTS = "alerts_data"
        private const val KEY_TASKS = "tasks_data"
        private const val KEY_STUDY_DATA = "study_manager_data"
        private const val KEY_THEME = "theme_key"
        private const val KEY_SHOW_STUDY_DUE = "show_study_due"
        private const val KEY_TIMETABLE = "timetable_settings"
        private const val KEY_FLASHCARD_BANK = "flashcard_bank"
        private const val KEY_POPUP_SETTINGS = "study_popup_settings"
    }
}
