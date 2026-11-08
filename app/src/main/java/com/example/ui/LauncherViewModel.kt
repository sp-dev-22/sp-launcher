package com.example.ui

import android.app.Application
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.OpenableColumns
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.AlertItem
import com.example.data.model.AppGroup
import com.example.data.model.AppInfo
import com.example.data.model.ContactEntry
import com.example.data.model.DeviceFile
import com.example.data.model.FavoriteSlots
import com.example.data.model.CardStats
import com.example.data.model.FlashcardItem
import com.example.data.model.StudyProgress
import com.example.data.model.StudyCourse
import com.example.data.model.StudyDueItem
import com.example.data.model.TaskItem
import com.example.data.model.GoalItem
import com.example.data.model.allClassRefs
import com.example.data.repository.AppRepository
import com.example.data.repository.AssistantRepository
import com.example.data.repository.LauncherPreferences
import com.example.data.model.SubjectPlanSettings
import com.example.data.model.TimeTableSettings
import com.example.data.model.StudyPopupSettings
import com.example.service.AlertReminderScheduler
import com.example.service.AutoBackupScheduler
import com.example.service.TaskReminderScheduler
import com.example.service.StudyPopupScheduler
import com.example.service.StudyReminderScheduler
import com.example.util.AutoBackup
import com.example.util.CardFileParser
import com.example.util.GoalProgress
import com.example.util.TimeTablePlanner
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar

class LauncherViewModel(application: Application) : AndroidViewModel(application) {

    private val appRepository = AppRepository(application)
    private val preferences = LauncherPreferences(application)

    private val _installedApps = MutableStateFlow<List<AppInfo>>(emptyList())
    val installedApps: StateFlow<List<AppInfo>> = _installedApps.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val isNightMode = preferences.isNightMode
    val favorites = preferences.favorites
    val groups = preferences.groups
    val autoThemeMode = preferences.autoThemeMode

    // SP Assistant (v1.12): search source toggles + phone data
    val searchContactsEnabled = preferences.searchContacts
    val searchFilesEnabled = preferences.searchFiles
    val searchCalcEnabled = preferences.searchCalc
    val searchWebEnabled = preferences.searchWeb
    val searchVoiceEnabled = preferences.searchVoice

    fun setSearchContactsEnabled(enabled: Boolean) = preferences.setSearchContacts(enabled)
    fun setSearchFilesEnabled(enabled: Boolean) = preferences.setSearchFiles(enabled)
    fun setSearchCalcEnabled(enabled: Boolean) = preferences.setSearchCalc(enabled)
    fun setSearchWebEnabled(enabled: Boolean) = preferences.setSearchWeb(enabled)
    fun setSearchVoiceEnabled(enabled: Boolean) = preferences.setSearchVoice(enabled)

    private val _contacts = MutableStateFlow<List<ContactEntry>>(emptyList())
    val contacts: StateFlow<List<ContactEntry>> = _contacts.asStateFlow()

    private val _fileHits = MutableStateFlow<List<DeviceFile>>(emptyList())
    val fileHits: StateFlow<List<DeviceFile>> = _fileHits.asStateFlow()

    private var fileSearchJob: Job? = null

    fun hasContactsPermission(): Boolean = ContextCompat.checkSelfPermission(
        getApplication(), android.Manifest.permission.READ_CONTACTS
    ) == PackageManager.PERMISSION_GRANTED

    fun hasFilesPermission(): Boolean {
        val app = getApplication<Application>()
        return if (android.os.Build.VERSION.SDK_INT >= 33) {
            listOf(
                android.Manifest.permission.READ_MEDIA_IMAGES,
                android.Manifest.permission.READ_MEDIA_VIDEO,
                android.Manifest.permission.READ_MEDIA_AUDIO
            ).any {
                ContextCompat.checkSelfPermission(app, it) == PackageManager.PERMISSION_GRANTED
            }
        } else {
            @Suppress("DEPRECATION")
            ContextCompat.checkSelfPermission(
                app, android.Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    /** Loads the address book in the background. Does nothing without the permission. */
    fun refreshContacts() {
        if (!hasContactsPermission()) return
        viewModelScope.launch {
            _contacts.value = withContext(Dispatchers.IO) {
                AssistantRepository.loadContacts(getApplication())
            }
        }
    }

    /** Debounced media-store search; results land in [fileHits]. */
    fun searchFiles(query: String) {
        fileSearchJob?.cancel()
        if (query.trim().length < 2 || !hasFilesPermission()) {
            _fileHits.value = emptyList()
            return
        }
        fileSearchJob = viewModelScope.launch {
            delay(250)
            _fileHits.value = withContext(Dispatchers.IO) {
                AssistantRepository.searchFiles(getApplication(), query)
            }
        }
    }
    val showTime = preferences.showTime
    val showDate = preferences.showDate
    val showAlerts = preferences.showAlerts
    val showTasks = preferences.showTasks
    val showStudyDue = preferences.showStudyDue
    val themeKey = preferences.themeKey
    val alerts = preferences.alerts
    val tasks = preferences.tasks
    val autoNightMode = preferences.autoNightMode
    val nightStartMinutes = preferences.nightStartMinutes
    val nightEndMinutes = preferences.nightEndMinutes
    val timeTable = preferences.timeTable
    val flashcardBank = preferences.flashcardBank
    val popupSettings = preferences.popupSettings
    val hiddenApps = preferences.hiddenApps
    val vaultPin = preferences.vaultPin
    val autoBackupEnabled = preferences.autoBackupEnabled
    val backupFolderUri = preferences.backupFolderUri
    val lastBackupMillis = preferences.lastBackupMillis

    // Goals (regular activity tracker) + the calendar's cleared-event set
    val goals = preferences.goals
    val showGoalsWidget = preferences.showGoalsWidget
    val clearedCalendarEvents = preferences.clearedCalendarEvents

    // Study Manager tree
    val studyCourses: StateFlow<List<StudyCourse>> = preferences.studyCourses

    // Study classes overdue, due today, or due within the next 7 days (Home "Due" card)
    val studyDueItems: StateFlow<List<StudyDueItem>> = studyCourses
        .map { courses -> computeStudyDue(courses) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private fun computeStudyDue(courses: List<StudyCourse>): List<StudyDueItem> {
        val now = System.currentTimeMillis()
        val endOfWindow = endOfWindowMillis()
        val items = mutableListOf<StudyDueItem>()
        for (course in courses) {
            for (ref in course.allClassRefs()) {
                if (ref.clazz.isCompleted) continue
                val deadline = ref.clazz.deadlineMillis ?: continue
                val overdue = deadline < now
                val withinWindow = deadline in now..endOfWindow
                if (overdue || withinWindow) {
                    items.add(StudyDueItem("${ref.topic.name} · ${ref.clazz.name}", deadline, overdue))
                }
            }
        }
        return items.sortedBy { it.deadlineMillis }
    }

    /** End of the day six days from now, i.e. the last moment of the "next 7 days" window. */
    private fun endOfWindowMillis(): Long = Calendar.getInstance().apply {
        add(Calendar.DAY_OF_YEAR, 6)
        set(Calendar.HOUR_OF_DAY, 23)
        set(Calendar.MINUTE, 59)
        set(Calendar.SECOND, 59)
        set(Calendar.MILLISECOND, 999)
    }.timeInMillis

    // Dialog & Sheet States
    private val _selectedAppForActions = MutableStateFlow<AppInfo?>(null)
    val selectedAppForActions: StateFlow<AppInfo?> = _selectedAppForActions.asStateFlow()

    private val _showAddFavoritesScreen = MutableStateFlow(false)
    val showAddFavoritesScreen: StateFlow<Boolean> = _showAddFavoritesScreen.asStateFlow()

    private val _showWidgetsDialog = MutableStateFlow(false)
    val showWidgetsDialog: StateFlow<Boolean> = _showWidgetsDialog.asStateFlow()

    private val _showStudyManager = MutableStateFlow(false)
    val showStudyManager: StateFlow<Boolean> = _showStudyManager.asStateFlow()

    private val _showBackupRestore = MutableStateFlow(false)
    val showBackupRestore: StateFlow<Boolean> = _showBackupRestore.asStateFlow()

    private val _showThemesSheet = MutableStateFlow(false)
    val showThemesSheet: StateFlow<Boolean> = _showThemesSheet.asStateFlow()

    private val _showTimeTable = MutableStateFlow(false)
    val showTimeTable: StateFlow<Boolean> = _showTimeTable.asStateFlow()

    private val _showCalendar = MutableStateFlow(false)
    val showCalendar: StateFlow<Boolean> = _showCalendar.asStateFlow()

    private val _showGoals = MutableStateFlow(false)
    val showGoals: StateFlow<Boolean> = _showGoals.asStateFlow()

    private val _showSupport = MutableStateFlow(false)
    val showSupport: StateFlow<Boolean> = _showSupport.asStateFlow()

    private val _showSearch = MutableStateFlow(false)
    val showSearch: StateFlow<Boolean> = _showSearch.asStateFlow()

    private val _showTimeGate = MutableStateFlow(false)
    val showTimeGate: StateFlow<Boolean> = _showTimeGate.asStateFlow()

    private val _showAutoAnimations = MutableStateFlow(false)
    val showAutoAnimations: StateFlow<Boolean> = _showAutoAnimations.asStateFlow()

    fun setShowAutoAnimations(show: Boolean) {
        _showAutoAnimations.value = show
    }

    private val _showHiddenApps = MutableStateFlow(false)
    val showHiddenApps: StateFlow<Boolean> = _showHiddenApps.asStateFlow()

    private val _hiddenAppsMode = MutableStateFlow(HiddenAppsMode.MANAGE)
    val hiddenAppsMode: StateFlow<HiddenAppsMode> = _hiddenAppsMode.asStateFlow()

    private val _showPractice = MutableStateFlow(false)
    val showPractice: StateFlow<Boolean> = _showPractice.asStateFlow()

    private val _backupMessage = MutableStateFlow<String?>(null)
    val backupMessage: StateFlow<String?> = _backupMessage.asStateFlow()

    // When set, the Home clock shows this time instead of the device time (the decoy).
    private val _clockOverride = MutableStateFlow<Long?>(null)
    val clockOverride: StateFlow<Long?> = _clockOverride.asStateFlow()
    private var clockOverrideJob: Job? = null

    /** A message to show when a time table could not be generated (e.g. not enough time). */
    private val _timeTableAlert = MutableStateFlow<String?>(null)
    val timeTableAlert: StateFlow<String?> = _timeTableAlert.asStateFlow()

    private val _showStudyPopups = MutableStateFlow(false)
    val showStudyPopups: StateFlow<Boolean> = _showStudyPopups.asStateFlow()

    private val _cardImportCount = MutableStateFlow<Int?>(null)
    val cardImportCount: StateFlow<Int?> = _cardImportCount.asStateFlow()

    private val _showCards = MutableStateFlow(false)
    val showCards: StateFlow<Boolean> = _showCards.asStateFlow()

    val cardStats: StateFlow<CardStats> = flashcardBank
        .map { cards ->
            val today = startOfToday()
            CardStats(
                total = cards.size,
                mastered = cards.count { it.locked },
                due = cards.count { !it.locked && (it.nextDueMillis == null || it.nextDueMillis <= System.currentTimeMillis()) },
                masteredToday = cards.count { it.masteredAtMillis != null && it.masteredAtMillis >= today }
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), CardStats())

    val studyProgress: StateFlow<StudyProgress> = studyCourses
        .map { courses ->
            var total = 0
            var done = 0
            for (course in courses) for (subject in course.subjects) for (topic in subject.topics) for (clazz in topic.classes) {
                total++
                if (clazz.isCompleted) done++
            }
            StudyProgress(completed = done, total = total)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), StudyProgress())

    // Filtered apps for the App Drawer (search only)
    val filteredApps: StateFlow<List<AppInfo>> =
        combine(_installedApps, _searchQuery) { apps, query ->
            if (query.isBlank()) apps
            else apps.filter { it.label.contains(query, ignoreCase = true) }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // What the drawer and search actually show (hidden apps removed).
    val drawerApps: StateFlow<List<AppInfo>> =
        combine(_installedApps, preferences.hiddenApps) { apps, hidden ->
            if (hidden.isEmpty()) apps else apps.filter { it.packageName !in hidden }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Favorite apps for the Home Screen
    val favoriteAppInfos: StateFlow<List<AppInfo>> =
        combine(_installedApps, favorites) { apps, favPkgs ->
            val appMap = apps.associateBy { it.packageName }
            favPkgs.mapNotNull { appMap[it] }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        loadApps()
        // Re-arm any pending Alerts and Study Manager reminders on launch.
        rescheduleAlertReminders()
        rescheduleStudyReminders()
        rescheduleTaskReminders()
        StudyPopupScheduler.reschedule(application, preferences.popupSettings.value)
        refreshAutoNightMode()
    }

    private var lastAppsLoadMillis = 0L

    /**
     * Reloads the app list, but at most once every few seconds. onResume fires on every return
     * to the launcher (Home press, unlock, closing an app), and a full package-manager scan each
     * time is wasteful and can make the launcher feel sluggish.
     */
    fun refreshApps() {
        val now = System.currentTimeMillis()
        if (_installedApps.value.isNotEmpty() && now - lastAppsLoadMillis < 5_000L) return
        loadApps()
    }

    fun loadApps() {
        lastAppsLoadMillis = System.currentTimeMillis()
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

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setNightMode(enabled: Boolean) {
        preferences.setNightMode(enabled)
    }

    fun setAutoNightMode(enabled: Boolean) {
        preferences.setAutoNightMode(enabled)
        refreshAutoNightMode()
    }

    fun setNightWindow(startMinutes: Int, endMinutes: Int) {
        preferences.setNightWindow(startMinutes, endMinutes)
        refreshAutoNightMode()
    }

    /** Applies the auto night schedule. Does nothing when auto night is off. */
    fun refreshAutoNightMode() {
        if (!preferences.autoNightMode.value) return
        setNightMode(isNightNow())
    }

    private fun isNightNow(): Boolean {
        val now = java.util.Calendar.getInstance()
        val minutes = now.get(java.util.Calendar.HOUR_OF_DAY) * 60 + now.get(java.util.Calendar.MINUTE)
        val start = preferences.nightStartMinutes.value
        val end = preferences.nightEndMinutes.value
        return if (start <= end) minutes in start until end else (minutes >= start || minutes < end)
    }

    fun setShowTime(enabled: Boolean) {
        preferences.setShowTime(enabled)
    }

    fun setShowDate(enabled: Boolean) {
        preferences.setShowDate(enabled)
    }

    fun setShowAlerts(enabled: Boolean) {
        preferences.setShowAlerts(enabled)
    }

    fun setShowTasks(enabled: Boolean) {
        preferences.setShowTasks(enabled)
    }

    fun setShowStudyDue(enabled: Boolean) {
        preferences.setShowStudyDue(enabled)
    }

    fun setTheme(key: String) {
        preferences.setTheme(key)
    }

    /** True when the favourites bar still has a free slot. */
    fun canAddFavorite(): Boolean = preferences.canAddFavorite()

    /** Returns false when the bar is already full (five apps and groups together). */
    fun addFavorite(packageName: String): Boolean = preferences.addFavorite(packageName)

    fun removeFavorite(packageName: String) {
        preferences.removeFavorite(packageName)
    }

    fun toggleFavorite(packageName: String): Boolean {
        val added = preferences.toggleFavorite(packageName)
        if (!added) {
            offerUndo("Favourites bar is full (${FavoriteSlots.MAX_SLOTS} slots)") { }
        }
        return added
    }

    fun reorderFavorites(newOrder: List<String>) {
        preferences.saveFavorites(newOrder)
    }

    // ---- App groups (favourites bar only) ----

    /** Returns false when the bar has no room for the new group. */
    /**
     * Creates a group and returns it (null only when no apps were picked). Placing the
     * group in the bar is the Favourites screen's job — it owns the bar's working copy.
     */
    fun addGroup(packages: List<String>): AppGroup? = preferences.addGroup(packages)

    fun updateGroup(group: AppGroup) = preferences.updateGroup(group)

    fun deleteGroup(groupId: String) = preferences.deleteGroup(groupId)

    fun setGroupPackages(groupId: String, packages: List<String>) =
        preferences.setGroupPackages(groupId, packages)

    // ---- Theme rotation ----

    fun setAutoThemeMode(mode: String) {
        preferences.setAutoThemeMode(mode)
        // Switching rotation on steps to the next palette straight away, so the change is
        // visible at once — the old clock-based pick landed on the same theme every time
        // it was switched on at the same time of day.
        if (mode != "off") advanceAutoTheme()
    }

    /**
     * Steps the palette forward once an hour or once a day, depending on the chosen mode.
     * Does nothing when rotation is off, and never rotates into (or out of) the Rainbow
     * theme on its own.
     *
     * The step is measured from the last actual change, not read off the clock: the
     * pre-v1.11.5 version picked palette[hour % size] / palette[day-of-year % size], so
     * switching the feature on at the same time of day always set the same theme.
     */
    fun refreshAutoTheme() {
        val mode = preferences.autoThemeMode.value
        if (mode == "off") return
        val intervalMillis = if (mode == "hour") 3_600_000L else 86_400_000L
        val last = preferences.lastAutoThemeMillis()
        if (last == 0L) {
            preferences.setLastAutoThemeMillis(System.currentTimeMillis())
            return
        }
        if (System.currentTimeMillis() - last >= intervalMillis) advanceAutoTheme()
    }

    /** Moves to the palette entry after the current theme, and stamps the change. */
    private fun advanceAutoTheme() {
        val palette = ROTATING_THEMES
        val index = palette.indexOf(preferences.themeKey.value)
        // A theme outside the rotation (Rainbow, chosen by hand) restarts from the top.
        val next = palette[(index + 1) % palette.size]
        preferences.setTheme(next)
        preferences.setLastAutoThemeMillis(System.currentTimeMillis())
    }

    fun addTask(text: String) {
        preferences.addTask(text)
    }

    fun toggleTask(id: String) {
        preferences.toggleTask(id)
        rescheduleTaskReminders()
    }

    fun removeTask(id: String) {
        val index = preferences.tasks.value.indexOfFirst { it.id == id }
        val removed = if (index >= 0) preferences.tasks.value[index] else null
        TaskReminderScheduler.cancel(getApplication<Application>(), id)
        preferences.removeTask(id)
        rescheduleTaskReminders()
        if (removed != null) offerUndo("Task deleted") { preferences.restoreTask(removed, index) }
    }

    fun updateTask(id: String, text: String) {
        preferences.updateTask(id, text)
    }

    fun updateTaskItem(task: TaskItem) {
        preferences.updateTaskItem(task)
        rescheduleTaskReminders()
    }

    fun moveTask(id: String, direction: Int) {
        preferences.moveTask(id, direction)
    }

    // ---- Alerts (with optional reminders) ----

    fun addAlert(alert: AlertItem) {
        preferences.addAlert(alert)
        rescheduleAlertReminders()
    }

    fun updateAlert(alert: AlertItem) {
        preferences.updateAlert(alert)
        rescheduleAlertReminders()
    }

    fun removeAlert(id: String) {
        val index = preferences.alerts.value.indexOfFirst { it.id == id }
        val removed = if (index >= 0) preferences.alerts.value[index] else null
        AlertReminderScheduler.cancel(getApplication(), id)
        preferences.removeAlert(id)
        rescheduleAlertReminders()
        if (removed != null) offerUndo("Note deleted") { preferences.restoreAlert(removed, index) }
    }

    fun moveAlert(id: String, direction: Int) {
        preferences.moveAlert(id, direction)
    }

    fun setAlertPinned(id: String, pinned: Boolean) {
        preferences.setAlertPinned(id, pinned)
    }

    fun setAlertArchived(id: String, archived: Boolean) {
        preferences.setAlertArchived(id, archived)
    }

    fun selectAppForActions(app: AppInfo?) {
        _selectedAppForActions.value = app
    }

    fun setShowAddFavoritesScreen(show: Boolean) {
        _showAddFavoritesScreen.value = show
    }

    fun setShowWidgetsDialog(show: Boolean) {
        _showWidgetsDialog.value = show
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

    fun setShowTimeTable(show: Boolean) {
        _showTimeTable.value = show
    }

    fun setShowCalendar(show: Boolean) {
        _showCalendar.value = show
    }

    fun setShowGoals(show: Boolean) {
        _showGoals.value = show
    }

    fun setShowSupport(show: Boolean) {
        _showSupport.value = show
    }

    fun setShowSearch(show: Boolean) {
        _showSearch.value = show
    }

    fun setShowTimeGate(show: Boolean) {
        _showTimeGate.value = show
    }

    fun setShowHiddenApps(show: Boolean) {
        _showHiddenApps.value = show
        if (!show) _hiddenAppsMode.value = HiddenAppsMode.MANAGE
    }

    fun setShowPractice(show: Boolean) {
        _showPractice.value = show
    }

    // ---------------------------------------------------------------------
    // Goals (regular activity tracker)
    // ---------------------------------------------------------------------

    fun setShowGoalsWidget(enabled: Boolean) = preferences.setShowGoalsWidget(enabled)

    fun addGoal(goal: GoalItem) = preferences.addGoal(goal)

    fun updateGoal(goal: GoalItem) = preferences.updateGoal(goal)

    fun deleteGoal(id: String) {
        val index = preferences.goals.value.indexOfFirst { it.id == id }
        val removed = if (index >= 0) preferences.goals.value[index] else null
        preferences.deleteGoal(id)
        if (removed != null) offerUndo("Goal deleted") { preferences.restoreGoal(removed, index) }
    }

    /** One tap on a goal, from the Home widget or anywhere else. */
    fun tickGoal(id: String) {
        val goal = preferences.goals.value.firstOrNull { it.id == id } ?: return
        val updated = GoalProgress.tick(goal)
        if (updated == goal) return
        preferences.updateGoal(updated)
        offerUndo("${goal.title}: ${GoalProgress.formatValue(updated.currentValue)}") {
            preferences.updateGoal(goal)
        }
    }

    /** Zeroes a goal's current progress but keeps its lifetime total. */
    fun resetGoal(id: String) {
        val goal = preferences.goals.value.firstOrNull { it.id == id } ?: return
        if (goal.currentValue == 0.0) return
        preferences.updateGoal(goal.copy(currentValue = 0.0))
        offerUndo("${goal.title} reset") { preferences.updateGoal(goal) }
    }

    /** Takes the last tap back (the undo bar's job, but also usable directly). */
    fun undoGoalTick(id: String) {
        val goal = preferences.goals.value.firstOrNull { it.id == id } ?: return
        preferences.updateGoal(GoalProgress.undo(goal))
    }

    // ---------------------------------------------------------------------
    // Calendar: open a screen from an event, and clear an event
    // ---------------------------------------------------------------------

    /** The calendar overlay closes first, so the target screen is not drawn underneath it. */
    fun openStudyManagerFromCalendar() {
        _showCalendar.value = false
        _showStudyManager.value = true
    }

    fun openStudyPopupsFromCalendar() {
        _showCalendar.value = false
        _showStudyPopups.value = true
    }

    /** The checklist lives in the Widgets sheet's task section. */
    fun openChecklistFromCalendar() {
        _showCalendar.value = false
        _showWidgetsDialog.value = true
    }

    /** Hides one event from the calendar. The item itself is untouched. */
    fun clearCalendarEvent(key: String) {
        preferences.clearCalendarEvent(key)
        offerUndo("Removed from calendar") { preferences.unclearCalendarEvent(key) }
    }

    // ---- flashcards: manual practice ----

    /** Grades a card from the Practice screen, using the same logic as the popups. */
    fun markCard(id: String, correct: Boolean) {
        val card = preferences.flashcardBank.value.firstOrNull { it.id == id } ?: return
        preferences.updateCard(com.example.util.SpacedRepetition.apply(card, correct))
    }

    // ---- auto-backup ----

    fun setAutoBackupEnabled(enabled: Boolean) {
        preferences.setAutoBackupEnabled(enabled)
        AutoBackupScheduler.reschedule(getApplication<Application>(), enabled)
    }

    fun setBackupFolderUri(uri: String?) {
        preferences.setBackupFolderUri(uri)
    }

    fun clearBackupMessage() {
        _backupMessage.value = null
    }

    fun runBackupNow() {
        viewModelScope.launch(Dispatchers.IO) {
            val ok = AutoBackup.perform(getApplication(), preferences)
            _backupMessage.value = if (ok) "Backup saved." else "Backup failed - choose a folder first."
        }
    }

    // ---- hidden apps / vault ----

    fun setAppHidden(packageName: String, hidden: Boolean) {
        preferences.setAppHidden(packageName, hidden)
    }

    /**
     * Stores the vault PIN. It is always kept as exactly four digits, so a PIN such as 0078 can
     * be typed in the search bar as `vault 0078` and still match.
     */
    fun setVaultPin(pin: String) {
        val digits = pin.filter { it.isDigit() }.take(4)
        if (digits.length != 4) return
        preferences.setVaultPin(digits)
    }

    /** True when [typed] is the vault PIN. Spaces and stray characters are ignored. */
    fun checkVaultPin(typed: String): Boolean {
        val digits = typed.filter { it.isDigit() }
        return digits.isNotEmpty() && digits == preferences.vaultPin.value.filter { it.isDigit() }
    }

    /**
     * Runs a typed search-bar command. Returns true when the query was a command and should not
     * be treated as a normal search.
     *
     * `vault <pin>` opens the hidden-apps screen (hide / unhide / change PIN).
     * `apps <pin>`  opens the same screen in launch mode, so a hidden app can be started without
     *               unhiding it first.
     */
    fun runSearchCommand(query: String): Boolean {
        val parts = query.trim().split(Regex("\\s+"))
        if (parts.size != 2) return false
        val command = parts[0].lowercase()
        if (command != "vault" && command != "apps") return false
        if (!checkVaultPin(parts[1])) return false
        _showSearch.value = false
        _hiddenAppsMode.value = if (command == "vault") HiddenAppsMode.MANAGE else HiddenAppsMode.LAUNCH
        _showHiddenApps.value = true
        return true
    }

    /** Hidden apps, resolved to full [AppInfo]s (used by the `apps <pin>` launch mode). */
    val hiddenAppInfos: StateFlow<List<AppInfo>> =
        combine(_installedApps, preferences.hiddenApps) { apps, hidden ->
            apps.filter { it.packageName in hidden }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setHiddenAppsMode(mode: HiddenAppsMode) {
        _hiddenAppsMode.value = mode
    }

    /** Pins the Home clock to [hour]:[minute] for 30 seconds (the decoy), then it reverts. */
    fun setClockOverride(hour: Int, minute: Int) {
        val cal = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, hour)
            set(java.util.Calendar.MINUTE, minute)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        _clockOverride.value = cal.timeInMillis
        clockOverrideJob?.cancel()
        clockOverrideJob = viewModelScope.launch {
            delay(30_000)
            _clockOverride.value = null
        }
    }

    fun setShowStudyPopups(show: Boolean) {
        _showStudyPopups.value = show
    }

    // Smart App Launching (no usage limits any more — just launch)
    fun launchApp(app: AppInfo) {
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

    // A freshly added course/subject/topic/class never carries a Reminder or Deadline, so there
    // is nothing to (re)schedule — skipping that work is what keeps adding instant.

    fun addStudyCourse(name: String) {
        preferences.addStudyCourse(name)
    }

    fun addStudySubject(courseId: String, name: String) {
        preferences.addStudySubject(courseId, name)
    }

    fun addStudyTopic(courseId: String, subjectId: String, name: String) {
        preferences.addStudyTopic(courseId, subjectId, name)
    }

    fun addStudyClass(courseId: String, subjectId: String, topicId: String, name: String) {
        preferences.addStudyClass(courseId, subjectId, topicId, name)
    }

    fun toggleStudyClass(courseId: String, subjectId: String, topicId: String, classId: String) {
        preferences.toggleStudyClass(courseId, subjectId, topicId, classId)
        rescheduleStudyReminders()
    }

    /** Clears every study task's Reminder & Deadline (backs the Study Manager "Clean" button). */
    fun clearAllStudySchedules() {
        preferences.clearAllStudySchedules()
        rescheduleStudyReminders()
    }

    fun deleteStudyCourse(courseId: String) {
        val index = preferences.studyCourses.value.indexOfFirst { it.id == courseId }
        val removed = if (index >= 0) preferences.studyCourses.value[index] else null
        preferences.deleteStudyCourse(courseId)
        rescheduleStudyReminders()
        if (removed != null) offerUndo("Course deleted") { preferences.restoreStudyCourse(removed, index) }
    }

    fun deleteStudySubject(courseId: String, subjectId: String) {
        val course = preferences.studyCourses.value.firstOrNull { it.id == courseId }
        val index = course?.subjects?.indexOfFirst { it.id == subjectId } ?: -1
        val removed = if (index >= 0) course?.subjects?.get(index) else null
        preferences.deleteStudySubject(courseId, subjectId)
        rescheduleStudyReminders()
        if (removed != null) offerUndo("Subject deleted") { preferences.restoreStudySubject(courseId, removed, index) }
    }

    fun deleteStudyTopic(courseId: String, subjectId: String, topicId: String) {
        val subject = preferences.studyCourses.value.firstOrNull { it.id == courseId }
            ?.subjects?.firstOrNull { it.id == subjectId }
        val index = subject?.topics?.indexOfFirst { it.id == topicId } ?: -1
        val removed = if (index >= 0) subject?.topics?.get(index) else null
        preferences.deleteStudyTopic(courseId, subjectId, topicId)
        rescheduleStudyReminders()
        if (removed != null) offerUndo("Topic deleted") {
            preferences.restoreStudyTopic(courseId, subjectId, removed, index)
        }
    }

    fun deleteStudyClass(courseId: String, subjectId: String, topicId: String, classId: String) {
        val topic = preferences.studyCourses.value.firstOrNull { it.id == courseId }
            ?.subjects?.firstOrNull { it.id == subjectId }
            ?.topics?.firstOrNull { it.id == topicId }
        val index = topic?.classes?.indexOfFirst { it.id == classId } ?: -1
        val removed = if (index >= 0) topic?.classes?.get(index) else null
        preferences.deleteStudyClass(courseId, subjectId, topicId, classId)
        rescheduleStudyReminders()
        if (removed != null) offerUndo("Class deleted") {
            preferences.restoreStudyClass(courseId, subjectId, topicId, removed, index)
        }
    }

    // ---- Drag & drop reordering (moves an item among its siblings) ----

    fun moveStudyCourse(courseId: String, direction: Int) = preferences.moveStudyCourse(courseId, direction)

    fun moveStudySubject(courseId: String, subjectId: String, direction: Int) =
        preferences.moveStudySubject(courseId, subjectId, direction)

    fun moveStudyTopic(courseId: String, subjectId: String, topicId: String, direction: Int) =
        preferences.moveStudyTopic(courseId, subjectId, topicId, direction)

    fun moveStudyClass(courseId: String, subjectId: String, topicId: String, classId: String, direction: Int) =
        preferences.moveStudyClass(courseId, subjectId, topicId, classId, direction)

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

    // ---------------------------------------------------------------------
    // Quick Time Table
    // ---------------------------------------------------------------------

    fun setTimeTableWindow(startMinutes: Int, endMinutes: Int) {
        val s = preferences.timeTable.value
        preferences.saveTimeTable(s.copy(windowStartMinutes = startMinutes, windowEndMinutes = endMinutes))
    }

    fun setTimeTableDays(days: Int) {
        val s = preferences.timeTable.value
        preferences.saveTimeTable(s.copy(days = days.coerceIn(1, 7)))
    }

    fun setSubjectPlan(subjectId: String, durationMinutes: Int, restMinutes: Int, sessionsPerDay: Int) {
        val s = preferences.timeTable.value
        val updated = s.subjectSettings.toMutableMap()
        updated[subjectId] = SubjectPlanSettings(
            durationMinutes = durationMinutes,
            restMinutes = restMinutes,
            sessionsPerDay = sessionsPerDay.coerceIn(1, 3)
        )
        preferences.saveTimeTable(s.copy(subjectSettings = updated))
    }

    fun generateTimeTable() {
        val settings = preferences.timeTable.value
        val courses = preferences.studyCourses.value
        if (courses.isEmpty()) {
            _timeTableAlert.value = "Add a course and a subject in Study Manager first."
            return
        }
        val result = TimeTablePlanner.generate(
            courses = courses,
            previousClassIds = settings.generatedClassIds.toSet(),
            settings = settings,
            todayStartMillis = startOfToday()
        )
        if (result.error != null) {
            // Not enough time — leave the existing plan untouched and surface the calculation.
            _timeTableAlert.value = result.error
            return
        }
        preferences.saveStudyCourses(result.courses)
        preferences.saveTimeTable(
            settings.copy(entries = result.entries, generatedClassIds = result.generatedClassIds)
        )
        _timeTableAlert.value = null
        rescheduleStudyReminders()
    }

    fun dismissTimeTableAlert() {
        _timeTableAlert.value = null
    }

    private fun startOfToday(): Long = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    // ---------------------------------------------------------------------
    // Study Popups
    // ---------------------------------------------------------------------

    private fun savePopupSettings(s: StudyPopupSettings) {
        preferences.savePopupSettings(s)
        StudyPopupScheduler.reschedule(getApplication<Application>(), s)
    }

    fun setPopupEnabled(enabled: Boolean) =
        savePopupSettings(preferences.popupSettings.value.copy(enabled = enabled))

    fun setPopupWindow(startMinutes: Int, endMinutes: Int) =
        savePopupSettings(preferences.popupSettings.value.copy(startMinutes = startMinutes, stopMinutes = endMinutes))

    fun setPopupInterval(minutes: Int) =
        savePopupSettings(preferences.popupSettings.value.copy(intervalMinutes = minutes))

    /** Reads one or more card files (JSON) and adds every card it finds. */
    fun importCardFiles(uris: List<Uri>) {
        viewModelScope.launch {
            val existing = preferences.flashcardBank.value.toMutableList()
            var added = 0
            for (uri in uris) {
                val text = withContext(Dispatchers.IO) {
                    try {
                        getApplication<Application>().contentResolver.openInputStream(uri)
                            ?.use { it.readBytes().decodeToString() }
                    } catch (_: Exception) { null }
                } ?: continue
                val deck = displayName(uri) ?: "file"
                val cards = CardFileParser.parse(text, deck)
                existing.addAll(cards)
                added += cards.size
            }
            if (added > 0) preferences.saveFlashcardBank(existing)
            _cardImportCount.value = added
        }
    }

    fun clearCards() {
        preferences.saveFlashcardBank(emptyList())
        _cardImportCount.value = null
    }

    fun setShowCards(show: Boolean) {
        _showCards.value = show
    }

    fun updateCard(card: FlashcardItem) {
        preferences.updateCard(card)
    }

    fun deleteCard(id: String) {
        val index = preferences.flashcardBank.value.indexOfFirst { it.id == id }
        val removed = if (index >= 0) preferences.flashcardBank.value[index] else null
        preferences.deleteCard(id)
        if (removed != null) offerUndo("Card deleted") { preferences.restoreCard(removed, index) }
    }

    /** Turns a whole deck on or off for the popups. */
    fun setDeckEnabled(deck: String, enabled: Boolean) {
        val s = preferences.popupSettings.value
        val disabled = s.disabledDecks.toMutableList()
        if (enabled) {
            disabled.remove(deck)
        } else if (!disabled.contains(deck)) {
            disabled.add(deck)
        }
        savePopupSettings(s.copy(disabledDecks = disabled))
    }

    private fun displayName(uri: Uri): String? = try {
        getApplication<Application>().contentResolver.query(uri, null, null, null, null)?.use { cur ->
            val idx = cur.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (idx >= 0 && cur.moveToFirst()) cur.getString(idx) else null
        }
    } catch (_: Exception) { null }

    private var studyRescheduleJob: Job? = null
    private var alertRescheduleJob: Job? = null
    private var taskRescheduleJob: Job? = null

    /**
     * (Re)arms the Study Manager alarms. This walks every task and touches the AlarmManager, so
     * it runs off the main thread and debounces bursts of edits — doing it inline on the main
     * thread is what made adding / completing a class feel laggy.
     */
    private fun rescheduleStudyReminders() {
        studyRescheduleJob?.cancel()
        studyRescheduleJob = viewModelScope.launch(Dispatchers.Default) {
            delay(150)
            StudyReminderScheduler.rescheduleAll(getApplication<Application>(), preferences.studyCourses.value)
        }
    }

    /** Same as [rescheduleStudyReminders]: off the main thread, and debounced. */
    private fun rescheduleAlertReminders() {
        alertRescheduleJob?.cancel()
        alertRescheduleJob = viewModelScope.launch(Dispatchers.Default) {
            delay(150)
            AlertReminderScheduler.rescheduleAll(getApplication<Application>(), preferences.alerts.value)
        }
    }

    /** Same as [rescheduleStudyReminders]: off the main thread, and debounced. */
    private fun rescheduleTaskReminders() {
        taskRescheduleJob?.cancel()
        taskRescheduleJob = viewModelScope.launch(Dispatchers.Default) {
            delay(150)
            TaskReminderScheduler.rescheduleAll(getApplication<Application>(), preferences.tasks.value)
        }
    }

    // ---------------------------------------------------------------------
    // Undo (delete-undo snackbar)
    // ---------------------------------------------------------------------

    private val _undoMessage = MutableStateFlow<String?>(null)
    val undoMessage: StateFlow<String?> = _undoMessage.asStateFlow()
    private var lastUndo: (() -> Unit)? = null
    private var undoTimerJob: Job? = null

    private fun offerUndo(message: String, restore: () -> Unit) {
        lastUndo = restore
        // null then the message, so the bar re-shows even when the text is identical.
        _undoMessage.value = null
        _undoMessage.value = message
        // The bar hides itself after a few seconds. The timer lives here (not in the UI) so it
        // behaves the same whether the bar is drawn on the main screen or inside the sheet.
        undoTimerJob?.cancel()
        undoTimerJob = viewModelScope.launch {
            delay(4_000)
            lastUndo = null
            _undoMessage.value = null
        }
    }

    fun performUndo() {
        undoTimerJob?.cancel()
        lastUndo?.invoke()
        lastUndo = null
        _undoMessage.value = null
    }

    fun clearUndo() {
        undoTimerJob?.cancel()
        lastUndo = null
        _undoMessage.value = null
    }

    // ---------------------------------------------------------------------
    // Backup & Restore
    // ---------------------------------------------------------------------

    fun exportAllData(): String = preferences.exportAllData()

    fun importAllData(json: String): Boolean {
        val ok = preferences.importAllData(json)
        if (ok) {
            rescheduleStudyReminders()
            rescheduleAlertReminders()
            StudyPopupScheduler.reschedule(getApplication<Application>(), preferences.popupSettings.value)
        }
        return ok
    }

    companion object {
        /**
         * The palettes auto-rotation walks through. The Rainbow theme is left out on
         * purpose: it is a hand-picked choice, not something the clock should switch
         * on by itself.
         */
        private val ROTATING_THEMES = listOf(
            "default", "pink", "green", "saffron", "seattle", "ocean", "lavender"
        )
    }
}
