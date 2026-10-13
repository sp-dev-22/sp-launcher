package com.example.ui

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.AppInfo
import com.example.data.model.BannerItem
import com.example.data.model.StudyCourse
import com.example.data.model.StudyDueItem
import com.example.data.model.allClassRefs
import com.example.data.model.TaskItem
import com.example.data.repository.AppRepository
import com.example.data.repository.LauncherPreferences
import com.example.focus.UsageTracker
import com.example.service.AppLimitService
import com.example.service.BackupReminderScheduler
import com.example.service.StudyReminderScheduler
import com.example.ui.components.BlockReason
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class LauncherViewModel(application: Application) : AndroidViewModel(application) {

    private val appRepository = AppRepository(application)
    private val preferences = LauncherPreferences(application)
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
    val showStudyDue = preferences.showStudyDue
    val themeKey = preferences.themeKey
    val banners = preferences.banners
    val tasks = preferences.tasks

    // App Usage & Limits states
    val appLimits = preferences.appLimits
    private val _usageMap = MutableStateFlow<Map<String, Int>>(emptyMap())
    val usageMap: StateFlow<Map<String, Int>> = _usageMap.asStateFlow()

    // Today's totals for the Home widgets
    val totalScreenTimeToday: StateFlow<Int> = usageMap
        .map { usage -> usage.values.sum() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

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

    private val _showAppLimitsSheet = MutableStateFlow(false)
    val showAppLimitsSheet: StateFlow<Boolean> = _showAppLimitsSheet.asStateFlow()

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

    // Filtered apps for the App Drawer (search only)
    val filteredApps: StateFlow<List<AppInfo>> =
        kotlinx.coroutines.flow.combine(_installedApps, _searchQuery) { apps, query ->
            if (query.isBlank()) apps
            else apps.filter { it.label.contains(query, ignoreCase = true) }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Favorite apps for the Home Screen
    val favoriteAppInfos: StateFlow<List<AppInfo>> =
        kotlinx.coroutines.flow.combine(_installedApps, favorites) { apps, favPkgs ->
            val appMap = apps.associateBy { it.packageName }
            favPkgs.mapNotNull { appMap[it] }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        loadApps()
        refreshUsageStats()
        // Keep the daily app-limit monitor running if any limit is set.
        if (preferences.appLimits.value.isNotEmpty()) {
            AppLimitService.start(getApplication())
        }
        // Re-arm any pending Study Manager reminders on launch.
        rescheduleStudyReminders()
        // Arm the once-a-day backup reminder (19:00).
        BackupReminderScheduler.apply(getApplication())
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

    fun setShowStudyDue(enabled: Boolean) {
        preferences.setShowStudyDue(enabled)
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

    fun setShowAppLimitsSheet(show: Boolean) {
        _showAppLimitsSheet.value = show
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

    // ---- Overlay permission (needed for the daily-limit lock overlay) ----

    fun canDrawOverlays(): Boolean = Settings.canDrawOverlays(getApplication())

    fun requestOverlayPermission() {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:${getApplication<Application>().packageName}")
        ).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            getApplication<Application>().startActivity(intent)
        } catch (_: Exception) {
            val fallback = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            try {
                getApplication<Application>().startActivity(fallback)
            } catch (_: Exception) {}
        }
    }

    fun hasUsagePermission(): Boolean = usageTracker.hasUsageStatsPermission()

    fun requestUsagePermission() {
        usageTracker.requestUsageStatsPermission()
    }

    fun setAppLimit(packageName: String, limitMinutes: Int) {
        preferences.setAppLimit(packageName, limitMinutes)
        // Ensure the background monitor is actively running to enforce limits live
        AppLimitService.start(getApplication())
    }

    // Smart App Launching: handles usage limit blocking
    fun tryLaunchApp(app: AppInfo) {
        // Check daily usage limit
        val limit = appLimits.value[app.packageName] ?: 0
        val used = _usageMap.value[app.packageName] ?: 0
        if (limit > 0 && used >= limit) {
            _blockedAppDetails.value = Pair(app, BlockReason.USAGE_LIMIT_EXCEEDED)
            return
        }

        // Otherwise launch normally
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
