package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.data.model.AlertItem
import com.example.data.model.AppGroup
import com.example.data.model.FavoriteSlots
import com.example.data.model.FlashcardItem
import com.example.data.model.StudyPopupSettings
import com.example.data.model.SubjectPlanSettings
import com.example.data.model.TimeTableEntry
import com.example.data.model.TimeTableSettings
import com.example.data.model.StudyClass
import com.example.data.model.StudyCourse
import com.example.data.model.StudySubject
import com.example.data.model.StudyTopic
import com.example.data.model.RepeatRule
import com.example.data.model.TaskItem
import com.example.data.model.GoalItem
import com.example.data.model.GoalType
import com.example.data.model.TaskPriority
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

    // Auto night mode: go dark between the window's start and end times.
    private val _autoNightMode = MutableStateFlow(prefs.getBoolean(KEY_AUTO_NIGHT, false))
    val autoNightMode: StateFlow<Boolean> = _autoNightMode.asStateFlow()

    private val _nightStartMinutes = MutableStateFlow(prefs.getInt(KEY_NIGHT_START, 19 * 60))
    val nightStartMinutes: StateFlow<Int> = _nightStartMinutes.asStateFlow()

    private val _nightEndMinutes = MutableStateFlow(prefs.getInt(KEY_NIGHT_END, 7 * 60))
    val nightEndMinutes: StateFlow<Int> = _nightEndMinutes.asStateFlow()

    // Hidden apps (kept out of the drawer) and the vault PIN (entered as a time).
    private val _hiddenApps = MutableStateFlow(prefs.getStringSet(KEY_HIDDEN_APPS, emptySet()) ?: emptySet())
    val hiddenApps: StateFlow<Set<String>> = _hiddenApps.asStateFlow()

    private val _vaultPin = MutableStateFlow(prefs.getString(KEY_VAULT_PIN, "1234") ?: "1234")
    val vaultPin: StateFlow<String> = _vaultPin.asStateFlow()

    // Auto-backup (a daily JSON written into a folder the user picks, via SAF).
    private val _autoBackupEnabled = MutableStateFlow(prefs.getBoolean(KEY_AUTO_BACKUP, false))
    val autoBackupEnabled: StateFlow<Boolean> = _autoBackupEnabled.asStateFlow()

    private val _backupFolderUri = MutableStateFlow(prefs.getString(KEY_BACKUP_URI, null))
    val backupFolderUri: StateFlow<String?> = _backupFolderUri.asStateFlow()

    private val _lastBackupMillis = MutableStateFlow(prefs.getLong(KEY_LAST_BACKUP, 0L))
    val lastBackupMillis: StateFlow<Long> = _lastBackupMillis.asStateFlow()

    // Selected colour theme key (see LauncherTheme): default / pink / green / saffron / seattle / ocean / lavender
    private val _themeKey = MutableStateFlow(prefs.getString(KEY_THEME, "default") ?: "default")
    val themeKey: StateFlow<String> = _themeKey.asStateFlow()

    // Favourites bar slots: a package name for an app, or "group:<id>" for an app group.
    // Five slots in total, apps and groups together.
    private val _favorites = MutableStateFlow(loadFavorites())
    val favorites: StateFlow<List<String>> = _favorites.asStateFlow()

    // App groups (only ever shown in the favourites bar)
    private val _groups = MutableStateFlow(loadGroups())
    val groups: StateFlow<List<AppGroup>> = _groups.asStateFlow()

    // Theme rotation: off / hour / day
    private val _autoThemeMode = MutableStateFlow(prefs.getString(KEY_AUTO_THEME, "off") ?: "off")
    val autoThemeMode: StateFlow<String> = _autoThemeMode.asStateFlow()

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

    // Goals (regular activity tracker) and their Home widget toggle
    private val _showGoalsWidget = MutableStateFlow(prefs.getBoolean(KEY_SHOW_GOALS, true))
    val showGoalsWidget: StateFlow<Boolean> = _showGoalsWidget.asStateFlow()

    private val _goals = MutableStateFlow(loadGoals())
    val goals: StateFlow<List<GoalItem>> = _goals.asStateFlow()

    // Calendar events the user cleared. Keyed by "<kind>:<id>:<dayStartMillis>" so a cleared
    // item stays hidden on that day only, and reappears if its date is moved.
    private val _clearedCalendarEvents =
        MutableStateFlow(prefs.getStringSet(KEY_CLEARED_CALENDAR, emptySet()) ?: emptySet())
    val clearedCalendarEvents: StateFlow<Set<String>> = _clearedCalendarEvents.asStateFlow()

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

    fun setAutoNightMode(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_NIGHT, enabled).apply()
        _autoNightMode.value = enabled
    }

    fun setNightWindow(startMinutes: Int, endMinutes: Int) {
        prefs.edit().putInt(KEY_NIGHT_START, startMinutes).putInt(KEY_NIGHT_END, endMinutes).apply()
        _nightStartMinutes.value = startMinutes
        _nightEndMinutes.value = endMinutes
    }

    fun setAppHidden(packageName: String, hidden: Boolean) {
        val current = _hiddenApps.value.toMutableSet()
        if (hidden) current.add(packageName) else current.remove(packageName)
        prefs.edit().putStringSet(KEY_HIDDEN_APPS, current).apply()
        _hiddenApps.value = current
    }

    fun setVaultPin(pin: String) {
        prefs.edit().putString(KEY_VAULT_PIN, pin).apply()
        _vaultPin.value = pin
    }

    fun setAutoBackupEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_BACKUP, enabled).apply()
        _autoBackupEnabled.value = enabled
    }

    fun setBackupFolderUri(uri: String?) {
        prefs.edit().putString(KEY_BACKUP_URI, uri).apply()
        _backupFolderUri.value = uri
    }

    fun setLastBackupMillis(millis: Long) {
        prefs.edit().putLong(KEY_LAST_BACKUP, millis).apply()
        _lastBackupMillis.value = millis
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

    fun setShowGoalsWidget(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SHOW_GOALS, enabled).apply()
        _showGoalsWidget.value = enabled
    }

    // ---- Goals ----

    fun saveGoals(list: List<GoalItem>) {
        val array = JSONArray()
        for (goal in list) {
            array.put(
                JSONObject().apply {
                    put("id", goal.id)
                    put("title", goal.title)
                    put("type", goal.type.name)
                    put("startText", goal.startText)
                    put("endText", goal.endText)
                    put("stepValue", goal.stepValue)
                    put("stepLabel", goal.stepLabel)
                    put("targetValue", goal.targetValue)
                    put("currentValue", goal.currentValue)
                    put("totalValue", goal.totalValue)
                    put("resetDaily", goal.resetDaily)
                    put("lastResetEpochDay", goal.lastResetEpochDay)
                    put("colorIndex", goal.colorIndex)
                    put("createdAtMillis", goal.createdAtMillis)
                }
            )
        }
        prefs.edit().putString(KEY_GOALS, array.toString()).apply()
        _goals.value = list
    }

    fun addGoal(goal: GoalItem) {
        val current = _goals.value.toMutableList()
        current.add(goal)
        saveGoals(current)
    }

    fun updateGoal(goal: GoalItem) {
        saveGoals(_goals.value.map { if (it.id == goal.id) goal else it })
    }

    fun deleteGoal(id: String) {
        saveGoals(_goals.value.filterNot { it.id == id })
    }

    fun restoreGoal(goal: GoalItem, index: Int) {
        val current = _goals.value.toMutableList()
        val at = index.coerceIn(0, current.size)
        current.add(at, goal)
        saveGoals(current)
    }

    // ---- Calendar: cleared events ----

    fun clearCalendarEvent(key: String) {
        val current = _clearedCalendarEvents.value.toMutableSet()
        current.add(key)
        prefs.edit().putStringSet(KEY_CLEARED_CALENDAR, current).apply()
        _clearedCalendarEvents.value = current
    }

    fun unclearCalendarEvent(key: String) {
        val current = _clearedCalendarEvents.value.toMutableSet()
        if (current.remove(key)) {
            prefs.edit().putStringSet(KEY_CLEARED_CALENDAR, current).apply()
            _clearedCalendarEvents.value = current
        }
    }

    fun setTheme(key: String) {
        prefs.edit().putString(KEY_THEME, key).apply()
        _themeKey.value = key
    }

    fun saveFavorites(list: List<String>) {
        // Never let more than MAX_SLOTS favourites through, whatever the caller sends.
        val capped = list.distinct().take(FavoriteSlots.MAX_SLOTS)
        val array = JSONArray()
        capped.forEach { array.put(it) }
        prefs.edit().putString(KEY_FAVORITES, array.toString()).apply()
        _favorites.value = capped
    }

    /** True when another favourites-bar slot can still be used. */
    fun canAddFavorite(): Boolean = _favorites.value.size < FavoriteSlots.MAX_SLOTS

    /** Returns false when the bar is already full. */
    fun addFavorite(packageName: String): Boolean {
        val current = _favorites.value.toMutableList()
        if (current.contains(packageName)) return true
        if (current.size >= FavoriteSlots.MAX_SLOTS) return false
        current.add(packageName)
        saveFavorites(current)
        return true
    }

    fun removeFavorite(packageName: String) {
        val current = _favorites.value.toMutableList()
        if (current.remove(packageName)) {
            saveFavorites(current)
        }
    }

    /** Returns false when the bar is full and the app could not be added. */
    fun toggleFavorite(packageName: String): Boolean {
        val current = _favorites.value.toMutableList()
        if (current.contains(packageName)) {
            current.remove(packageName)
            saveFavorites(current)
            return true
        }
        if (current.size >= FavoriteSlots.MAX_SLOTS) return false
        current.add(packageName)
        saveFavorites(current)
        return true
    }

    // ---- App groups ----

    fun saveGroups(list: List<AppGroup>) {
        val array = JSONArray()
        for (group in list) {
            val packages = JSONArray()
            group.packages.take(FavoriteSlots.MAX_APPS_PER_GROUP).forEach { packages.put(it) }
            array.put(
                JSONObject().apply {
                    put("id", group.id)
                    put("packages", packages)
                }
            )
        }
        prefs.edit().putString(KEY_GROUPS, array.toString()).apply()
        _groups.value = list
    }

    /**
     * Creates a group of up to four apps.
     *
     * The group is NOT put in the bar here: the Favourites screen holds the bar's working
     * copy and places the new group through its own slot path, so the 5-slot limit is
     * judged by the bar on screen, not by a saved list the user cannot see. (Before
     * v1.11.5 this checked and filled the saved bar directly, which is why a second
     * group so often refused to be made.)
     */
    fun addGroup(packages: List<String>): AppGroup? {
        val apps = packages.distinct().take(FavoriteSlots.MAX_APPS_PER_GROUP)
        if (apps.isEmpty()) return null
        val group = AppGroup(packages = apps)
        saveGroups(_groups.value + group)
        return group
    }

    fun updateGroup(group: AppGroup) {
        saveGroups(_groups.value.map { if (it.id == group.id) group else it })
    }

    fun deleteGroup(groupId: String) {
        saveGroups(_groups.value.filterNot { it.id == groupId })
        removeFavorite(FavoriteSlots.forGroup(groupId))
    }

    fun setGroupPackages(groupId: String, packages: List<String>) {
        saveGroups(
            _groups.value.map {
                if (it.id == groupId) {
                    it.copy(packages = packages.distinct().take(FavoriteSlots.MAX_APPS_PER_GROUP))
                } else {
                    it
                }
            }
        )
    }

    // ---- Theme rotation ----

    fun setAutoThemeMode(mode: String) {
        prefs.edit().putString(KEY_AUTO_THEME, mode).apply()
        _autoThemeMode.value = mode
    }

    /** When the auto rotation last stepped to a new theme (epoch millis); 0 means never. */
    fun lastAutoThemeMillis(): Long = prefs.getLong(KEY_AUTO_THEME_LAST, 0L)

    fun setLastAutoThemeMillis(value: Long) {
        prefs.edit().putLong(KEY_AUTO_THEME_LAST, value).apply()
    }

    fun saveTasks(list: List<TaskItem>) {
        val array = JSONArray()
        for (item in list) {
            val obj = JSONObject().apply {
                put("id", item.id)
                put("text", item.text)
                put("isCompleted", item.isCompleted)
                item.dueMillis?.let { put("dueMillis", it) }
                item.reminderMillis?.let { put("reminderMillis", it) }
                put("repeat", item.repeat.name)
                put("priority", item.priority.name)
                item.completedAtMillis?.let { put("completedAtMillis", it) }
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
        val now = System.currentTimeMillis()
        val current = _tasks.value.map {
            if (it.id == id) {
                val completing = !it.isCompleted
                it.copy(
                    isCompleted = completing,
                    completedAtMillis = if (completing) now else null
                )
            } else it
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

    /** Replaces a task wholesale (task editor: text + due + reminder + repeat + priority). */
    fun updateTaskItem(task: TaskItem) {
        saveTasks(_tasks.value.map { if (it.id == task.id) task else it })
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
                put("repeat", item.repeat.name)
                put("pinned", item.pinned)
                put("archived", item.archived)
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

    fun setAlertPinned(id: String, pinned: Boolean) {
        saveAlerts(_alerts.value.map { if (it.id == id) it.copy(pinned = pinned) else it })
    }

    fun setAlertArchived(id: String, archived: Boolean) {
        saveAlerts(_alerts.value.map { if (it.id == id) it.copy(archived = archived) else it })
    }

    private fun loadGroups(): List<AppGroup> {
        val raw = prefs.getString(KEY_GROUPS, null) ?: return emptyList()
        return try {
            val array = JSONArray(raw)
            val list = mutableListOf<AppGroup>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val packagesArr = obj.optJSONArray("packages") ?: JSONArray()
                val packages = mutableListOf<String>()
                for (j in 0 until packagesArr.length()) packages.add(packagesArr.getString(j))
                list.add(
                    AppGroup(
                        id = obj.optString("id", java.util.UUID.randomUUID().toString()),
                        packages = packages.take(FavoriteSlots.MAX_APPS_PER_GROUP)
                    )
                )
            }
            list
        } catch (_: Exception) {
            emptyList()
        }
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
                        isCompleted = obj.getBoolean("isCompleted"),
                        dueMillis = if (obj.has("dueMillis")) obj.optLong("dueMillis") else null,
                        reminderMillis = if (obj.has("reminderMillis")) obj.optLong("reminderMillis") else null,
                        repeat = runCatching { RepeatRule.valueOf(obj.optString("repeat", "NONE")) }
                            .getOrDefault(RepeatRule.NONE),
                        priority = runCatching { TaskPriority.valueOf(obj.optString("priority", "NONE")) }
                            .getOrDefault(TaskPriority.NONE),
                        completedAtMillis = if (obj.has("completedAtMillis")) obj.optLong("completedAtMillis") else null
                    )
                )
            }
            list
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun loadGoals(): List<GoalItem> {
        val raw = prefs.getString(KEY_GOALS, null) ?: return emptyList()
        return try {
            val array = JSONArray(raw)
            val list = mutableListOf<GoalItem>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    GoalItem(
                        id = obj.optString("id").ifBlank { java.util.UUID.randomUUID().toString() },
                        title = obj.optString("title"),
                        type = runCatching { GoalType.valueOf(obj.optString("type", "LIMITED")) }
                            .getOrDefault(GoalType.LIMITED),
                        startText = obj.optString("startText", "Beginner"),
                        endText = obj.optString("endText", "Pro"),
                        stepValue = obj.optDouble("stepValue", 1.0),
                        stepLabel = obj.optString("stepLabel"),
                        targetValue = obj.optDouble("targetValue", 100.0),
                        currentValue = obj.optDouble("currentValue", 0.0),
                        totalValue = obj.optDouble("totalValue", 0.0),
                        resetDaily = obj.optBoolean("resetDaily", false),
                        lastResetEpochDay = obj.optLong("lastResetEpochDay", 0L),
                        colorIndex = obj.optInt("colorIndex", 0),
                        createdAtMillis = obj.optLong("createdAtMillis", 0L)
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
                        reminderMillis = if (obj.has("reminderMillis")) obj.optLong("reminderMillis") else null,
                        repeat = runCatching { RepeatRule.valueOf(obj.optString("repeat", "NONE")) }
                            .getOrDefault(RepeatRule.NONE),
                        pinned = obj.optBoolean("pinned", false),
                        archived = obj.optBoolean("archived", false)
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
                                completedAtMillis = if (completed) System.currentTimeMillis() else null,
                                // Completing a task auto-clears its Reminder and Deadline so the
                                // notification/timer does not linger for something already done.
                                deadlineMillis = if (completed) null else clazz.deadlineMillis,
                                alertMillis = if (completed) null else clazz.alertMillis
                            )
                        } else clazz
                    })
                })
            })
        })
    }

    /**
     * Clears the Reminder and Deadline of every study task — both topic-level and class-level —
     * without touching completion marks. Backs the "Clean" button in Study Manager.
     */
    fun clearAllStudySchedules() {
        saveStudyCourses(_studyCourses.value.map { course ->
            course.copy(subjects = course.subjects.map { subject ->
                subject.copy(topics = subject.topics.map { topic ->
                    topic.copy(
                        deadlineMillis = null,
                        alertMillis = null,
                        classes = topic.classes.map { clazz ->
                            clazz.copy(deadlineMillis = null, alertMillis = null)
                        }
                    )
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
        put("days", s.days)
        val ss = JSONObject()
        for ((id, v) in s.subjectSettings) {
            ss.put(id, JSONObject().apply {
                put("duration", v.durationMinutes)
                put("rest", v.restMinutes)
                put("sessions", v.sessionsPerDay)
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
                        restMinutes = v.optInt("rest", 10),
                        sessionsPerDay = v.optInt("sessions", 1)
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
                days = obj.optInt("days", 7),
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
    // Undo / restore (backs the delete-undo snackbar)
    // ---------------------------------------------------------------------

    fun restoreTask(task: TaskItem, index: Int) {
        val list = _tasks.value.toMutableList()
        list.add(index.coerceIn(0, list.size), task)
        saveTasks(list)
    }

    fun restoreAlert(alert: AlertItem, index: Int) {
        val list = _alerts.value.toMutableList()
        list.add(index.coerceIn(0, list.size), alert)
        saveAlerts(list)
    }

    fun restoreCard(card: FlashcardItem, index: Int) {
        val list = _flashcardBank.value.toMutableList()
        list.add(index.coerceIn(0, list.size), card)
        saveFlashcardBank(list)
    }

    fun restoreStudyCourse(course: StudyCourse, index: Int) {
        val list = _studyCourses.value.toMutableList()
        list.add(index.coerceIn(0, list.size), course)
        saveStudyCourses(list)
    }

    fun restoreStudySubject(courseId: String, subject: StudySubject, index: Int) {
        saveStudyCourses(_studyCourses.value.map { course ->
            if (course.id == courseId) {
                val subjects = course.subjects.toMutableList()
                subjects.add(index.coerceIn(0, subjects.size), subject)
                course.copy(subjects = subjects)
            } else course
        })
    }

    fun restoreStudyTopic(courseId: String, subjectId: String, topic: StudyTopic, index: Int) {
        saveStudyCourses(_studyCourses.value.map { course ->
            if (course.id != courseId) course else course.copy(subjects = course.subjects.map { subject ->
                if (subject.id != subjectId) subject else {
                    val topics = subject.topics.toMutableList()
                    topics.add(index.coerceIn(0, topics.size), topic)
                    subject.copy(topics = topics)
                }
            })
        })
    }

    fun restoreStudyClass(courseId: String, subjectId: String, topicId: String, clazz: StudyClass, index: Int) {
        saveStudyCourses(_studyCourses.value.map { course ->
            if (course.id != courseId) course else course.copy(subjects = course.subjects.map { subject ->
                if (subject.id != subjectId) subject else subject.copy(topics = subject.topics.map { topic ->
                    if (topic.id != topicId) topic else {
                        val classes = topic.classes.toMutableList()
                        classes.add(index.coerceIn(0, classes.size), clazz)
                        topic.copy(classes = classes)
                    }
                })
            })
        })
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
        root.put("formatVersion", 3)
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
        _autoNightMode.value = prefs.getBoolean(KEY_AUTO_NIGHT, false)
        _nightStartMinutes.value = prefs.getInt(KEY_NIGHT_START, 19 * 60)
        _nightEndMinutes.value = prefs.getInt(KEY_NIGHT_END, 7 * 60)
        _hiddenApps.value = prefs.getStringSet(KEY_HIDDEN_APPS, emptySet()) ?: emptySet()
        _vaultPin.value = prefs.getString(KEY_VAULT_PIN, "1234") ?: "1234"
        _autoBackupEnabled.value = prefs.getBoolean(KEY_AUTO_BACKUP, false)
        _backupFolderUri.value = prefs.getString(KEY_BACKUP_URI, null)
        _lastBackupMillis.value = prefs.getLong(KEY_LAST_BACKUP, 0L)
        _favorites.value = loadFavorites()
        _groups.value = loadGroups()
        _autoThemeMode.value = prefs.getString(KEY_AUTO_THEME, "off") ?: "off"
        _showTime.value = prefs.getBoolean(KEY_SHOW_TIME, true)
        _showDate.value = prefs.getBoolean(KEY_SHOW_DATE, true)
        _showAlerts.value = prefs.getBoolean(KEY_SHOW_ALERTS, true)
        _showTasks.value = prefs.getBoolean(KEY_SHOW_TASKS, true)
        _showStudyDue.value = prefs.getBoolean(KEY_SHOW_STUDY_DUE, true)
        _showGoalsWidget.value = prefs.getBoolean(KEY_SHOW_GOALS, true)
        _goals.value = loadGoals()
        _clearedCalendarEvents.value =
            prefs.getStringSet(KEY_CLEARED_CALENDAR, emptySet()) ?: emptySet()
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
        private const val KEY_AUTO_NIGHT = "auto_night"
        private const val KEY_NIGHT_START = "night_start_minutes"
        private const val KEY_NIGHT_END = "night_end_minutes"
        private const val KEY_HIDDEN_APPS = "hidden_apps"
        private const val KEY_VAULT_PIN = "vault_pin"
        private const val KEY_AUTO_BACKUP = "auto_backup"
        private const val KEY_BACKUP_URI = "backup_folder_uri"
        private const val KEY_LAST_BACKUP = "last_backup_millis"
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
        private const val KEY_SHOW_GOALS = "show_goals_widget"
        private const val KEY_GOALS = "goals_data"
        private const val KEY_CLEARED_CALENDAR = "cleared_calendar_events"
        private const val KEY_TIMETABLE = "timetable_settings"
        private const val KEY_FLASHCARD_BANK = "flashcard_bank"
        private const val KEY_POPUP_SETTINGS = "study_popup_settings"
        private const val KEY_GROUPS = "app_groups"
        private const val KEY_AUTO_THEME = "auto_theme_mode"
        private const val KEY_AUTO_THEME_LAST = "auto_theme_last_change"
    }
}
