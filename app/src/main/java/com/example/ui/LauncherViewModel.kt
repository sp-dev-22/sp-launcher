package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.AppInfo
import com.example.data.model.Badge
import com.example.data.model.BannerItem
import com.example.data.model.FocusSessionRecord
import com.example.data.model.TaskItem
import com.example.data.repository.AppRepository
import com.example.data.repository.LauncherPreferences
import com.example.focus.FocusManager
import com.example.focus.UsageTracker
import com.example.ui.components.BlockReason
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
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
    val showReminders = preferences.showReminders
    val showGoals = preferences.showGoals
    val showTasks = preferences.showTasks
    val banners = preferences.banners
    val tasks = preferences.tasks

    // Categorized lists for separate Notes, Reminders, and Goals widgets
    val notesList: StateFlow<List<BannerItem>> = banners.combine(banners) { list, _ ->
        list.filter { it.category.equals("Note", ignoreCase = true) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val remindersList: StateFlow<List<BannerItem>> = banners.combine(banners) { list, _ ->
        list.filter { it.category.equals("Reminder", ignoreCase = true) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val goalsList: StateFlow<List<BannerItem>> = banners.combine(banners) { list, _ ->
        list.filter { it.category.equals("Goal", ignoreCase = true) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

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

    // Badges & Milestones
    val completedSessionsCount = preferences.completedSessionsCount
    val totalFocusMinutes = preferences.totalFocusMinutes
    val latestBadge = preferences.latestBadge
    val unlockedBadges = preferences.unlockedBadges

    // Focus History
    val focusHistory: StateFlow<List<FocusSessionRecord>> = preferences.focusHistory

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

    private val _showBadgesSheet = MutableStateFlow(false)
    val showBadgesSheet: StateFlow<Boolean> = _showBadgesSheet.asStateFlow()

    private val _showEndFocusConfirmDialog = MutableStateFlow(false)
    val showEndFocusConfirmDialog: StateFlow<Boolean> = _showEndFocusConfirmDialog.asStateFlow()

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

    fun setShowReminders(enabled: Boolean) {
        preferences.setShowReminders(enabled)
    }

    fun setShowGoals(enabled: Boolean) {
        preferences.setShowGoals(enabled)
    }

    fun setShowTasks(enabled: Boolean) {
        preferences.setShowTasks(enabled)
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

    fun setShowBadgesSheet(show: Boolean) {
        _showBadgesSheet.value = show
    }

    fun setShowEndFocusConfirmDialog(show: Boolean) {
        _showEndFocusConfirmDialog.value = show
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

    fun getAllBadges(): List<Badge> = preferences.getAllBadges()

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

}
