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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.example.ui.anim.RainbowBackground
import com.example.ui.components.AppActionBottomSheet
import com.example.ui.components.FavoritesScreen
import com.example.ui.components.ThemesSheet
import com.example.ui.components.UndoBar
import com.example.ui.components.WidgetsManagementSheet
import com.example.ui.screens.AutoAnimationsScreen
import com.example.ui.screens.BackupRestoreScreen
import com.example.ui.screens.CalendarScreen
import com.example.ui.screens.CardsScreen
import com.example.ui.screens.DrawerScreen
import com.example.ui.screens.GoalsScreen
import com.example.ui.screens.HiddenAppsScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.PracticeScreen
import com.example.ui.screens.SearchScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.StudyManagerScreen
import com.example.ui.screens.StudyPopupsScreen
import com.example.ui.screens.SupportScreen
import com.example.ui.screens.TimeTableScreen
import com.example.ui.theme.LauncherTheme
import com.example.ui.theme.isRainbowTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope

@Composable
fun MainScreen(
    viewModel: LauncherViewModel,
    onSetDefaultLauncher: () -> Unit
) {
    val pagerState = rememberPagerState(initialPage = 1, pageCount = { 3 })
    val coroutineScope = rememberCoroutineScope()
    val undoMessage by viewModel.undoMessage.collectAsState()

    // Keep auto night mode and the per-hour / per-day theme rotation in step with the clock.
    LaunchedEffect(Unit) {
        while (true) {
            viewModel.refreshAutoNightMode()
            viewModel.refreshAutoTheme()
            delay(60_000L)
        }
    }

    val isDarkTheme by viewModel.isNightMode.collectAsState()
    val allApps by viewModel.installedApps.collectAsState()
    val favoriteAppInfos by viewModel.favoriteAppInfos.collectAsState()
    val favoritePackages by viewModel.favorites.collectAsState()
    val groups by viewModel.groups.collectAsState()

    val showTime by viewModel.showTime.collectAsState()
    val showDate by viewModel.showDate.collectAsState()
    val showAlerts by viewModel.showAlerts.collectAsState()
    val showTasks by viewModel.showTasks.collectAsState()

    val alerts by viewModel.alerts.collectAsState()
    val tasks by viewModel.tasks.collectAsState()

    // Themes
    val themeKey by viewModel.themeKey.collectAsState()
    val autoThemeMode by viewModel.autoThemeMode.collectAsState()
    val showAutoAnimations by viewModel.showAutoAnimations.collectAsState()
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
    val showGoals by viewModel.showGoals.collectAsState()
    val showSupport by viewModel.showSupport.collectAsState()
    val goals by viewModel.goals.collectAsState()
    val showGoalsWidget by viewModel.showGoalsWidget.collectAsState()
    val showSearch by viewModel.showSearch.collectAsState()
    val searchAutoVoice by viewModel.searchAutoVoice.collectAsState()
    val isLoadingApps by viewModel.isLoadingApps.collectAsState()
    val showHomeSearchBar by viewModel.searchHomeBarEnabled.collectAsState()
    val autoBackupOn by viewModel.autoBackupEnabled.collectAsState()
    val backupFolder by viewModel.backupFolderUri.collectAsState()
    val backupFolderValid by viewModel.backupFolderValid.collectAsState()
    // Settings > Backup & Restore warning (v1.13): shown only when auto-backup is
    // on but its folder cannot actually be used.
    val backupWarning = when {
        !autoBackupOn || backupFolderValid -> null
        backupFolder == null -> "Backup folder not selected"
        else -> "Backup folder unavailable"
    }
    val showHiddenApps by viewModel.showHiddenApps.collectAsState()
    val clockOverride by viewModel.clockOverride.collectAsState()
    val drawerApps by viewModel.drawerApps.collectAsState()
    val showPractice by viewModel.showPractice.collectAsState()
    val showStudyPopups by viewModel.showStudyPopups.collectAsState()
    val showCards by viewModel.showCards.collectAsState()
    val studyProgress by viewModel.studyProgress.collectAsState()
    val autoNight by viewModel.autoNightMode.collectAsState()
    val nightStart by viewModel.nightStartMinutes.collectAsState()
    val nightEnd by viewModel.nightEndMinutes.collectAsState()

    val currentTheme = LauncherTheme.fromKey(themeKey)
    val rainbow = isRainbowTheme(currentTheme)

    // Back handling: when not on Center Home page (page 1), pressing Back scrolls back to Home page
    BackHandler(
        enabled = pagerState.currentPage != 1 || showAddFavoritesScreen || showStudyManager ||
            showBackupRestore || showTimeTable || showStudyPopups || showCards || showCalendar ||
            showGoals || showSupport || showSearch || showHiddenApps || showPractice ||
            showAutoAnimations
    ) {
        when {
            showAutoAnimations -> viewModel.setShowAutoAnimations(false)
            showPractice -> viewModel.setShowPractice(false)
            showHiddenApps -> viewModel.setShowHiddenApps(false)
            showCards -> viewModel.setShowCards(false)
            showCalendar -> viewModel.setShowCalendar(false)
            showGoals -> viewModel.setShowGoals(false)
            showSupport -> viewModel.setShowSupport(false)
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
        containerColor = if (rainbow) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Rainbow theme: a soft rainbow wash behind everything.
            if (rainbow) {
                RainbowBackground(dark = isDarkTheme)
            }

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
                            onOpenAutoAnimations = { viewModel.setShowAutoAnimations(true) },
                            onOpenStudyManager = { viewModel.setShowStudyManager(true) },
                            onOpenStudyPopups = { viewModel.setShowStudyPopups(true) },
                            onOpenGoals = { viewModel.setShowGoals(true) },
                            onOpenWidgetsManagement = { viewModel.setShowWidgetsDialog(true) },
                            onOpenFavorites = { viewModel.setShowAddFavoritesScreen(true) },
                            onOpenBackupRestore = { viewModel.setShowBackupRestore(true) },
                            onOpenSupport = { viewModel.setShowSupport(true) },
                            onSetDefaultLauncher = onSetDefaultLauncher,
                            backupWarning = backupWarning,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    1 -> {
                        // Center Page: Home
                        HomeScreen(
                            favoriteApps = favoriteAppInfos,
                            allApps = allApps,
                            favorites = favoritePackages,
                            groups = groups,
                            showTime = showTime,
                            showDate = showDate,
                            showAlerts = showAlerts,
                            showTasks = showTasks,
                            alerts = alerts,
                            tasks = tasks,
                            showStudyDue = showStudyDue,
                            studyDueItems = studyDueItems,
                            studyProgress = studyProgress,
                            showGoals = showGoalsWidget,
                            goals = goals,
                            isDarkTheme = isDarkTheme,
                            onTickGoal = { id -> viewModel.tickGoal(id) },
                            onOpenGoals = { viewModel.setShowGoals(true) },
                            onAppClick = { app -> viewModel.launchApp(app) },
                            onAppLongClick = { viewModel.setShowAddFavoritesScreen(true) },
                            onEditFavorites = { viewModel.setShowAddFavoritesScreen(true) },
                            onToggleTask = { id -> viewModel.toggleTask(id) },
                            onAddTask = { text -> viewModel.addTask(text) },
                            onOpenWidgetsManagement = { viewModel.setShowWidgetsDialog(true) },
                            onOpenStudyManager = { viewModel.setShowStudyManager(true) },
                            onOpenCalendar = { viewModel.setShowCalendar(true) },
                            // The clock is no longer a door to the vault: the search commands
                            // (`vault <pin>` / `apps <pin>`) are the only way in now.
                            onTimeClick = { },
                            clockOverrideMillis = clockOverride,
                            isLoadingApps = isLoadingApps,
                            showSearchBar = showHomeSearchBar,
                            onOpenSearch = { autoVoice -> viewModel.openSearch(autoVoice) },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    2 -> {
                        // Right Page: App Drawer
                        DrawerScreen(
                            apps = drawerApps,
                            onOpenSearch = { viewModel.openSearch(false) },
                            isDarkTheme = isDarkTheme,
                            onAppClick = { app -> viewModel.launchApp(app) },
                            onAppLongClick = { app -> viewModel.selectAppForActions(app) },
                            isLoadingApps = isLoadingApps,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }

            // Fullscreen Favorites (pick apps + groups, drag to reorder)
            if (showAddFavoritesScreen) {
                FavoritesScreen(
                    allApps = allApps,
                    initialFavorites = favoritePackages,
                    groups = groups,
                    onSave = { newFavorites -> viewModel.reorderFavorites(newFavorites) },
                    onAddGroup = { packages -> viewModel.addGroup(packages) },
                    onUpdateGroup = { group -> viewModel.updateGroup(group) },
                    onDeleteGroup = { id -> viewModel.deleteGroup(id) },
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

            // Fullscreen Goals (regular activity tracker)
            if (showGoals) {
                GoalsScreen(
                    goals = goals,
                    onAdd = { goal -> viewModel.addGoal(goal) },
                    onUpdate = { goal -> viewModel.updateGoal(goal) },
                    onDelete = { id -> viewModel.deleteGoal(id) },
                    onReset = { id -> viewModel.resetGoal(id) },
                    onBack = { viewModel.setShowGoals(false) }
                )
            }

            // Fullscreen Universal Search (also the vault door: `vault <pin>` / `apps <pin>`)
            if (showSearch) {
                SearchScreen(
                    viewModel = viewModel,
                    autoStartVoice = searchAutoVoice,
                    onBack = { viewModel.setShowSearch(false) }
                )
            }

            // The vault, opened by a search command
            if (showHiddenApps) {
                HiddenAppsScreen(
                    viewModel = viewModel,
                    onBack = { viewModel.setShowHiddenApps(false) }
                )
            }

            // Fullscreen flashcard Practice
            if (showPractice) {
                PracticeScreen(
                    viewModel = viewModel,
                    onBack = { viewModel.setShowPractice(false) }
                )
            }

            // Fullscreen Support (tips + links)
            if (showSupport) {
                SupportScreen(onBack = { viewModel.setShowSupport(false) })
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
            showGoals = showGoalsWidget,
            onToggleShowGoals = { viewModel.setShowGoalsWidget(it) },
            onOpenGoals = {
                viewModel.setShowWidgetsDialog(false)
                viewModel.setShowGoals(true)
            },
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

    // Themes Sheet: palettes and night mode only. The schedules live on their own
    // screen (Settings > Automatics).
    if (showThemesSheet) {
        ThemesSheet(
            currentTheme = currentTheme,
            isDarkTheme = isDarkTheme,
            onSelectTheme = { viewModel.setTheme(it.key) },
            onToggleNightMode = { viewModel.setNightMode(it) },
            onOpenAutoAnimations = {
                viewModel.setShowThemesSheet(false)
                viewModel.setShowAutoAnimations(true)
            },
            onDismiss = { viewModel.setShowThemesSheet(false) }
        )
    }

    // Automatics (auto night mode, auto theme change)
    if (showAutoAnimations) {
        AutoAnimationsScreen(
            autoNightEnabled = autoNight,
            nightStartMinutes = nightStart,
            nightEndMinutes = nightEnd,
            autoThemeMode = autoThemeMode,
            onToggleAutoNight = { viewModel.setAutoNightMode(it) },
            onSetNightWindow = { s, e -> viewModel.setNightWindow(s, e) },
            onSetAutoThemeMode = { viewModel.setAutoThemeMode(it) },
            onBack = { viewModel.setShowAutoAnimations(false) }
        )
    }
}
