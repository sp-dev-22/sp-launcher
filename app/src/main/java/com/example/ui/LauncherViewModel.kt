package com.example.ui

import android.app.Application
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.AlertItem
import com.example.data.model.AppInfo
import com.example.data.model.CardStats
import com.example.data.model.FlashcardItem
import com.example.data.model.StudyProgress
import com.example.data.model.StudyCourse
import com.example.data.model.StudyDueItem
import com.example.data.model.TaskItem
import com.example.data.model.allClassRefs
import com.example.data.repository.AppRepository
import com.example.data.repository.LauncherPreferences
import com.example.data.model.SubjectPlanSettings
import com.example.data.model.TimeTableSettings
import com.example.data.model.StudyPopupSettings
import com.example.service.AlertReminderScheduler
import com.example.service.StudyPopupScheduler
import com.example.service.StudyReminderScheduler
import com.example.util.CardFileParser
import com.example.util.TimeTablePlanner
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.Dispatchers
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
    val showTime = preferences.showTime
    val showDate = preferences.showDate
    val showAlerts = preferences.showAlerts
    val showTasks = preferences.showTasks
    val showStudyDue = preferences.showStudyDue
    val themeKey = preferences.themeKey
    val alerts = preferences.alerts
    val tasks = preferences.tasks
    val timeTable = preferences.timeTable
    val flashcardBank = preferences.flashcardBank
    val popupSettings = preferences.popupSettings

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
        StudyPopupScheduler.reschedule(application, preferences.popupSettings.value)
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

    fun updateTask(id: String, text: String) {
        preferences.updateTask(id, text)
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
        AlertReminderScheduler.cancel(getApplication(), id)
        preferences.removeAlert(id)
        rescheduleAlertReminders()
    }

    fun moveAlert(id: String, direction: Int) {
        preferences.moveAlert(id, direction)
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

    fun setSubjectPlan(subjectId: String, durationMinutes: Int, restMinutes: Int) {
        val s = preferences.timeTable.value
        val updated = s.subjectSettings.toMutableMap()
        updated[subjectId] = SubjectPlanSettings(durationMinutes = durationMinutes, restMinutes = restMinutes)
        preferences.saveTimeTable(s.copy(subjectSettings = updated))
    }

    fun generateTimeTable() {
        val settings = preferences.timeTable.value
        val courses = preferences.studyCourses.value
        if (courses.isEmpty()) return
        val result = TimeTablePlanner.generate(
            courses = courses,
            previousClassIds = settings.generatedClassIds.toSet(),
            settings = settings,
            todayStartMillis = startOfToday()
        )
        preferences.saveStudyCourses(result.courses)
        preferences.saveTimeTable(
            settings.copy(entries = result.entries, generatedClassIds = result.generatedClassIds)
        )
        rescheduleStudyReminders()
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
        preferences.deleteCard(id)
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

    private fun rescheduleStudyReminders() {
        StudyReminderScheduler.rescheduleAll(getApplication<Application>(), preferences.studyCourses.value)
    }

    private fun rescheduleAlertReminders() {
        AlertReminderScheduler.rescheduleAll(getApplication<Application>(), preferences.alerts.value)
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
}
