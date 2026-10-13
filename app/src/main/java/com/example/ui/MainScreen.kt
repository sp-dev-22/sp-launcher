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
import com.example.ui.components.AppBlockedDialog
import com.example.ui.components.AppLimitsSheet
import com.example.ui.components.EditFavoritesScreen
import com.example.ui.components.FavoritesReorderBottomSheet
import com.example.ui.components.ThemesSheet
import com.example.ui.components.WidgetsManagementSheet
import com.example.ui.screens.AboutDevScreen
import com.example.ui.screens.BackupRestoreScreen
import com.example.ui.screens.DrawerScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.StudyManagerScreen
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
    val showNotes by viewModel.showNotes.collectAsState()
    val showTasks by viewModel.showTasks.collectAsState()

    val banners by viewModel.banners.collectAsState()
    val tasks by viewModel.tasks.collectAsState()

    // Usage tracking & Limits
    val usageMap by viewModel.usageMap.collectAsState()
    val appLimits by viewModel.appLimits.collectAsState()

    // Themes & Today-time widgets
    val themeKey by viewModel.themeKey.collectAsState()
    val showScreenTime by viewModel.showScreenTime.collectAsState()
    val totalScreenTimeToday by viewModel.totalScreenTimeToday.collectAsState()
    val showStudyDue by viewModel.showStudyDue.collectAsState()
    val studyDueItems by viewModel.studyDueItems.collectAsState()

    val selectedAppForActions by viewModel.selectedAppForActions.collectAsState()
    val showArrangeSheet by viewModel.showArrangeSheet.collectAsState()
    val showAddFavoritesScreen by viewModel.showAddFavoritesScreen.collectAsState()
    val showWidgetsDialog by viewModel.showWidgetsDialog.collectAsState()
    val showAppLimitsSheet by viewModel.showAppLimitsSheet.collectAsState()
    val showThemesSheet by viewModel.showThemesSheet.collectAsState()
    val blockedAppDetails by viewModel.blockedAppDetails.collectAsState()
    val showStudyManager by viewModel.showStudyManager.collectAsState()
    val showBackupRestore by viewModel.showBackupRestore.collectAsState()
    val showAboutDev by viewModel.showAboutDev.collectAsState()

    // Back handling: when not on Center Home page (page 1), pressing Back scrolls back to Home page
    BackHandler(
        enabled = pagerState.currentPage != 1 || showAddFavoritesScreen || showStudyManager || showBackupRestore || showAboutDev
    ) {
        when {
            showAboutDev -> viewModel.setShowAboutDev(false)
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
                            onOpenAppLimits = {
                                viewModel.refreshUsageStats()
                                viewModel.setShowAppLimitsSheet(true)
                            },
                            onOpenThemes = { viewModel.setShowThemesSheet(true) },
                            onOpenEditFavorites = { viewModel.setShowAddFavoritesScreen(true) },
                            onOpenArrangeFavorites = { viewModel.setShowArrangeSheet(true) },
                            onOpenWidgetsManagement = { viewModel.setShowWidgetsDialog(true) },
                            onOpenStudyManager = { viewModel.setShowStudyManager(true) },
                            onOpenBackupRestore = { viewModel.setShowBackupRestore(true) },
                            onOpenAboutDev = { viewModel.setShowAboutDev(true) },
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
                            showNotes = showNotes,
                            showTasks = showTasks,
                            notes = banners,
                            tasks = tasks,
                            showScreenTime = showScreenTime,
                            totalScreenTimeMinutes = totalScreenTimeToday,
                            showStudyDue = showStudyDue,
                            studyDueItems = studyDueItems,
                            isDarkTheme = isDarkTheme,
                            onAppClick = { app -> viewModel.tryLaunchApp(app) },
                            onAppLongClick = { app -> viewModel.setShowArrangeSheet(true) },
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

            // Fullscreen Study Manager
            if (showStudyManager) {
                StudyManagerScreen(
                    viewModel = viewModel,
                    isDarkTheme = isDarkTheme,
                    onBack = { viewModel.setShowStudyManager(false) }
                )
            }

            // Fullscreen Backup & Restore
            if (showBackupRestore) {
                BackupRestoreScreen(
                    viewModel = viewModel,
                    onBack = { viewModel.setShowBackupRestore(false) }
                )
            }

            // Fullscreen About Dev
            if (showAboutDev) {
                AboutDevScreen(onBack = { viewModel.setShowAboutDev(false) })
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
            showTasks = showTasks,
            showScreenTime = showScreenTime,
            showStudyDue = showStudyDue,
            banners = banners,
            tasks = tasks,
            onToggleShowTime = { viewModel.setShowTime(it) },
            onToggleShowDate = { viewModel.setShowDate(it) },
            onToggleShowNotes = { viewModel.setShowNotes(it) },
            onToggleShowTasks = { viewModel.setShowTasks(it) },
            onToggleShowScreenTime = { viewModel.setShowScreenTime(it) },
            onToggleShowStudyDue = { viewModel.setShowStudyDue(it) },
            onAddBanner = { viewModel.addBanner(it) },
            onRemoveBanner = { viewModel.removeBanner(it) },
            onAddTask = { viewModel.addTask(it) },
            onRemoveTask = { viewModel.removeTask(it) },
            onDismiss = { viewModel.setShowWidgetsDialog(false) }
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

    // App Blocked / Limit Exceeded Dialog
    blockedAppDetails?.let { (app, reason) ->
        val limit = appLimits[app.packageName] ?: 0
        val used = usageMap[app.packageName] ?: 0
        AppBlockedDialog(
            app = app,
            reason = reason,
            usedMinutes = used,
            limitMinutes = limit,
            onOpenSettings = { viewModel.setShowAppLimitsSheet(true) },
            onDismiss = { viewModel.dismissBlockedAppDialog() }
        )
    }
}
