package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.data.model.BannerItem
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

    // Selected colour theme key (see LauncherTheme): default / pink / green / saffron / seattle
    private val _themeKey = MutableStateFlow(prefs.getString(KEY_THEME, "default") ?: "default")
    val themeKey: StateFlow<String> = _themeKey.asStateFlow()

    private val _favorites = MutableStateFlow(loadFavorites())
    val favorites: StateFlow<List<String>> = _favorites.asStateFlow()

    private val _showTime = MutableStateFlow(prefs.getBoolean(KEY_SHOW_TIME, true))
    val showTime: StateFlow<Boolean> = _showTime.asStateFlow()

    private val _showDate = MutableStateFlow(prefs.getBoolean(KEY_SHOW_DATE, true))
    val showDate: StateFlow<Boolean> = _showDate.asStateFlow()

    private val _showNotes = MutableStateFlow(prefs.getBoolean(KEY_SHOW_NOTES, true))
    val showNotes: StateFlow<Boolean> = _showNotes.asStateFlow()

    private val _showTasks = MutableStateFlow(prefs.getBoolean(KEY_SHOW_TASKS, true))
    val showTasks: StateFlow<Boolean> = _showTasks.asStateFlow()

    private val _showScreenTime = MutableStateFlow(prefs.getBoolean(KEY_SHOW_SCREEN_TIME, true))
    val showScreenTime: StateFlow<Boolean> = _showScreenTime.asStateFlow()

    private val _showStudyDue = MutableStateFlow(prefs.getBoolean(KEY_SHOW_STUDY_DUE, true))
    val showStudyDue: StateFlow<Boolean> = _showStudyDue.asStateFlow()

    private val _banners = MutableStateFlow(loadBanners())
    val banners: StateFlow<List<BannerItem>> = _banners.asStateFlow()

    private val _tasks = MutableStateFlow(loadTasks())
    val tasks: StateFlow<List<TaskItem>> = _tasks.asStateFlow()

    // App Usage Limits (packageName -> daily limit in minutes)
    private val _appLimits = MutableStateFlow(loadAppLimits())
    val appLimits: StateFlow<Map<String, Int>> = _appLimits.asStateFlow()

    // Study Manager tree (Course > Subject > Topic > Class)
    private val _studyCourses = MutableStateFlow(loadStudyCourses())
    val studyCourses: StateFlow<List<StudyCourse>> = _studyCourses.asStateFlow()

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

    fun setShowNotes(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SHOW_NOTES, enabled).apply()
        _showNotes.value = enabled
    }

    fun setShowTasks(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SHOW_TASKS, enabled).apply()
        _showTasks.value = enabled
    }

    fun setShowScreenTime(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SHOW_SCREEN_TIME, enabled).apply()
        _showScreenTime.value = enabled
    }

    fun setShowStudyDue(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SHOW_STUDY_DUE, enabled).apply()
        _showStudyDue.value = enabled
    }

    fun setTheme(key: String) {
        prefs.edit().putString(KEY_THEME, key).apply()
        _themeKey.value = key
    }

    fun setAppLimit(packageName: String, limitMinutes: Int) {
        val current = _appLimits.value.toMutableMap()
        if (limitMinutes <= 0) {
            current.remove(packageName)
        } else {
            current[packageName] = limitMinutes
        }
        val obj = JSONObject()
        current.forEach { (pkg, limit) -> obj.put(pkg, limit) }
        prefs.edit().putString(KEY_APP_LIMITS, obj.toString()).apply()
        _appLimits.value = current
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

    fun saveBanners(list: List<BannerItem>) {
        val array = JSONArray()
        for (item in list) {
            val obj = JSONObject().apply {
                put("id", item.id)
                put("title", item.title)
                put("content", item.content)
                put("category", item.category)
            }
            array.put(obj)
        }
        prefs.edit().putString(KEY_BANNERS, array.toString()).apply()
        _banners.value = list
    }

    fun addBanner(banner: BannerItem) {
        val current = _banners.value.toMutableList()
        current.add(banner)
        saveBanners(current)
    }

    fun removeBanner(id: String) {
        val current = _banners.value.filter { it.id != id }
        saveBanners(current)
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

    private fun loadAppLimits(): Map<String, Int> {
        val raw = prefs.getString(KEY_APP_LIMITS, null) ?: return emptyMap()
        return try {
            val obj = JSONObject(raw)
            val map = mutableMapOf<String, Int>()
            val keys = obj.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                map[key] = obj.getInt(key)
            }
            map
        } catch (_: Exception) {
            emptyMap()
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

    private fun loadBanners(): List<BannerItem> {
        val raw = prefs.getString(KEY_BANNERS, null)
        if (raw == null) {
            return emptyList()
        }
        return try {
            val array = JSONArray(raw)
            val list = mutableListOf<BannerItem>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    BannerItem(
                        id = obj.getString("id"),
                        title = obj.getString("title"),
                        content = obj.getString("content"),
                        category = obj.optString("category", "Note")
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
    // Backup & Restore (whole-app export / import)
    // ---------------------------------------------------------------------

    /** Serialises every stored preference (settings + todos + banners + study data ...). */
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
        root.put("formatVersion", 1)
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
        _showNotes.value = prefs.getBoolean(KEY_SHOW_NOTES, true)
        _showTasks.value = prefs.getBoolean(KEY_SHOW_TASKS, true)
        _showScreenTime.value = prefs.getBoolean(KEY_SHOW_SCREEN_TIME, true)
        _showStudyDue.value = prefs.getBoolean(KEY_SHOW_STUDY_DUE, true)
        _themeKey.value = prefs.getString(KEY_THEME, "default") ?: "default"
        _banners.value = loadBanners()
        _tasks.value = loadTasks()
        _appLimits.value = loadAppLimits()
        _studyCourses.value = loadStudyCourses()
    }

    companion object {
        private const val KEY_NIGHT_MODE = "is_night_mode"
        private const val KEY_FAVORITES = "favorite_packages"
        private const val KEY_SHOW_TIME = "show_time"
        private const val KEY_SHOW_DATE = "show_date"
        private const val KEY_SHOW_NOTES = "show_notes"
        private const val KEY_SHOW_TASKS = "show_tasks"
        private const val KEY_BANNERS = "banners_data"
        private const val KEY_TASKS = "tasks_data"
        private const val KEY_STUDY_DATA = "study_manager_data"
        private const val KEY_THEME = "theme_key"
        private const val KEY_SHOW_SCREEN_TIME = "show_screen_time"
        private const val KEY_SHOW_STUDY_DUE = "show_study_due"

        // App Limits
        private const val KEY_APP_LIMITS = "app_limits_data"
    }
}
