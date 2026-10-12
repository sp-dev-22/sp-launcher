package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import com.example.data.model.AppInfo
import com.example.ui.components.AllowedAppsPickerSheet
import com.example.ui.components.AppActionBottomSheet
import com.example.ui.components.AppBlockedDialog
import com.example.ui.components.AppLimitsSheet
import com.example.ui.components.BadgesSheet
import com.example.ui.components.BlockReason
import com.example.ui.components.EditFavoritesScreen
import com.example.ui.components.FavoritesReorderBottomSheet
import com.example.ui.components.FocusHistorySheet
import com.example.ui.components.FocusModeSheet
import com.example.ui.components.WidgetsManagementSheet
import com.example.ui.screens.DrawerScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.SettingsScreen
import kotlinx.coroutines.launch

@Composable
fun MainScreen(viewModel: LauncherViewModel) {
    val pagerState = rememberPagerState(initialPage = 1, pageCount = { 3 })
    val coroutineScope = rememberCoroutineScope()

    val isDarkTheme by viewModel.isNightMode.collectAsState()
    val allApps by viewModel.installedApps.collectAsState()
    val filteredApps by viewModel.filteredApps.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val favoriteAppInfos by viewModel.favoriteAppInfos.collectAsState()
    val favoritePackages by viewModel.favorites.collectAsState()

    val showTime by viewModel.showTime.collectAsState()
    val showDate by viewModel.showDate.collectAsState()
    val showNotes by viewModel.showNotes.collectAsState()
    val showReminders by viewModel.showReminders.collectAsState()
    val showGoals by viewModel.showGoals.collectAsState()
    val showTasks by viewModel.showTasks.collectAsState()

    val notesList by viewModel.notesList.collectAsState()
    val remindersList by viewModel.remindersList.collectAsState()
    val goalsList by viewModel.goalsList.collectAsState()
    val banners by viewModel.banners.collectAsState()
    val tasks by viewModel.tasks.collectAsState()

    // Focus Mode states
    val isFocusModeActive by viewModel.isFocusModeActive.collectAsState()
    val studyDurationMinutes by viewModel.studyDurationMinutes.collectAsState()
    val breakDurationMinutes by viewModel.breakDurationMinutes.collectAsState()
    val longBreakDurationMinutes by viewModel.longBreakDurationMinutes.collectAsState()
    val targetSessions by viewModel.targetSessions.collectAsState()
    val allowedFocusApps by viewModel.allowedFocusApps.collectAsState()
    val hideBlockedApps by viewModel.hideBlockedApps.collectAsState()

    // Usage tracking, Limits, Badges & History
    val usageMap by viewModel.usageMap.collectAsState()
    val appLimits by viewModel.appLimits.collectAsState()
    val latestBadge by viewModel.latestBadge.collectAsState()
    val completedSessions by viewModel.completedSessionsCount.collectAsState()
    val totalFocusMinutes by viewModel.totalFocusMinutes.collectAsState()
    val focusHistory by viewModel.focusHistory.collectAsState()

    val selectedAppForActions by viewModel.selectedAppForActions.collectAsState()
    val showArrangeSheet by viewModel.showArrangeSheet.collectAsState()
    val showAddFavoritesScreen by viewModel.showAddFavoritesScreen.collectAsState()
    val showWidgetsDialog by viewModel.showWidgetsDialog.collectAsState()
    val showFocusSettingsDialog by viewModel.showFocusSettingsDialog.collectAsState()
    val showFocusHistorySheet by viewModel.showFocusHistorySheet.collectAsState()
    val showAllowedAppsSheet by viewModel.showAllowedAppsSheet.collectAsState()
    val showAppLimitsSheet by viewModel.showAppLimitsSheet.collectAsState()
    val showBadgesSheet by viewModel.showBadgesSheet.collectAsState()
    val showEndFocusConfirmDialog by viewModel.showEndFocusConfirmDialog.collectAsState()
    val blockedAppDetails by viewModel.blockedAppDetails.collectAsState()

    // Back handling: when not on Center Home page (page 1), pressing Back scrolls back to Home page
    BackHandler(enabled = pagerState.currentPage != 1 || showAddFavoritesScreen) {
        if (showAddFavoritesScreen) {
            viewModel.setShowAddFavoritesScreen(false)
        } else if (pagerState.currentPage != 1) {
            coroutineScope.launch {
                pagerState.animateScrollToPage(1)
            }
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .testTag("launcher_main_scaffold"),
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                when (page) {
                    0 -> {
                        // Left Page: Settings
                        SettingsScreen(
                            isDarkTheme = isDarkTheme,
                            isFocusModeActive = isFocusModeActive,
                            onToggleTheme = { viewModel.setNightMode(!isDarkTheme) },
                            onOpenFocusModeSettings = { viewModel.setShowFocusSettingsDialog(true) },
                            onOpenFocusHistory = { viewModel.setShowFocusHistorySheet(true) },
                            onOpenAppLimits = {
                                viewModel.refreshUsageStats()
                                viewModel.setShowAppLimitsSheet(true)
                            },
                            onOpenBadges = { viewModel.setShowBadgesSheet(true) },
                            onOpenEditFavorites = { viewModel.setShowAddFavoritesScreen(true) },
                            onOpenArrangeFavorites = { viewModel.setShowArrangeSheet(true) },
                            onOpenWidgetsManagement = { viewModel.setShowWidgetsDialog(true) },
                            onSetDefaultLauncher = { viewModel.openDefaultLauncherSettings() },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    1 -> {
                        // Center Page: Home
                        HomeScreen(
                            favoriteApps = favoriteAppInfos,
                            showTime = showTime,
                            showDate = showDate,
                            showNotes = showNotes,
                            showReminders = showReminders,
                            showGoals = showGoals,
                            showTasks = showTasks,
                            isFocusModeActive = isFocusModeActive,
                            notes = notesList,
                            reminders = remindersList,
                            goals = goalsList,
                            tasks = tasks,
                            latestBadge = latestBadge,
                            completedSessions = completedSessions,
                            isDarkTheme = isDarkTheme,
                            onAppClick = { app -> viewModel.tryLaunchApp(app) },
                            onAppLongClick = { app -> viewModel.setShowArrangeSheet(true) },
                            onToggleTask = { id -> viewModel.toggleTask(id) },
                            onAddTask = { text -> viewModel.addTask(text) },
                            onOpenWidgetsManagement = { viewModel.setShowWidgetsDialog(true) },
                            onPromptEndFocus = { viewModel.setShowEndFocusConfirmDialog(true) },
                            onOpenBadgesSheet = { viewModel.setShowBadgesSheet(true) },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    2 -> {
                        // Right Page: App Drawer
                        DrawerScreen(
                            apps = filteredApps,
                            searchQuery = searchQuery,
                            onSearchQueryChange = { query -> viewModel.setSearchQuery(query) },
                            isDarkTheme = isDarkTheme,
                            onAppClick = { app -> viewModel.tryLaunchApp(app) },
                            onAppLongClick = { app -> viewModel.selectAppForActions(app) },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }

            // Fullscreen Edit Favorites Screen
            if (showAddFavoritesScreen) {
                EditFavoritesScreen(
                    allApps = allApps,
                    initialFavorites = favoritePackages,
                    onSaveFavorites = { newFavorites ->
                        viewModel.reorderFavorites(newFavorites)
                        viewModel.setShowAddFavoritesScreen(false)
                    },
                    onBack = { viewModel.setShowAddFavoritesScreen(false) }
                )
            }
        }
    }

    // App Action Bottom Sheet (from Drawer long press)
    selectedAppForActions?.let { app ->
        val isFav = favoritePackages.contains(app.packageName)
        AppActionBottomSheet(
            app = app,
            isFavorite = isFav,
            onToggleFavorite = { viewModel.toggleFavorite(app.packageName) },
            onAppInfo = { viewModel.openAppInfo(app.packageName) },
            onUninstall = { viewModel.uninstallApp(app.packageName) },
            onDismiss = { viewModel.selectAppForActions(null) }
        )
    }

    // Arrange Favorites Bottom Sheet
    if (showArrangeSheet) {
        FavoritesReorderBottomSheet(
            favoriteApps = favoriteAppInfos,
            onSaveOrder = { newOrder ->
                viewModel.reorderFavorites(newOrder)
            },
            onDismiss = { viewModel.setShowArrangeSheet(false) }
        )
    }

    // Widgets Management Bottom Sheet
    if (showWidgetsDialog) {
        WidgetsManagementSheet(
            showTime = showTime,
            showDate = showDate,
            showNotes = showNotes,
            showReminders = showReminders,
            showGoals = showGoals,
            showTasks = showTasks,
            banners = banners,
            tasks = tasks,
            onToggleShowTime = { viewModel.setShowTime(it) },
            onToggleShowDate = { viewModel.setShowDate(it) },
            onToggleShowNotes = { viewModel.setShowNotes(it) },
            onToggleShowReminders = { viewModel.setShowReminders(it) },
            onToggleShowGoals = { viewModel.setShowGoals(it) },
            onToggleShowTasks = { viewModel.setShowTasks(it) },
            onAddBanner = { viewModel.addBanner(it) },
            onRemoveBanner = { viewModel.removeBanner(it) },
            onAddTask = { viewModel.addTask(it) },
            onRemoveTask = { viewModel.removeTask(it) },
            onDismiss = { viewModel.setShowWidgetsDialog(false) }
        )
    }

    // Focus Mode Configuration Bottom Sheet
    if (showFocusSettingsDialog) {
        FocusModeSheet(
            isActive = isFocusModeActive,
            studyMinutes = studyDurationMinutes,
            breakMinutes = breakDurationMinutes,
            longBreakMinutes = longBreakDurationMinutes,
            targetSessions = targetSessions,
            allowedAppsCount = allowedFocusApps.size,
            hideBlockedApps = hideBlockedApps,
            hasOverlayPermission = viewModel.canDrawOverlays(),
            hasNotificationPolicyPermission = viewModel.isNotificationPolicyAccessGranted(),
            onOpenAllowedAppsPicker = { viewModel.setShowAllowedAppsSheet(true) },
            onToggleHideBlockedApps = { viewModel.setHideBlockedApps(it) },
            onRequestOverlayPermission = { viewModel.requestOverlayPermission() },
            onRequestNotificationPolicyPermission = { viewModel.requestNotificationPolicyAccess() },
            onToggleFocusMode = { viewModel.toggleFocusMode() },
            onUpdateStudyMinutes = { viewModel.setStudyDurationMinutes(it) },
            onUpdateBreakMinutes = { viewModel.setBreakDurationMinutes(it) },
            onUpdateLongBreakMinutes = { viewModel.setLongBreakDurationMinutes(it) },
            onUpdateTargetSessions = { viewModel.setTargetSessions(it) },
            onDismiss = { viewModel.setShowFocusSettingsDialog(false) }
        )
    }

    // Focus History Sheet (with custom duration and date/time clear options)
    if (showFocusHistorySheet) {
        FocusHistorySheet(
            historyList = focusHistory,
            totalSessions = completedSessions,
            totalMinutes = totalFocusMinutes,
            onClearAll = { viewModel.clearAllFocusHistory() },
            onClearOlderThan = { cutoff -> viewModel.clearFocusHistoryOlderThan(cutoff) },
            onDismiss = { viewModel.setShowFocusHistorySheet(false) }
        )
    }

    // Allowed Apps in Focus Mode Picker Sheet
    if (showAllowedAppsSheet) {
        AllowedAppsPickerSheet(
            allApps = allApps,
            allowedPackages = allowedFocusApps,
            onToggleAllowed = { viewModel.toggleAllowedFocusApp(it) },
            onDismiss = { viewModel.setShowAllowedAppsSheet(false) }
        )
    }

    // App Limits & Usage Stats Sheet
    if (showAppLimitsSheet) {
        AppLimitsSheet(
            allApps = allApps,
            usageMap = usageMap,
            appLimits = appLimits,
            hasUsagePermission = viewModel.hasUsagePermission(),
            hasOverlayPermission = viewModel.canDrawOverlays(),
            onRequestUsagePermission = { viewModel.requestUsagePermission() },
            onRequestOverlayPermission = { viewModel.requestOverlayPermission() },
            onSetAppLimit = { pkg, limit -> viewModel.setAppLimit(pkg, limit) },
            onDismiss = { viewModel.setShowAppLimitsSheet(false) }
        )
    }

    // Badges Sheet (35+ badges with filters)
    if (showBadgesSheet) {
        BadgesSheet(
            badges = viewModel.getAllBadges(),
            completedSessions = completedSessions,
            totalFocusMinutes = totalFocusMinutes,
            onDismiss = { viewModel.setShowBadgesSheet(false) }
        )
    }

    // Confirmation Dialog to End Focus Mode early (triggered by long-pressing timer)
    if (showEndFocusConfirmDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.setShowEndFocusConfirmDialog(false) },
            title = { Text("End Focus Mode Early?", fontWeight = FontWeight.Bold) },
            text = {
                Text("Are you sure you want to stop Focus Mode? Your phone will be un-silenced and all launcher settings & apps will be restored.")
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.stopFocusMode() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828))
                ) {
                    Text("End Focus Mode")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { viewModel.setShowEndFocusConfirmDialog(false) }) {
                    Text("Keep Focusing")
                }
            }
        )
    }

    // App Blocked / Limit Exceeded Overlap Screen Dialog
    blockedAppDetails?.let { (app, reason) ->
        val limit = appLimits[app.packageName] ?: 0
        val used = usageMap[app.packageName] ?: 0
        AppBlockedDialog(
            app = app,
            reason = reason,
            usedMinutes = used,
            limitMinutes = limit,
            onOpenSettings = {
                if (reason == BlockReason.FOCUS_MODE_RESTRICTED) {
                    viewModel.setShowFocusSettingsDialog(true)
                } else {
                    viewModel.setShowAppLimitsSheet(true)
                }
            },
            onDismiss = { viewModel.dismissBlockedAppDialog() }
        )
    }
}
