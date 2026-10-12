package com.example.data.repository

import android.app.NotificationManager
import android.content.Context
import android.content.SharedPreferences
import com.example.data.model.Badge
import com.example.data.model.BannerItem
import com.example.data.model.FocusSessionRecord
import com.example.data.model.TaskItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class LauncherPreferences(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("focus_launcher_prefs", Context.MODE_PRIVATE)

    private val _isNightMode = MutableStateFlow(prefs.getBoolean(KEY_NIGHT_MODE, false))
    val isNightMode: StateFlow<Boolean> = _isNightMode.asStateFlow()

    private val _favorites = MutableStateFlow(loadFavorites())
    val favorites: StateFlow<List<String>> = _favorites.asStateFlow()

    private val _showTime = MutableStateFlow(prefs.getBoolean(KEY_SHOW_TIME, true))
    val showTime: StateFlow<Boolean> = _showTime.asStateFlow()

    private val _showDate = MutableStateFlow(prefs.getBoolean(KEY_SHOW_DATE, true))
    val showDate: StateFlow<Boolean> = _showDate.asStateFlow()

    // Separate widget toggles for Notes, Reminders, and Goals
    private val _showNotes = MutableStateFlow(prefs.getBoolean(KEY_SHOW_NOTES, true))
    val showNotes: StateFlow<Boolean> = _showNotes.asStateFlow()

    private val _showReminders = MutableStateFlow(prefs.getBoolean(KEY_SHOW_REMINDERS, true))
    val showReminders: StateFlow<Boolean> = _showReminders.asStateFlow()

    private val _showGoals = MutableStateFlow(prefs.getBoolean(KEY_SHOW_GOALS, true))
    val showGoals: StateFlow<Boolean> = _showGoals.asStateFlow()

    private val _showTasks = MutableStateFlow(prefs.getBoolean(KEY_SHOW_TASKS, true))
    val showTasks: StateFlow<Boolean> = _showTasks.asStateFlow()

    private val _banners = MutableStateFlow(loadBanners())
    val banners: StateFlow<List<BannerItem>> = _banners.asStateFlow()

    private val _tasks = MutableStateFlow(loadTasks())
    val tasks: StateFlow<List<TaskItem>> = _tasks.asStateFlow()

    // Focus Mode Preferences
    private val _isFocusModeActive = MutableStateFlow(prefs.getBoolean(KEY_FOCUS_MODE_ACTIVE, false))
    val isFocusModeActive: StateFlow<Boolean> = _isFocusModeActive.asStateFlow()

    // Break Phase State: During Break, locked apps temporarily unlock & phone un-silents
    private val _isFocusBreakPhase = MutableStateFlow(prefs.getBoolean(KEY_FOCUS_BREAK_PHASE, false))
    val isFocusBreakPhase: StateFlow<Boolean> = _isFocusBreakPhase.asStateFlow()

    private val _studyDurationMinutes = MutableStateFlow(prefs.getInt(KEY_STUDY_DURATION, 25))
    val studyDurationMinutes: StateFlow<Int> = _studyDurationMinutes.asStateFlow()

    private val _breakDurationMinutes = MutableStateFlow(prefs.getInt(KEY_BREAK_DURATION, 5))
    val breakDurationMinutes: StateFlow<Int> = _breakDurationMinutes.asStateFlow()

    private val _longBreakDurationMinutes = MutableStateFlow(prefs.getInt(KEY_LONG_BREAK_DURATION, 15))
    val longBreakDurationMinutes: StateFlow<Int> = _longBreakDurationMinutes.asStateFlow()

    private val _targetSessions = MutableStateFlow(prefs.getInt(KEY_TARGET_SESSIONS, 4))
    val targetSessions: StateFlow<Int> = _targetSessions.asStateFlow()

    // Allowed apps during Focus Mode
    private val _allowedFocusApps = MutableStateFlow(loadAllowedFocusApps())
    val allowedFocusApps: StateFlow<List<String>> = _allowedFocusApps.asStateFlow()

    private val _hideBlockedApps = MutableStateFlow(prefs.getBoolean(KEY_HIDE_BLOCKED_APPS, true))
    val hideBlockedApps: StateFlow<Boolean> = _hideBlockedApps.asStateFlow()

    // App Usage Limits (packageName -> daily limit in minutes)
    private val _appLimits = MutableStateFlow(loadAppLimits())
    val appLimits: StateFlow<Map<String, Int>> = _appLimits.asStateFlow()

    // Badges & Gamification State (35+ badges)
    private val _completedSessionsCount = MutableStateFlow(prefs.getInt(KEY_COMPLETED_SESSIONS, 0))
    val completedSessionsCount: StateFlow<Int> = _completedSessionsCount.asStateFlow()

    private val _totalFocusMinutes = MutableStateFlow(prefs.getInt(KEY_TOTAL_FOCUS_MINUTES, 0))
    val totalFocusMinutes: StateFlow<Int> = _totalFocusMinutes.asStateFlow()

    private val _unlockedBadges = MutableStateFlow(loadUnlockedBadgeIds())
    val unlockedBadges: StateFlow<List<String>> = _unlockedBadges.asStateFlow()

    private val _latestBadge = MutableStateFlow(loadLatestBadge())
    val latestBadge: StateFlow<Badge?> = _latestBadge.asStateFlow()

    // Focus History
    private val _focusHistory = MutableStateFlow(loadFocusHistory())
    val focusHistory: StateFlow<List<FocusSessionRecord>> = _focusHistory.asStateFlow()

    fun setNightMode(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_NIGHT_MODE, enabled).apply()
        _isNightMode.value = enabled
        checkAndUnlockBadges()
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

    fun setShowReminders(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SHOW_REMINDERS, enabled).apply()
        _showReminders.value = enabled
    }

    fun setShowGoals(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SHOW_GOALS, enabled).apply()
        _showGoals.value = enabled
    }

    fun setShowTasks(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SHOW_TASKS, enabled).apply()
        _showTasks.value = enabled
    }

    fun setFocusModeActive(active: Boolean) {
        prefs.edit().putBoolean(KEY_FOCUS_MODE_ACTIVE, active).apply()
        _isFocusModeActive.value = active
        if (!active) {
            setFocusBreakPhase(false)
        }
    }

    fun setFocusBreakPhase(isBreak: Boolean) {
        prefs.edit().putBoolean(KEY_FOCUS_BREAK_PHASE, isBreak).apply()
        _isFocusBreakPhase.value = isBreak
    }

    // Persisted Pomodoro timer state, so a foreground-service restart can resume the
    // in-progress session instead of resetting the timer to a brand-new study block.
    fun getFocusSecondsRemaining(): Int = prefs.getInt(KEY_FOCUS_SECONDS_REMAINING, -1)

    fun setFocusSecondsRemaining(seconds: Int) {
        prefs.edit().putInt(KEY_FOCUS_SECONDS_REMAINING, seconds).apply()
    }

    fun getFocusIsStudying(): Boolean = prefs.getBoolean(KEY_FOCUS_IS_STUDYING, true)

    fun setFocusIsStudying(isStudying: Boolean) {
        prefs.edit().putBoolean(KEY_FOCUS_IS_STUDYING, isStudying).apply()
    }

    fun getFocusCurrentSession(): Int = prefs.getInt(KEY_FOCUS_CURRENT_SESSION, 1)

    fun setFocusCurrentSession(session: Int) {
        prefs.edit().putInt(KEY_FOCUS_CURRENT_SESSION, session).apply()
    }

    fun setStudyDurationMinutes(minutes: Int) {
        val clamped = minutes.coerceIn(1, 120)
        prefs.edit().putInt(KEY_STUDY_DURATION, clamped).apply()
        _studyDurationMinutes.value = clamped
    }

    fun setBreakDurationMinutes(minutes: Int) {
        val clamped = minutes.coerceIn(1, 60)
        prefs.edit().putInt(KEY_BREAK_DURATION, clamped).apply()
        _breakDurationMinutes.value = clamped
    }

    fun setLongBreakDurationMinutes(minutes: Int) {
        val clamped = minutes.coerceIn(1, 90)
        prefs.edit().putInt(KEY_LONG_BREAK_DURATION, clamped).apply()
        _longBreakDurationMinutes.value = clamped
    }

    fun setTargetSessions(sessions: Int) {
        val clamped = sessions.coerceIn(1, 12)
        prefs.edit().putInt(KEY_TARGET_SESSIONS, clamped).apply()
        _targetSessions.value = clamped
    }

    fun setHideBlockedApps(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_HIDE_BLOCKED_APPS, enabled).apply()
        _hideBlockedApps.value = enabled
    }

    fun setAllowedFocusApps(list: List<String>) {
        val array = JSONArray()
        list.forEach { array.put(it) }
        prefs.edit().putString(KEY_ALLOWED_FOCUS_APPS, array.toString()).apply()
        _allowedFocusApps.value = list
    }

    fun toggleAllowedFocusApp(packageName: String) {
        val current = _allowedFocusApps.value.toMutableList()
        if (current.contains(packageName)) {
            current.remove(packageName)
        } else {
            current.add(packageName)
        }
        setAllowedFocusApps(current)
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
        checkAndUnlockBadges()
    }

    fun recordCompletedFocusSession(minutes: Int, sessionType: String = "Focus Session") {
        val newCount = _completedSessionsCount.value + 1
        val newMinutes = _totalFocusMinutes.value + minutes
        prefs.edit()
            .putInt(KEY_COMPLETED_SESSIONS, newCount)
            .putInt(KEY_TOTAL_FOCUS_MINUTES, newMinutes)
            .apply()
        _completedSessionsCount.value = newCount
        _totalFocusMinutes.value = newMinutes

        // Record session to history
        val now = System.currentTimeMillis()
        val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
        val record = FocusSessionRecord(
            timestamp = now,
            durationMinutes = minutes,
            sessionType = sessionType,
            dateFormatted = dateFormat.format(Date(now)),
            timeFormatted = timeFormat.format(Date(now))
        )
        addFocusHistoryRecord(record)

        // Check badge unlocks
        checkAndUnlockBadges(newCount, newMinutes)
    }

    private fun addFocusHistoryRecord(record: FocusSessionRecord) {
        val list = _focusHistory.value.toMutableList()
        list.add(0, record)
        saveFocusHistory(list)
    }

    fun clearAllFocusHistory() {
        saveFocusHistory(emptyList())
    }

    /**
     * Clears focus history records older than the cutoff timestamp (i.e. timestamp < cutoffTimestamp).
     */
    fun clearFocusHistoryBefore(cutoffTimestamp: Long) {
        val list = _focusHistory.value.filter { it.timestamp >= cutoffTimestamp }
        saveFocusHistory(list)
    }

    private fun saveFocusHistory(list: List<FocusSessionRecord>) {
        val array = JSONArray()
        for (item in list) {
            val obj = JSONObject().apply {
                put("id", item.id)
                put("timestamp", item.timestamp)
                put("durationMinutes", item.durationMinutes)
                put("sessionType", item.sessionType)
                put("dateFormatted", item.dateFormatted)
                put("timeFormatted", item.timeFormatted)
            }
            array.put(obj)
        }
        prefs.edit().putString(KEY_FOCUS_HISTORY, array.toString()).apply()
        _focusHistory.value = list
    }

    private fun loadFocusHistory(): List<FocusSessionRecord> {
        val raw = prefs.getString(KEY_FOCUS_HISTORY, null) ?: return emptyList()
        return try {
            val array = JSONArray(raw)
            val list = mutableListOf<FocusSessionRecord>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    FocusSessionRecord(
                        id = obj.getString("id"),
                        timestamp = obj.getLong("timestamp"),
                        durationMinutes = obj.getInt("durationMinutes"),
                        sessionType = obj.optString("sessionType", "Focus Session"),
                        dateFormatted = obj.optString("dateFormatted", ""),
                        timeFormatted = obj.optString("timeFormatted", "")
                    )
                )
            }
            list
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun checkAndUnlockBadges(
        sessions: Int = _completedSessionsCount.value,
        focusMins: Int = _totalFocusMinutes.value
    ) {
        val unlocked = _unlockedBadges.value.toMutableList()
        var newBadgeUnlocked: Badge? = null

        fun tryUnlock(badge: Badge, condition: Boolean) {
            if (condition && !unlocked.contains(badge.id)) {
                unlocked.add(badge.id)
                newBadgeUnlocked = badge
            }
        }

        val allBadges = getAllBadges()
        val tasksCompleted = _tasks.value.count { it.isCompleted }
        val goalsCount = _banners.value.count { it.category.equals("Goal", ignoreCase = true) }
        val notesCount = _banners.value.count { it.category.equals("Note", ignoreCase = true) }
        val remindersCount = _banners.value.count { it.category.equals("Reminder", ignoreCase = true) }
        val limitsCount = _appLimits.value.size
        val isNight = _isNightMode.value

        for (b in allBadges) {
            val condition = when (b.id) {
                // Sessions milestones
                "b_first_session" -> sessions >= 1
                "b_session_2" -> sessions >= 2
                "b_session_3" -> sessions >= 3
                "b_focus_master" -> sessions >= 5
                "b_session_7" -> sessions >= 7
                "b_zen_monk" -> sessions >= 10
                "b_deep_diver" -> sessions >= 15
                "b_flow_state" -> sessions >= 20
                "b_streak_champion" -> sessions >= 25
                "b_session_35" -> sessions >= 35
                "b_session_50" -> sessions >= 50
                "b_session_75" -> sessions >= 75
                "b_session_100" -> sessions >= 100

                // Total Minutes milestones
                "b_mins_15" -> focusMins >= 15
                "b_mins_30" -> focusMins >= 30
                "b_hour_hero" -> focusMins >= 60
                "b_mins_120" -> focusMins >= 120
                "b_mins_180" -> focusMins >= 180
                "b_mins_300" -> focusMins >= 300
                "b_mins_600" -> focusMins >= 600
                "b_mins_1000" -> focusMins >= 1000
                "b_mins_1440" -> focusMins >= 1440

                // App Limits & Digital Wellbeing
                "b_digital_minimalist" -> limitsCount >= 1
                "b_limit_disciplinarian" -> limitsCount >= 3
                "b_limit_guardian" -> limitsCount >= 5

                // Tasks milestones
                "b_task_tackler" -> tasksCompleted >= 1
                "b_task_crusher" -> tasksCompleted >= 5
                "b_task_champion" -> tasksCompleted >= 10

                // Goals, Notes, Reminders & Mindset
                "b_goal_setter" -> goalsCount >= 1
                "b_goal_trio" -> goalsCount >= 3
                "b_note_taker" -> notesCount >= 1
                "b_reminder_pro" -> remindersCount >= 1
                "b_night_scholar" -> isNight && sessions >= 1
                "b_day_achiever" -> !isNight && sessions >= 1
                "b_balanced_mind" -> sessions >= 2
                else -> false
            }
            tryUnlock(b.copy(isUnlocked = true, unlockedTimestamp = System.currentTimeMillis()), condition)
        }

        if (unlocked.size != _unlockedBadges.value.size) {
            val array = JSONArray()
            unlocked.forEach { array.put(it) }
            prefs.edit().putString(KEY_UNLOCKED_BADGES, array.toString()).apply()
            _unlockedBadges.value = unlocked

            if (newBadgeUnlocked != null) {
                saveLatestBadge(newBadgeUnlocked!!)
            }
        }
    }

    private fun saveLatestBadge(badge: Badge) {
        val obj = JSONObject().apply {
            put("id", badge.id)
            put("title", badge.title)
            put("description", badge.description)
            put("iconEmoji", badge.iconEmoji)
            put("unlockedTimestamp", badge.unlockedTimestamp)
        }
        prefs.edit().putString(KEY_LATEST_BADGE, obj.toString()).apply()
        _latestBadge.value = badge
    }

    private fun loadLatestBadge(): Badge? {
        val raw = prefs.getString(KEY_LATEST_BADGE, null) ?: return null
        return try {
            val obj = JSONObject(raw)
            Badge(
                id = obj.getString("id"),
                title = obj.getString("title"),
                description = obj.getString("description"),
                iconEmoji = obj.getString("iconEmoji"),
                isUnlocked = true,
                unlockedTimestamp = obj.optLong("unlockedTimestamp", 0L)
            )
        } catch (_: Exception) {
            null
        }
    }

    /**
     * 35 Distinct Badges across Sessions, Time, Limits, Tasks, and Mindset.
     */
    fun getAllBadges(): List<Badge> {
        val unlockedIds = _unlockedBadges.value.toSet()
        val all = listOf(
            // Sessions milestones (13 badges)
            Badge("b_first_session", "Focus Starter", "Completed your 1st focus session", "🌱"),
            Badge("b_session_2", "Double Spark", "Completed 2 focus sessions", "✌️"),
            Badge("b_session_3", "Triple Spark", "Completed 3 focus sessions", "🥉"),
            Badge("b_focus_master", "Focus Master", "Completed 5 focus sessions", "🔥"),
            Badge("b_session_7", "Weekly Warrior", "Completed 7 focus sessions", "⚡"),
            Badge("b_zen_monk", "Zen Monk", "Completed 10 focus sessions", "🧘"),
            Badge("b_deep_diver", "Deep Diver", "Completed 15 focus sessions", "🌊"),
            Badge("b_flow_state", "Flow State", "Completed 20 focus sessions", "🌀"),
            Badge("b_streak_champion", "Mindful Master", "Completed 25 focus sessions", "👑"),
            Badge("b_session_35", "Iron Will", "Completed 35 focus sessions", "🛡️"),
            Badge("b_session_50", "Centurion of Focus", "Completed 50 focus sessions", "🏛️"),
            Badge("b_session_75", "Grand Sage", "Completed 75 focus sessions", "🔮"),
            Badge("b_session_100", "Focus Legend", "Completed 100 focus sessions", "💎"),

            // Total Minutes milestones (9 badges)
            Badge("b_mins_15", "Quick Sitter", "Logged 15+ minutes of focus", "⏱️"),
            Badge("b_mins_30", "Half-Hour Hero", "Logged 30+ minutes of focus", "⏳"),
            Badge("b_hour_hero", "Hour Hero", "Logged 60+ minutes in deep focus", "⚡"),
            Badge("b_mins_120", "Double Hour", "Logged 120+ minutes of focus", "🚀"),
            Badge("b_mins_180", "3-Hour Titan", "Logged 180+ minutes of focus", "🛡️"),
            Badge("b_mins_300", "5-Hour Marathoner", "Logged 300+ minutes of focus", "🎯"),
            Badge("b_mins_600", "10-Hour Sage", "Logged 600+ minutes of focus", "🌟"),
            Badge("b_mins_1000", "Millennium Club", "Logged 1,000+ minutes of focus", "🏆"),
            Badge("b_mins_1440", "Day of Discipline", "Logged 24 hours of focus time", "☀️"),

            // Digital Wellbeing & Limits (3 badges)
            Badge("b_digital_minimalist", "Digital Minimalist", "Configured daily app use limits", "🛡️"),
            Badge("b_limit_disciplinarian", "Strict Boundary", "Configured 3 or more app limits", "🔒"),
            Badge("b_limit_guardian", "Digital Guardian", "Configured 5 or more app limits", "🔕"),

            // Tasks milestones (3 badges)
            Badge("b_task_tackler", "Task Tackler", "Checked off your first task", "✅"),
            Badge("b_task_crusher", "Task Crusher", "Completed 5 tasks in checklist", "📋"),
            Badge("b_task_champion", "Task Champion", "Completed 10 tasks in checklist", "🥇"),

            // Goals, Notes & Mindset (7 badges)
            Badge("b_goal_setter", "Visionary", "Created your first personal Goal card", "🚩"),
            Badge("b_goal_trio", "Goal Hunter", "Created 3 or more Goals", "🎯"),
            Badge("b_note_taker", "Thinker", "Saved your personal Notes", "📝"),
            Badge("b_reminder_pro", "On Time", "Created Reminders in widget", "🔔"),
            Badge("b_night_scholar", "Night Scholar", "Focused during Night theme", "🌙"),
            Badge("b_day_achiever", "Dawn Achiever", "Focused during Day theme", "🌅"),
            Badge("b_balanced_mind", "Balanced Mind", "Took a mindful Pomodoro break", "☕")
        )
        return all.map { b ->
            if (unlockedIds.contains(b.id)) b.copy(isUnlocked = true) else b
        }
    }

    fun getSavedInterruptionFilter(): Int {
        return prefs.getInt(KEY_PREVIOUS_INTERRUPTION_FILTER, NotificationManager.INTERRUPTION_FILTER_ALL)
    }

    fun setSavedInterruptionFilter(filter: Int) {
        prefs.edit().putInt(KEY_PREVIOUS_INTERRUPTION_FILTER, filter).apply()
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
        checkAndUnlockBadges()
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
        checkAndUnlockBadges()
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

    private fun loadAllowedFocusApps(): List<String> {
        val raw = prefs.getString(KEY_ALLOWED_FOCUS_APPS, null) ?: return emptyList()
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

    private fun loadUnlockedBadgeIds(): List<String> {
        val raw = prefs.getString(KEY_UNLOCKED_BADGES, null) ?: return emptyList()
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

    companion object {
        private const val KEY_NIGHT_MODE = "is_night_mode"
        private const val KEY_FAVORITES = "favorite_packages"
        private const val KEY_SHOW_TIME = "show_time"
        private const val KEY_SHOW_DATE = "show_date"
        private const val KEY_SHOW_NOTES = "show_notes"
        private const val KEY_SHOW_REMINDERS = "show_reminders"
        private const val KEY_SHOW_GOALS = "show_goals"
        private const val KEY_SHOW_TASKS = "show_tasks"
        private const val KEY_BANNERS = "banners_data"
        private const val KEY_TASKS = "tasks_data"

        // Focus Mode Keys
        private const val KEY_FOCUS_MODE_ACTIVE = "focus_mode_active"
        private const val KEY_FOCUS_BREAK_PHASE = "focus_break_phase"
        private const val KEY_STUDY_DURATION = "study_duration"
        private const val KEY_BREAK_DURATION = "break_duration"
        private const val KEY_LONG_BREAK_DURATION = "long_break_duration"
        private const val KEY_TARGET_SESSIONS = "target_sessions"
        private const val KEY_ALLOWED_FOCUS_APPS = "allowed_focus_apps"
        private const val KEY_HIDE_BLOCKED_APPS = "hide_blocked_apps"
        private const val KEY_PREVIOUS_INTERRUPTION_FILTER = "prev_interruption_filter"
        private const val KEY_FOCUS_SECONDS_REMAINING = "focus_seconds_remaining"
        private const val KEY_FOCUS_IS_STUDYING = "focus_is_studying"
        private const val KEY_FOCUS_CURRENT_SESSION = "focus_current_session"

        // App Limits & Badges
        private const val KEY_APP_LIMITS = "app_limits_data"
        private const val KEY_COMPLETED_SESSIONS = "completed_sessions"
        private const val KEY_TOTAL_FOCUS_MINUTES = "total_focus_minutes"
        private const val KEY_UNLOCKED_BADGES = "unlocked_badges"
        private const val KEY_LATEST_BADGE = "latest_badge"
        private const val KEY_FOCUS_HISTORY = "focus_history_records"
    }
}
