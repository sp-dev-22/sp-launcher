package com.example.ui

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.provider.AlarmClock
import android.provider.CalendarContract
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import com.example.BuildConfig
import com.example.ui.anim.RainbowBackground
import com.example.ui.components.AppActionBottomSheet
import com.example.ui.components.AppLockPinDialog
import com.example.ui.components.FavoritesScreen
import com.example.ui.components.ThemesSheet
import com.example.ui.components.UndoBar
import com.example.ui.components.WhatsNewDialog
import com.example.ui.components.WidgetsManagementSheet
import com.example.ui.screens.AlertsScreen
import com.example.ui.screens.AppLockScreen
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
import com.example.ui.screens.TasksScreen
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
    val showAlertsScreen by viewModel.showAlertsScreen.collectAsState()
    val showTasksScreen by viewModel.showTasksScreen.collectAsState()
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

    // Theme animations (v1.14.0): lighter Diwali effects, OFF by default.
    val diwaliAnimationsEnabled by viewModel.diwaliAnimationsEnabled.collectAsState()
    val powerSaveMode by viewModel.isPowerSaveMode.collectAsState()

    // App Lock (v1.14.0)
    val lockedApps by viewModel.lockedApps.collectAsState()
    val appLockDialog by viewModel.appLockDialog.collectAsState()
    val showAppLock by viewModel.showAppLock.collectAsState()

    // Date widget tap target (v1.15.0): in-app Calendar, or the system Calendar app.
    val dateOpensSystem by viewModel.dateOpensSystem.collectAsState()
    val context = LocalContext.current

    // Now/Next class strip (v1.17.0) + What's New popup (v1.17.0).
    val showNowNextClass by viewModel.showNowNextClass.collectAsState()
    val timeTable by viewModel.timeTable.collectAsState()
    val showWhatsNew by viewModel.showWhatsNew.collectAsState()

    // True while any fullscreen overlay is up (all 16 of them, including
    // Automatics, which is drawn after the Scaffold). Drives the touch shield.
    val anyOverlay = showAddFavoritesScreen || showStudyManager || showBackupRestore ||
        showTimeTable || showStudyPopups || showCards || showCalendar || showGoals ||
        showAlertsScreen || showTasksScreen || showSupport || showSearch ||
        showHiddenApps || showPractice || showAppLock || showAutoAnimations

    val currentTheme = LauncherTheme.fromKey(themeKey)
    val rainbow = isRainbowTheme(currentTheme)

    // An animation shows only on the Home page, only with its own theme active, only
    // when its toggle is on, and never while Battery Saver is on.
    val onHomePage = pagerState.currentPage == 1
    val showDiwaliAnimations = onHomePage && rainbow && diwaliAnimationsEnabled && !powerSaveMode

    // Back handling: when not on Center Home page (page 1), pressing Back scrolls back to Home page
    BackHandler(
        enabled = pagerState.currentPage != 1 || showAddFavoritesScreen || showStudyManager ||
            showBackupRestore || showTimeTable || showStudyPopups || showCards || showCalendar ||
            showGoals || showAlertsScreen || showTasksScreen || showSupport || showSearch ||
            showHiddenApps || showPractice || showAutoAnimations || showAppLock
    ) {
        when {
            showAutoAnimations -> viewModel.setShowAutoAnimations(false)
            showAppLock -> viewModel.setShowAppLock(false)
            showPractice -> viewModel.setShowPractice(false)
            showHiddenApps -> viewModel.setShowHiddenApps(false)
            showCards -> viewModel.setShowCards(false)
            showCalendar -> viewModel.setShowCalendar(false)
            showGoals -> viewModel.setShowGoals(false)
            showAlertsScreen -> viewModel.setShowAlertsScreen(false)
            showTasksScreen -> viewModel.setShowTasksScreen(false)
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
                            onOpenAlerts = { viewModel.setShowAlertsScreen(true) },
                            onOpenTasks = { viewModel.setShowTasksScreen(true) },
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
                            onOpenAlerts = { viewModel.setShowAlertsScreen(true) },
                            onOpenTasks = { viewModel.setShowTasksScreen(true) },
                            onOpenStudyManager = { viewModel.setShowStudyManager(true) },
                            // Now/Next class strip (v1.17.0): today's Time Table
                            // entries; tapping the strip opens the Time Table.
                            showNowNextClass = showNowNextClass,
                            timeTableEntries = timeTable.entries,
                            onOpenTimeTable = { viewModel.setShowTimeTable(true) },
                            // Date tap (v1.15.0): the in-app Calendar, or the system
                            // Calendar app when the user picked that under Widgets.
                            onOpenCalendar = {
                                if (dateOpensSystem) openSystemCalendar(context)
                                else viewModel.setShowCalendar(true)
                            },
                            // Time tap (v1.15.0): the system Clock app. It did nothing
                            // at all before, so this is pure gain.
                            onTimeClick = { openSystemClock(context) },
                            clockOverrideMillis = clockOverride,
                            isLoadingApps = isLoadingApps,
                            showSearchBar = showHomeSearchBar,
                            onOpenSearch = { autoVoice -> viewModel.openSearch(autoVoice) },
                            showDiwaliAnimations = showDiwaliAnimations,
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
                            lockedApps = lockedApps,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }

            // Overlay touch shield (v1.17.0, bug 2): every fullscreen screen
            // above is drawn over the pager, and a tap on an empty part of one
            // used to fall through to the page below — a long-press on the
            // Hidden Apps title gap opened the drawer app's menu underneath.
            // This shield sits between the pager and the overlays and swallows
            // those taps with a no-op clickable (no ripple). Taps on real
            // controls and scrolls inside the screens still work: inner
            // clickables and scrollables consume their gestures first, and a
            // clickable only claims the press, never a drag. It only exists
            // while an overlay is up, so Home/Drawer taps are untouched. The
            // dialogs and bottom sheets need no shield — their Popup windows
            // already consume outside touches with their scrims.
            if (anyOverlay) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { /* swallow: nothing beneath an overlay may be tapped */ }
                        .testTag("overlay_touch_shield")
                )
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

            // Fullscreen Alerts (reminders with optional notifications)
            if (showAlertsScreen) {
                AlertsScreen(
                    alerts = alerts,
                    onAddAlert = { viewModel.addAlert(it) },
                    onEditAlert = { viewModel.updateAlert(it) },
                    onRemoveAlert = { viewModel.removeAlert(it) },
                    onMoveAlert = { id, dir -> viewModel.moveAlert(id, dir) },
                    onPinAlert = { id, pinned -> viewModel.setAlertPinned(id, pinned) },
                    onArchiveAlert = { id, archived -> viewModel.setAlertArchived(id, archived) },
                    onBack = { viewModel.setShowAlertsScreen(false) }
                )
            }

            // Fullscreen Tasks (things to do, with due dates and reminders)
            if (showTasksScreen) {
                TasksScreen(
                    tasks = tasks,
                    onAddTask = { viewModel.addTask(it) },
                    onRemoveTask = { viewModel.removeTask(it) },
                    onEditTaskItem = { task -> viewModel.updateTaskItem(task) },
                    onMoveTask = { id, dir -> viewModel.moveTask(id, dir) },
                    onToggleTask = { id -> viewModel.toggleTask(id) },
                    onBack = { viewModel.setShowTasksScreen(false) }
                )
            }

            // Fullscreen Universal Search (its menu is the door to the vault and to App Lock)
            if (showSearch) {
                SearchScreen(
                    viewModel = viewModel,
                    autoStartVoice = searchAutoVoice,
                    onBack = { viewModel.setShowSearch(false) }
                )
            }

            // The vault, opened from the search menu (Hidden Apps, PIN first)
            if (showHiddenApps) {
                HiddenAppsScreen(
                    viewModel = viewModel,
                    onBack = { viewModel.setShowHiddenApps(false) }
                )
            }

            // App Lock management (locked apps + change PIN), from the search menu
            if (showAppLock) {
                AppLockScreen(
                    viewModel = viewModel,
                    onBack = { viewModel.setShowAppLock(false) }
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
        val isLocked = lockedApps.contains(app.packageName)
        AppActionBottomSheet(
            app = app,
            isFavorite = isFav,
            onToggleFavorite = { viewModel.toggleFavorite(app.packageName) },
            onAppInfo = { viewModel.openAppInfo(app.packageName) },
            onUninstall = { viewModel.uninstallApp(app.packageName) },
            onDismiss = { viewModel.selectAppForActions(null) },
            isLocked = isLocked,
            onToggleLock = {
                if (isLocked) viewModel.requestUnlockApp(app.packageName)
                else viewModel.requestLockApp(app.packageName)
            }
        )
    }

    // App Lock PIN dialog (set / verify to open / verify to unlock / change PIN)
    appLockDialog?.let { dialog ->
        val appLabel = when (dialog) {
            is AppLockDialog.SetupPin ->
                allApps.firstOrNull { it.packageName == dialog.packageName }?.label
                    ?: dialog.packageName
            is AppLockDialog.VerifyToLaunch -> dialog.app.label
            is AppLockDialog.VerifyToUnlock ->
                allApps.firstOrNull { it.packageName == dialog.packageName }?.label
                    ?: dialog.packageName
            AppLockDialog.ChangePin -> ""
        }
        val setupPackage = (dialog as? AppLockDialog.SetupPin)?.packageName
        AppLockPinDialog(
            dialog = dialog,
            appLabel = appLabel,
            onDismiss = { viewModel.dismissAppLockDialog() },
            onSetupPin = { pin ->
                setupPackage?.let { viewModel.saveAppLockPinAndLock(pin, it) }
            },
            onVerifyPin = { pin -> viewModel.verifyAppLockPin(pin) },
            onSubmitVerify = { pin -> viewModel.submitAppLockPin(pin) },
            onSubmitBiometric = { viewModel.submitAppLockBiometric() },
            onChangePin = { pin -> viewModel.changeAppLockPin(pin) }
        )
    }

    // Widgets Management Bottom Sheet (widget settings only, since v1.16.0 —
    // Alerts and Tasks live on their own screens now)
    if (showWidgetsDialog) {
        WidgetsManagementSheet(
            showTime = showTime,
            showDate = showDate,
            showAlerts = showAlerts,
            showTasks = showTasks,
            showStudyDue = showStudyDue,
            onToggleShowTime = { viewModel.setShowTime(it) },
            onToggleShowDate = { viewModel.setShowDate(it) },
            onToggleShowAlerts = { viewModel.setShowAlerts(it) },
            onToggleShowTasks = { viewModel.setShowTasks(it) },
            onToggleShowStudyDue = { viewModel.setShowStudyDue(it) },
            showGoals = showGoalsWidget,
            onToggleShowGoals = { viewModel.setShowGoalsWidget(it) },
            showNowNextClass = showNowNextClass,
            onToggleShowNowNextClass = { viewModel.setShowNowNextClass(it) },
            onOpenGoals = {
                viewModel.setShowWidgetsDialog(false)
                viewModel.setShowGoals(true)
            },
            onDismiss = { viewModel.setShowWidgetsDialog(false) },
            dateOpensSystem = dateOpensSystem,
            onSetDateOpensSystem = { viewModel.setDateOpensSystem(it) }
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

    // What's New (v1.17.0): once after an update. Dismissing by any means
    // records the version, so it never shows again for this version.
    if (showWhatsNew) {
        WhatsNewDialog(
            versionName = BuildConfig.VERSION_NAME,
            onDismiss = { viewModel.dismissWhatsNew() }
        )
    }

    // Automatics (auto night mode, auto theme change, theme animations)
    if (showAutoAnimations) {
        AutoAnimationsScreen(
            autoNightEnabled = autoNight,
            nightStartMinutes = nightStart,
            nightEndMinutes = nightEnd,
            autoThemeMode = autoThemeMode,
            onToggleAutoNight = { viewModel.setAutoNightMode(it) },
            onSetNightWindow = { s, e -> viewModel.setNightWindow(s, e) },
            onSetAutoThemeMode = { viewModel.setAutoThemeMode(it) },
            diwaliAnimationsEnabled = diwaliAnimationsEnabled,
            onToggleDiwaliAnimations = { viewModel.setDiwaliAnimationsEnabled(it) },
            onBack = { viewModel.setShowAutoAnimations(false) }
        )
    }
}

/** Opens the system Clock app (alarms tab). No clock app -> a toast, never a crash. */
private fun openSystemClock(context: Context) {
    val intent = Intent(AlarmClock.ACTION_SHOW_ALARMS)
    try {
        context.startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        Toast.makeText(context, "No clock app found", Toast.LENGTH_SHORT).show()
    }
}

/**
 * Opens the system Calendar app at today. No calendar app -> a toast, never a crash.
 * Needs no permission: viewing the calendar is a plain ACTION_VIEW.
 */
private fun openSystemCalendar(context: Context) {
    val timeUri = CalendarContract.CONTENT_URI.buildUpon()
        .appendPath("time")
        .appendPath(System.currentTimeMillis().toString())
        .build()
    val intent = Intent(Intent.ACTION_VIEW, timeUri)
    try {
        context.startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        Toast.makeText(context, "No calendar app found", Toast.LENGTH_SHORT).show()
    }
}
