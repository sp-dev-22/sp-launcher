package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.example.ui.components.AppActionBottomSheet
import com.example.ui.components.FavoritesScreen
import com.example.ui.components.ThemesSheet
import com.example.ui.components.UndoBar
import com.example.ui.components.WidgetsManagementSheet
import com.example.ui.screens.BackupRestoreScreen
import com.example.ui.screens.CalendarScreen
import com.example.ui.screens.CardsScreen
import com.example.ui.screens.DrawerScreen
import com.example.ui.screens.HiddenAppsScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.StudyManagerScreen
import com.example.ui.screens.SearchScreen
import com.example.ui.screens.StatsScreen
import com.example.ui.screens.StudyPopupsScreen
import com.example.ui.screens.TimeGateScreen
import com.example.ui.screens.TimeTableScreen
import com.example.ui.theme.LauncherTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun MainScreen(
    viewModel: LauncherViewModel,
    onSetDefaultLauncher: () -> Unit
) {
    val pagerState = rememberPagerState(initialPage = 1, pageCount = { 3 })
    val coroutineScope = rememberCoroutineScope()
    val undoMessage by viewModel.undoMessage.collectAsState()

    // Keep auto night mode in step with the clock.
    LaunchedEffect(Unit) {
        while (true) {
            viewModel.refreshAutoNightMode()
            delay(60_000L)
        }
    }

    val isDarkTheme by viewModel.isNightMode.collectAsState()
    val allApps by viewModel.installedApps.collectAsState()
    val favoriteAppInfos by viewModel.favoriteAppInfos.collectAsState()
    val favoritePackages by viewModel.favorites.collectAsState()

    val showTime by viewModel.showTime.collectAsState()
    val showDate by viewModel.showDate.collectAsState()
    val showAlerts by viewModel.showAlerts.collectAsState()
    val showTasks by viewModel.showTasks.collectAsState()

    val alerts by viewModel.alerts.collectAsState()
    val tasks by viewModel.tasks.collectAsState()

    // Themes
    val themeKey by viewModel.themeKey.collectAsState()
    val showStudyDue by viewModel.showStudyDue.collectAsState()
    val studyDueItems by viewModel.studyDueItems.collectAsState()

    val selectedAppForActions by viewModel.selectedAppForActions.collectAsState()
    val showAddFavoritesScreen by viewModel.showAddFavoritesScreen.collectAsState()
    val showWidgetsDialog by viewModel.showWidgetsDialog.collectAsState()
    val showThemesSheet by viewModel.showThemesSheet.collectAsState()
    val showStudyManager by viewModel.showStudyManager.collectAsState()
    val showBackupRestore by viewModel.showBackupRestore.collectAsState()
    val showTimeTable by viewModel.showTimeTable.collectAsState()
    val showCalendar by viewModel.showCalendar.collectAsState()
    val showStats by viewModel.showStats.collectAsState()
    val showSearch by viewModel.showSearch.collectAsState()
    val showTimeGate by viewModel.showTimeGate.collectAsState()
    val showHiddenApps by viewModel.showHiddenApps.collectAsState()
    val clockOverride by viewModel.clockOverride.collectAsState()
    val drawerApps by viewModel.drawerApps.collectAsState()
    val showStudyPopups by viewModel.showStudyPopups.collectAsState()
    val showCards by viewModel.showCards.collectAsState()
    val studyProgress by viewModel.studyProgress.collectAsState()
    val autoNight by viewModel.autoNightMode.collectAsState()
    val nightStart by viewModel.nightStartMinutes.collectAsState()
    val nightEnd by viewModel.nightEndMinutes.collectAsState()

    // Back handling: when not on Center Home page (page 1), pressing Back scrolls back to Home page
    BackHandler(
        enabled = pagerState.currentPage != 1 || showAddFavoritesScreen || showStudyManager || showBackupRestore || showTimeTable || showStudyPopups || showCards || showCalendar || showStats || showSearch || showTimeGate || showHiddenApps
    ) {
        when {
            showTimeGate -> viewModel.setShowTimeGate(false)
            showHiddenApps -> viewModel.setShowHiddenApps(false)
            showCards -> viewModel.setShowCards(false)
            showCalendar -> viewModel.setShowCalendar(false)
            showStats -> viewModel.setShowStats(false)
            showSearch -> viewModel.setShowSearch(false)
            showStudyPopups -> viewModel.setShowStudyPopups(false)
            showTimeTable -> viewModel.setShowTimeTable(false)
            showStudyManager -> viewModel.setShowStudyManager(false)
            showBackupRestore -> viewModel.setShowBackupRestore(false)
            showAddFavoritesScreen -> viewModel.setShowAddFavoritesScreen(false)
            pagerState.currentPage != 1 -> coroutineScope.launch { pagerState.animateScrollToPage(1) }
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
                            onOpenThemes = { viewModel.setShowThemesSheet(true) },
                            onOpenStudyManager = { viewModel.setShowStudyManager(true) },
                            onOpenStudyPopups = { viewModel.setShowStudyPopups(true) },
                            onOpenStats = { viewModel.setShowStats(true) },
                            onOpenWidgetsManagement = { viewModel.setShowWidgetsDialog(true) },
                            onOpenFavorites = { viewModel.setShowAddFavoritesScreen(true) },
                            onOpenBackupRestore = { viewModel.setShowBackupRestore(true) },
                            onSetDefaultLauncher = onSetDefaultLauncher,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    1 -> {
                        // Center Page: Home
                        HomeScreen(
                            favoriteApps = favoriteAppInfos,
                            showTime = showTime,
                            showDate = showDate,
                            showAlerts = showAlerts,
                            showTasks = showTasks,
                            alerts = alerts,
                            tasks = tasks,
                            showStudyDue = showStudyDue,
                            studyDueItems = studyDueItems,
                            studyProgress = studyProgress,
                            isDarkTheme = isDarkTheme,
                            onAppClick = { app -> viewModel.launchApp(app) },
                            onAppLongClick = { app -> viewModel.setShowAddFavoritesScreen(true) },
                            onToggleTask = { id -> viewModel.toggleTask(id) },
                            onAddTask = { text -> viewModel.addTask(text) },
                            onOpenWidgetsManagement = { viewModel.setShowWidgetsDialog(true) },
                            onOpenStudyManager = { viewModel.setShowStudyManager(true) },
                            onOpenCalendar = { viewModel.setShowCalendar(true) },
                            onTimeClick = { viewModel.setShowTimeGate(true) },
                            clockOverrideMillis = clockOverride,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    2 -> {
                        // Right Page: App Drawer
                        DrawerScreen(
                            apps = drawerApps,
                            onOpenSearch = { viewModel.setShowSearch(true) },
                            isDarkTheme = isDarkTheme,
                            onAppClick = { app -> viewModel.launchApp(app) },
                            onAppLongClick = { app -> viewModel.selectAppForActions(app) },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }

            // Fullscreen Favorites (pick apps + drag to reorder)
            if (showAddFavoritesScreen) {
                FavoritesScreen(
                    allApps = allApps,
                    initialFavorites = favoritePackages,
                    onSave = { newFavorites -> viewModel.reorderFavorites(newFavorites) },
                    onBack = { viewModel.setShowAddFavoritesScreen(false) }
                )
            }

            // Fullscreen Study Manager
            if (showStudyManager) {
                StudyManagerScreen(
                    viewModel = viewModel,
                    isDarkTheme = isDarkTheme,
                    onBack = { viewModel.setShowStudyManager(false) },
                    onOpenTimeTable = { viewModel.setShowTimeTable(true) }
                )
            }

            // Fullscreen Backup & Restore
            if (showBackupRestore) {
                BackupRestoreScreen(
                    viewModel = viewModel,
                    onBack = { viewModel.setShowBackupRestore(false) }
                )
            }

            // Fullscreen Quick Time Table
            if (showTimeTable) {
                TimeTableScreen(
                    viewModel = viewModel,
                    onBack = { viewModel.setShowTimeTable(false) }
                )
            }

            // Fullscreen Study Popups
            if (showStudyPopups) {
                StudyPopupsScreen(
                    viewModel = viewModel,
                    onBack = { viewModel.setShowStudyPopups(false) }
                )
            }

            // Fullscreen Cards (browse / edit / decks)
            if (showCards) {
                CardsScreen(
                    viewModel = viewModel,
                    onBack = { viewModel.setShowCards(false) }
                )
            }

            // Fullscreen in-app Calendar
            if (showCalendar) {
                CalendarScreen(
                    viewModel = viewModel,
                    onBack = { viewModel.setShowCalendar(false) }
                )
            }

            // Fullscreen Study Stats
            if (showStats) {
                StatsScreen(
                    viewModel = viewModel,
                    onBack = { viewModel.setShowStats(false) }
                )
            }

            // Fullscreen Universal Search
            if (showSearch) {
                SearchScreen(
                    viewModel = viewModel,
                    onBack = { viewModel.setShowSearch(false) }
                )
            }

            // Secret "set time" door -> hidden apps (the vault)
            if (showTimeGate) {
                TimeGateScreen(
                    viewModel = viewModel,
                    onBack = { viewModel.setShowTimeGate(false) }
                )
            }
            if (showHiddenApps) {
                HiddenAppsScreen(
                    viewModel = viewModel,
                    onBack = { viewModel.setShowHiddenApps(false) }
                )
            }

            // Delete-undo bar (drawn here; the sheet draws its own copy so it stays reachable)
            UndoBar(
                message = undoMessage,
                onUndo = { viewModel.performUndo() },
                modifier = Modifier.align(Alignment.BottomCenter)
            )
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

    // Widgets Management Bottom Sheet
    if (showWidgetsDialog) {
        WidgetsManagementSheet(
            showTime = showTime,
            showDate = showDate,
            showAlerts = showAlerts,
            showTasks = showTasks,
            showStudyDue = showStudyDue,
            alerts = alerts,
            tasks = tasks,
            onToggleShowTime = { viewModel.setShowTime(it) },
            onToggleShowDate = { viewModel.setShowDate(it) },
            onToggleShowAlerts = { viewModel.setShowAlerts(it) },
            onToggleShowTasks = { viewModel.setShowTasks(it) },
            onToggleShowStudyDue = { viewModel.setShowStudyDue(it) },
            onAddAlert = { viewModel.addAlert(it) },
                            onEditAlert = { viewModel.updateAlert(it) },
                            onRemoveAlert = { viewModel.removeAlert(it) },
                            onMoveAlert = { id, dir -> viewModel.moveAlert(id, dir) },
                            onPinAlert = { id, pinned -> viewModel.setAlertPinned(id, pinned) },
                            onArchiveAlert = { id, archived -> viewModel.setAlertArchived(id, archived) },
                            onAddTask = { viewModel.addTask(it) },
                            onRemoveTask = { viewModel.removeTask(it) },
                            onEditTaskItem = { task -> viewModel.updateTaskItem(task) },
                            onMoveTask = { id, dir -> viewModel.moveTask(id, dir) },
                            undoMessage = undoMessage,
                            onUndo = { viewModel.performUndo() },
            onDismiss = { viewModel.setShowWidgetsDialog(false) }
        )
    }

    // Themes Sheet (colour palettes + night mode)
    if (showThemesSheet) {
        ThemesSheet(
            currentTheme = LauncherTheme.fromKey(themeKey),
            isDarkTheme = isDarkTheme,
            autoNightEnabled = autoNight,
            nightStartMinutes = nightStart,
            nightEndMinutes = nightEnd,
            onSelectTheme = { viewModel.setTheme(it.key) },
            onToggleNightMode = { viewModel.setNightMode(it) },
            onToggleAutoNight = { viewModel.setAutoNightMode(it) },
            onSetNightWindow = { s, e -> viewModel.setNightWindow(s, e) },
            onDismiss = { viewModel.setShowThemesSheet(false) }
        )
    }
}
