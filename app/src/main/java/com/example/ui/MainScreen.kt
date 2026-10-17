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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.example.ui.components.AppActionBottomSheet
import com.example.ui.components.FavoritesScreen
import com.example.ui.components.ThemesSheet
import com.example.ui.components.WidgetsManagementSheet
import com.example.ui.screens.BackupRestoreScreen
import com.example.ui.screens.CardsScreen
import com.example.ui.screens.DrawerScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.StudyManagerScreen
import com.example.ui.screens.StudyPopupsScreen
import com.example.ui.screens.TimeTableScreen
import com.example.ui.theme.LauncherTheme
import kotlinx.coroutines.launch

@Composable
fun MainScreen(
    viewModel: LauncherViewModel,
    onSetDefaultLauncher: () -> Unit
) {
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
    val showStudyPopups by viewModel.showStudyPopups.collectAsState()
    val showCards by viewModel.showCards.collectAsState()
    val studyProgress by viewModel.studyProgress.collectAsState()

    // Back handling: when not on Center Home page (page 1), pressing Back scrolls back to Home page
    BackHandler(
        enabled = pagerState.currentPage != 1 || showAddFavoritesScreen || showStudyManager || showBackupRestore || showTimeTable || showStudyPopups || showCards
    ) {
        when {
            showCards -> viewModel.setShowCards(false)
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
                            onAddTask = { viewModel.addTask(it) },
                            onRemoveTask = { viewModel.removeTask(it) },
                            onEditTask = { id, text -> viewModel.updateTask(id, text) },
                            onMoveTask = { id, dir -> viewModel.moveTask(id, dir) },
            onDismiss = { viewModel.setShowWidgetsDialog(false) }
        )
    }

    // Themes Sheet (colour palettes + night mode)
    if (showThemesSheet) {
        ThemesSheet(
            currentTheme = LauncherTheme.fromKey(themeKey),
            isDarkTheme = isDarkTheme,
            onSelectTheme = { viewModel.setTheme(it.key) },
            onToggleNightMode = { viewModel.setNightMode(it) },
            onDismiss = { viewModel.setShowThemesSheet(false) }
        )
    }
}
