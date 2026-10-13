package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.AppInfo
import com.example.data.model.BannerItem
import com.example.data.model.FocusSessionRecord
import com.example.data.model.StudyCourse
import com.example.data.model.StudyDueItem
import com.example.data.model.StudyFocusTarget
import com.example.data.model.allClassRefs
import com.example.data.model.TaskItem
import com.example.data.repository.AppRepository
import com.example.data.repository.LauncherPreferences
import com.example.focus.FocusManager
import com.example.focus.UsageTracker
import com.example.service.AutoBackupScheduler
import com.example.service.StudyReminderScheduler
import com.example.ui.components.BlockReason
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class FocusFilterState(
    val isFocus: Boolean,
    val isBreak: Boolean,
    val allowedApps: Set<String>,
    val shouldHide: Boolean
)

class LauncherViewModel(application: Application) : AndroidViewModel(application) {

    private val appRepository = AppRepository(application)
    private val preferences = LauncherPreferences(application)
    private val focusManager = FocusManager(application)
    private val usageTracker = UsageTracker(application)

    private val _installedApps = MutableStateFlow<List<AppInfo>>(emptyList())
    val installedApps: StateFlow<List<AppInfo>> = _installedApps.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val isNightMode = preferences.isNightMode
    val favorites = preferences.favorites
    val showTime = preferences.showTime
    val showDate = preferences.showDate
    val showNotes = preferences.showNotes
    val showTasks = preferences.showTasks
    val showScreenTime = preferences.showScreenTime
    val showFocusTime = preferences.showFocusTime
    val showStudyDue = preferences.showStudyDue
    val showStudySummary = preferences.showStudySummary
    val studySummaryHour = preferences.studySummaryHour
    val studySummaryMinute = preferences.studySummaryMinute
    val autoBackupEnabled = preferences.autoBackupEnabled
    val autoBackupUri = preferences.autoBackupUri
    val autoBackupLast = preferences.autoBackupLast
    val themeKey = preferences.themeKey
    val banners = preferences.banners
    val tasks = preferences.tasks

    // Focus Mode states
    val isFocusModeActive = preferences.isFocusModeActive
    val isFocusBreakPhase = preferences.isFocusBreakPhase
    val studyDurationMinutes = preferences.studyDurationMinutes
    val breakDurationMinutes = preferences.breakDurationMinutes
    val longBreakDurationMinutes = preferences.longBreakDurationMinutes
    val targetSessions = preferences.targetSessions
    val allowedFocusApps = preferences.allowedFocusApps
    val hideBlockedApps = preferences.hideBlockedApps

    // App Usage & Limits states
    val appLimits = preferences.appLimits
    private val _usageMap = MutableStateFlow<Map<String, Int>>(emptyMap())
    val usageMap: StateFlow<Map<String, Int>> = _usageMap.asStateFlow()

    // Focus session totals
    val completedSessionsCount = preferences.completedSessionsCount
    val totalFocusMinutes = preferences.totalFocusMinutes

    // Focus History
    val focusHistory: StateFlow<List<FocusSessionRecord>> = preferences.focusHistory

    // Today's totals for the Home widgets
    val totalScreenTimeToday: StateFlow<Int> = usageMap
        .map { usage -> usage.values.sum() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val totalFocusedTimeToday: StateFlow<Int> = focusHistory
        .map { list ->
            val now = System.currentTimeMillis()
            list.filter { isSameDay(it.timestamp, now) }.sumOf { it.durationMinutes }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    private fun isSameDay(a: Long, b: Long): Boolean {
        val calA = java.util.Calendar.getInstance().apply { timeInMillis = a }
        val calB = java.util.Calendar.getInstance().apply { timeInMillis = b }
        return calA.get(java.util.Calendar.YEAR) == calB.get(java.util.Calendar.YEAR) &&
            calA.get(java.util.Calendar.DAY_OF_YEAR) == calB.get(java.util.Calendar.DAY_OF_YEAR)
    }

    // Study Manager tree
    val studyCourses: StateFlow<List<StudyCourse>> = preferences.studyCourses

    // Study classes due today or overdue (Home "Due today" card)
    val studyDueItems: StateFlow<List<StudyDueItem>> = studyCourses
        .map { courses -> computeStudyDue(courses) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private fun computeStudyDue(courses: List<StudyCourse>): List<StudyDueItem> {
        val now = System.currentTimeMillis()
        val endOfToday = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, 23)
            set(java.util.Calendar.MINUTE, 59)
            set(java.util.Calendar.SECOND, 59)
        }.timeInMillis
        val items = mutableListOf<StudyDueItem>()
        for (course in courses) {
            for (ref in course.allClassRefs()) {
                if (ref.clazz.isCompleted) continue
                val deadline = ref.clazz.deadlineMillis ?: continue
                val overdue = deadline < now
                val dueToday = deadline in now..endOfToday
                if (overdue || dueToday) {
                    items.add(StudyDueItem("${ref.topic.name} · ${ref.clazz.name}", deadline, overdue))
                }
            }
        }
        return items.sortedBy { it.deadlineMillis }
    }

    // Dialog & Sheet States
    private val _selectedAppForActions = MutableStateFlow<AppInfo?>(null)
    val selectedAppForActions: StateFlow<AppInfo?> = _selectedAppForActions.asStateFlow()

    private val _showArrangeSheet = MutableStateFlow(false)
    val showArrangeSheet: StateFlow<Boolean> = _showArrangeSheet.asStateFlow()

    private val _showAddFavoritesScreen = MutableStateFlow(false)
    val showAddFavoritesScreen: StateFlow<Boolean> = _showAddFavoritesScreen.asStateFlow()

    private val _showWidgetsDialog = MutableStateFlow(false)
    val showWidgetsDialog: StateFlow<Boolean> = _showWidgetsDialog.asStateFlow()

    private val _showFocusSettingsDialog = MutableStateFlow(false)
    val showFocusSettingsDialog: StateFlow<Boolean> = _showFocusSettingsDialog.asStateFlow()

    private val _showFocusHistorySheet = MutableStateFlow(false)
    val showFocusHistorySheet: StateFlow<Boolean> = _showFocusHistorySheet.asStateFlow()

    private val _showAllowedAppsSheet = MutableStateFlow(false)
    val showAllowedAppsSheet: StateFlow<Boolean> = _showAllowedAppsSheet.asStateFlow()

    private val _showAppLimitsSheet = MutableStateFlow(false)
    val showAppLimitsSheet: StateFlow<Boolean> = _showAppLimitsSheet.asStateFlow()

    private val _showEndFocusConfirmDialog = MutableStateFlow(false)
    val showEndFocusConfirmDialog: StateFlow<Boolean> = _showEndFocusConfirmDialog.asStateFlow()

    private val _showStudyManager = MutableStateFlow(false)
    val showStudyManager: StateFlow<Boolean> = _showStudyManager.asStateFlow()

    private val _showBackupRestore = MutableStateFlow(false)
    val showBackupRestore: StateFlow<Boolean> = _showBackupRestore.asStateFlow()

    private val _showThemesSheet = MutableStateFlow(false)
    val showThemesSheet: StateFlow<Boolean> = _showThemesSheet.asStateFlow()

    private val _showAboutDev = MutableStateFlow(false)
    val showAboutDev: StateFlow<Boolean> = _showAboutDev.asStateFlow()

    private val _blockedAppDetails = MutableStateFlow<Pair<AppInfo, BlockReason>?>(null)
    val blockedAppDetails: StateFlow<Pair<AppInfo, BlockReason>?> = _blockedAppDetails.asStateFlow()

    // Combined focus filter state flow
    private val focusFilterState = combine(
        isFocusModeActive,
        isFocusBreakPhase,
        allowedFocusApps,
        hideBlockedApps
    ) { isFocus, isBreak, allowedList, shouldHide ->
        FocusFilterState(
            isFocus = isFocus,
            isBreak = isBreak,
            allowedApps = allowedList.toSet(),
            shouldHide = shouldHide
        )
    }

    // Filtered apps for Drawer: during Break phase, apps temporarily unhide and are accessible!
    val filteredApps: StateFlow<List<AppInfo>> =
        combine(_installedApps, _searchQuery, focusFilterState) { apps, query, focusState ->
            var list = apps

            // If focus mode is active, NOT in break phase, and hideBlockedApps is enabled: hide non-allowed apps
            if (focusState.isFocus && !focusState.isBreak && focusState.shouldHide) {
                list = list.filter { focusState.allowedApps.contains(it.packageName) }
            }

            if (query.isNotBlank()) {
                list = list.filter { it.label.contains(query, ignoreCase = true) }
            }
            list
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Filtered favorites for Home Screen: during Break phase, apps unhide!
    val favoriteAppInfos: StateFlow<List<AppInfo>> =
        combine(_installedApps, favorites, focusFilterState) { apps, favPkgs, focusState ->
            val appMap = apps.associateBy { it.packageName }
            var favs = favPkgs.mapNotNull { appMap[it] }

            if (focusState.isFocus && !focusState.isBreak && focusState.shouldHide) {
                favs = favs.filter { focusState.allowedApps.contains(it.packageName) }
            }
            favs
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        loadApps()
        refreshUsageStats()
        // Ensure background monitor is running if limits are set or focus mode was active
        if (preferences.appLimits.value.isNotEmpty() || preferences.isFocusModeActive.value) {
            focusManager.updateLimitMonitoring()
        }
        // Re-arm any pending Study Manager reminders on launch.
        rescheduleStudyReminders()
        StudyReminderScheduler.applyDailySummary(
            getApplication<Application>(),
            preferences.showStudySummary.value,
            preferences.studySummaryHour.value,
            preferences.studySummaryMinute.value
        )
        AutoBackupScheduler.apply(getApplication<Application>(), preferences.autoBackupEnabled.value)
    }

    fun loadApps() {
        viewModelScope.launch {
            val apps = appRepository.getInstalledApps()
            _installedApps.value = apps

            if (favorites.value.isEmpty() && apps.isNotEmpty()) {
                val preferredKeywords = listOf("phone", "message", "camera", "gallery", "chrome", "browser", "whatsapp", "music", "contact", "file")
                val seed = mutableListOf<String>()

                for (kw in preferredKeywords) {
                    val match = apps.firstOrNull {
                        it.label.contains(kw, ignoreCase = true) || it.packageName.contains(kw, ignoreCase = true)
                    }
                    if (match != null && !seed.contains(match.packageName)) {
                        seed.add(match.packageName)
                    }
                }

                if (seed.size < 6) {
                    for (app in apps) {
                        if (!seed.contains(app.packageName)) {
                            seed.add(app.packageName)
                            if (seed.size >= 8) break
                        }
                    }
                }
                preferences.saveFavorites(seed)
            }
        }
    }

    fun refreshUsageStats() {
        viewModelScope.launch {
            if (usageTracker.hasUsageStatsPermission()) {
                _usageMap.value = usageTracker.getTodayAppUsageMinutes()
            }
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setNightMode(enabled: Boolean) {
        preferences.setNightMode(enabled)
    }

    fun setShowTime(enabled: Boolean) {
        preferences.setShowTime(enabled)
    }

    fun setShowDate(enabled: Boolean) {
        preferences.setShowDate(enabled)
    }

    fun setShowNotes(enabled: Boolean) {
        preferences.setShowNotes(enabled)
    }

    fun setShowTasks(enabled: Boolean) {
        preferences.setShowTasks(enabled)
    }

    fun setShowScreenTime(enabled: Boolean) {
        preferences.setShowScreenTime(enabled)
    }

    fun setShowFocusTime(enabled: Boolean) {
        preferences.setShowFocusTime(enabled)
    }

    fun setShowStudyDue(enabled: Boolean) {
        preferences.setShowStudyDue(enabled)
    }

    fun setStudySummary(enabled: Boolean, hour: Int, minute: Int) {
        preferences.setStudySummary(enabled, hour, minute)
        StudyReminderScheduler.applyDailySummary(getApplication<Application>(), enabled, hour, minute)
    }

    fun setAutoBackup(enabled: Boolean) {
        preferences.setAutoBackupEnabled(enabled)
        AutoBackupScheduler.apply(getApplication<Application>(), enabled)
    }

    fun setAutoBackupFolder(uri: String) {
        preferences.setAutoBackupUri(uri)
        preferences.setAutoBackupEnabled(true)
        AutoBackupScheduler.apply(getApplication<Application>(), true)
    }

    fun setTheme(key: String) {
        preferences.setTheme(key)
    }

    fun addFavorite(packageName: String) {
        preferences.addFavorite(packageName)
    }

    fun removeFavorite(packageName: String) {
        preferences.removeFavorite(packageName)
    }

    fun toggleFavorite(packageName: String) {
        preferences.toggleFavorite(packageName)
    }

    fun reorderFavorites(newOrder: List<String>) {
        preferences.saveFavorites(newOrder)
    }

    fun addTask(text: String) {
        preferences.addTask(text)
    }

    fun toggleTask(id: String) {
        preferences.toggleTask(id)
    }

    fun removeTask(id: String) {
        preferences.removeTask(id)
    }

    fun addBanner(banner: BannerItem) {
        preferences.addBanner(banner)
    }

    fun removeBanner(id: String) {
        preferences.removeBanner(id)
    }

    fun selectAppForActions(app: AppInfo?) {
        _selectedAppForActions.value = app
    }

    fun setShowArrangeSheet(show: Boolean) {
        _showArrangeSheet.value = show
    }

    fun setShowAddFavoritesScreen(show: Boolean) {
        _showAddFavoritesScreen.value = show
    }

    fun setShowWidgetsDialog(show: Boolean) {
        _showWidgetsDialog.value = show
    }

    fun setShowFocusSettingsDialog(show: Boolean) {
        _showFocusSettingsDialog.value = show
    }

    fun setShowFocusHistorySheet(show: Boolean) {
        _showFocusHistorySheet.value = show
    }

    fun setShowAllowedAppsSheet(show: Boolean) {
        _showAllowedAppsSheet.value = show
    }

    fun setShowAppLimitsSheet(show: Boolean) {
        _showAppLimitsSheet.value = show
    }

    fun setShowEndFocusConfirmDialog(show: Boolean) {
        _showEndFocusConfirmDialog.value = show
    }

    fun setShowStudyManager(show: Boolean) {
        _showStudyManager.value = show
    }

    fun setShowBackupRestore(show: Boolean) {
        _showBackupRestore.value = show
    }

    fun setShowThemesSheet(show: Boolean) {
        _showThemesSheet.value = show
    }

    fun setShowAboutDev(show: Boolean) {
        _showAboutDev.value = show
    }

    fun dismissBlockedAppDialog() {
        _blockedAppDetails.value = null
    }

    // Focus Mode methods
    fun canDrawOverlays(): Boolean = focusManager.canDrawOverlays()

    fun requestOverlayPermission() {
        focusManager.requestOverlayPermission()
    }

    fun isNotificationPolicyAccessGranted(): Boolean = focusManager.isNotificationPolicyAccessGranted()

    fun requestNotificationPolicyAccess() {
        focusManager.requestNotificationPolicyAccess()
    }

    fun hasUsagePermission(): Boolean = usageTracker.hasUsageStatsPermission()

    fun requestUsagePermission() {
        usageTracker.requestUsageStatsPermission()
    }

    fun startFocusMode() {
        focusManager.startFocusMode()
    }

    fun stopFocusMode() {
        focusManager.stopFocusMode()
        _showEndFocusConfirmDialog.value = false
    }

    fun toggleFocusMode() {
        if (isFocusModeActive.value) {
            stopFocusMode()
        } else {
            startFocusMode()
        }
    }

    fun setStudyDurationMinutes(minutes: Int) {
        preferences.setStudyDurationMinutes(minutes)
    }

    fun setBreakDurationMinutes(minutes: Int) {
        preferences.setBreakDurationMinutes(minutes)
    }

    fun setLongBreakDurationMinutes(minutes: Int) {
        preferences.setLongBreakDurationMinutes(minutes)
    }

    fun setTargetSessions(sessions: Int) {
        preferences.setTargetSessions(sessions)
    }

    fun toggleAllowedFocusApp(packageName: String) {
        preferences.toggleAllowedFocusApp(packageName)
    }

    fun setHideBlockedApps(enabled: Boolean) {
        preferences.setHideBlockedApps(enabled)
    }

    fun setAppLimit(packageName: String, limitMinutes: Int) {
        preferences.setAppLimit(packageName, limitMinutes)
        // Ensure background monitor is actively running to enforce limits live
        focusManager.updateLimitMonitoring()
    }

    fun clearAllFocusHistory() {
        preferences.clearAllFocusHistory()
    }

    fun clearFocusHistoryOlderThan(cutoffTimestamp: Long) {
        preferences.clearFocusHistoryBefore(cutoffTimestamp)
    }

    // Smart App Launching: handles usage limit and focus mode blocking
    fun tryLaunchApp(app: AppInfo) {
        // 1. Check daily usage limit
        val limit = appLimits.value[app.packageName] ?: 0
        val used = _usageMap.value[app.packageName] ?: 0
        if (limit > 0 && used >= limit) {
            _blockedAppDetails.value = Pair(app, BlockReason.USAGE_LIMIT_EXCEEDED)
            return
        }

        // 2. Check focus mode restriction (only when in Study phase, not in Break phase!)
        if (isFocusModeActive.value && !isFocusBreakPhase.value) {
            val allowedSet = allowedFocusApps.value.toSet()
            if (!allowedSet.contains(app.packageName)) {
                _blockedAppDetails.value = Pair(app, BlockReason.FOCUS_MODE_RESTRICTED)
                return
            }
        }

        // 3. Otherwise launch normally
        appRepository.launchApp(app.packageName)
    }

    fun openAppInfo(packageName: String) {
        appRepository.openAppInfo(packageName)
    }

    fun uninstallApp(packageName: String) {
        appRepository.uninstallApp(packageName)
    }

    // ---------------------------------------------------------------------
    // Study Manager
    // ---------------------------------------------------------------------

    fun addStudyCourse(name: String) {
        preferences.addStudyCourse(name)
        rescheduleStudyReminders()
    }

    fun addStudySubject(courseId: String, name: String) {
        preferences.addStudySubject(courseId, name)
        rescheduleStudyReminders()
    }

    fun addStudyTopic(courseId: String, subjectId: String, name: String) {
        preferences.addStudyTopic(courseId, subjectId, name)
        rescheduleStudyReminders()
    }

    fun addStudyClass(courseId: String, subjectId: String, topicId: String, name: String) {
        preferences.addStudyClass(courseId, subjectId, topicId, name)
        rescheduleStudyReminders()
    }

    fun toggleStudyClass(courseId: String, subjectId: String, topicId: String, classId: String) {
        preferences.toggleStudyClass(courseId, subjectId, topicId, classId)
        rescheduleStudyReminders()
    }

    fun deleteStudyCourse(courseId: String) {
        preferences.deleteStudyCourse(courseId)
        rescheduleStudyReminders()
    }

    fun deleteStudySubject(courseId: String, subjectId: String) {
        preferences.deleteStudySubject(courseId, subjectId)
        rescheduleStudyReminders()
    }

    fun deleteStudyTopic(courseId: String, subjectId: String, topicId: String) {
        preferences.deleteStudyTopic(courseId, subjectId, topicId)
        rescheduleStudyReminders()
    }

    fun deleteStudyClass(courseId: String, subjectId: String, topicId: String, classId: String) {
        preferences.deleteStudyClass(courseId, subjectId, topicId, classId)
        rescheduleStudyReminders()
    }

    fun setStudyTopicSchedule(
        courseId: String, subjectId: String, topicId: String,
        deadlineMillis: Long?, alertMillis: Long?
    ) {
        preferences.setStudyTopicSchedule(courseId, subjectId, topicId, deadlineMillis, alertMillis)
        rescheduleStudyReminders()
    }

    fun setStudyClassSchedule(
        courseId: String, subjectId: String, topicId: String, classId: String,
        deadlineMillis: Long?, alertMillis: Long?
    ) {
        preferences.setStudyClassSchedule(courseId, subjectId, topicId, classId, deadlineMillis, alertMillis)
        rescheduleStudyReminders()
    }

    fun renameStudyCourse(courseId: String, name: String) = preferences.renameStudyCourse(courseId, name)

    fun renameStudySubject(courseId: String, subjectId: String, name: String) =
        preferences.renameStudySubject(courseId, subjectId, name)

    fun renameStudyTopic(courseId: String, subjectId: String, topicId: String, name: String) =
        preferences.renameStudyTopic(courseId, subjectId, topicId, name)

    fun renameStudyClass(courseId: String, subjectId: String, topicId: String, classId: String, name: String) =
        preferences.renameStudyClass(courseId, subjectId, topicId, classId, name)

    /** Starts a Focus session aimed at a specific Study node (Study <-> Focus link). */
    fun startFocusForStudy(target: StudyFocusTarget) {
        preferences.setFocusTarget(target)
        focusManager.startFocusMode()
    }

    private fun rescheduleStudyReminders() {
        StudyReminderScheduler.rescheduleAll(getApplication<Application>(), preferences.studyCourses.value)
    }

    // ---------------------------------------------------------------------
    // Backup & Restore
    // ---------------------------------------------------------------------

    fun exportAllData(): String = preferences.exportAllData()

    fun importAllData(json: String): Boolean {
        val ok = preferences.importAllData(json)
        if (ok) rescheduleStudyReminders()
        return ok
    }

}
