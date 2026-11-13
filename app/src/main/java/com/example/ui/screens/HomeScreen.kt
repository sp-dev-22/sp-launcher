package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AlertItem
import com.example.data.model.AppGroup
import com.example.data.model.AppInfo
import com.example.data.model.GoalItem
import com.example.data.model.StudyDueItem
import com.example.data.model.StudyProgress
import com.example.data.model.TaskItem
import com.example.data.model.TimeTableEntry
import com.example.ui.anim.DiwaliAnimationLayer
import com.example.ui.components.AlertsWidget
import com.example.ui.components.ClockWidget
import com.example.ui.components.FavoritesBar
import com.example.ui.components.GoalsWidget
import com.example.ui.components.LoadingIndicator
import com.example.ui.components.NowNextStrip
import com.example.ui.components.StudyDueWidget
import com.example.ui.components.TasksWidget

@Composable
fun HomeScreen(
    favoriteApps: List<AppInfo>,
    allApps: List<AppInfo>,
    favorites: List<String>,
    groups: List<AppGroup>,
    showTime: Boolean,
    showDate: Boolean,
    showAlerts: Boolean,
    showTasks: Boolean,
    alerts: List<AlertItem>,
    tasks: List<TaskItem>,
    showStudyDue: Boolean,
    studyDueItems: List<StudyDueItem>,
    studyProgress: StudyProgress,
    showGoals: Boolean,
    goals: List<GoalItem>,
    isDarkTheme: Boolean,
    onTickGoal: (String) -> Unit,
    onOpenGoals: () -> Unit,
    onAppClick: (AppInfo) -> Unit,
    onAppLongClick: (AppInfo) -> Unit,
    onEditFavorites: () -> Unit,
    onToggleTask: (String) -> Unit,
    onAddTask: (String) -> Unit,
    onOpenAlerts: () -> Unit,
    onOpenTasks: () -> Unit,
    onOpenStudyManager: () -> Unit,
    showNowNextClass: Boolean = true,
    timeTableEntries: List<TimeTableEntry> = emptyList(),
    onOpenTimeTable: () -> Unit = {},
    onOpenCalendar: () -> Unit,
    onTimeClick: () -> Unit,
    clockOverrideMillis: Long?,
    isLoadingApps: Boolean = false,
    showSearchBar: Boolean = true,
    onOpenSearch: (autoVoice: Boolean) -> Unit = {},
    showDiwaliAnimations: Boolean = false,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    // The favourites bar is pinned to the bottom; everything else scrolls above it, so the bar
    // stays put however many widgets the user has switched on.
    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("home_screen")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                // Room for whatever is pinned at the bottom: the favorites bar,
                // plus the search bar above it when that is switched on.
                .padding(bottom = if (showSearchBar) 150.dp else 84.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(10.dp))

                // Digital Clock & Date Widget
                ClockWidget(
                    showTime = showTime,
                    showDate = showDate,
                    onDateClick = onOpenCalendar,
                    onTimeClick = onTimeClick,
                    overrideMillis = clockOverrideMillis,
                    modifier = Modifier.fillMaxWidth()
                )

                // Now/Next class strip (v1.17.0): today's current and next Time
                // Table class. Hidden by its toggle, and hides itself when
                // there is no timetable or no more classes today.
                if (showNowNextClass) {
                    NowNextStrip(
                        entries = timeTableEntries,
                        onOpenTimeTable = onOpenTimeTable,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Study Manager status — progress + tasks due, in one combined card
                if (showStudyDue && (studyDueItems.isNotEmpty() || studyProgress.total > 0)) {
                    Spacer(modifier = Modifier.height(8.dp))
                    StudyDueWidget(
                        items = studyDueItems,
                        progress = studyProgress,
                        isDarkTheme = isDarkTheme,
                        onOpenStudyManager = onOpenStudyManager,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Goals widget (regular activity tracker) - tick straight from here
                if (showGoals && goals.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    GoalsWidget(
                        goals = goals,
                        isDarkTheme = isDarkTheme,
                        onTick = onTickGoal,
                        onOpenGoals = onOpenGoals,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Alerts widget (with custom tags + optional reminders) - hidden when empty
                if (showAlerts && alerts.isNotEmpty()) {
                    AlertsWidget(
                        items = alerts,
                        isDarkTheme = isDarkTheme,
                        onManage = onOpenAlerts,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Interactive Tasks Widget (hidden when there are no tasks)
                if (showTasks && tasks.isNotEmpty()) {
                    TasksWidget(
                        tasks = tasks,
                        isDarkTheme = isDarkTheme,
                        onToggleTask = onToggleTask,
                        onAddTask = onAddTask,
                        onManageTasks = onOpenTasks,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                }

                if (favorites.isEmpty() && favoriteApps.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isLoadingApps) {
                            // First open: the app list is still loading — say so,
                            // instead of looking like an empty launcher.
                            LoadingIndicator()
                        } else {
                            Text(
                                text = "Swipe right to the App Drawer,\nor tap + in the bar below to add favorites",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }

        // Pinned at the bottom: the Home search bar (v1.13, optional) directly
        // above the favorites bar. Both open the one Search screen — the bar is
        // only a door, never a second search.
        Column(
            modifier = Modifier.align(Alignment.BottomCenter),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (showSearchBar) {
                HomeSearchBar(
                    isDarkTheme = isDarkTheme,
                    onOpenSearch = onOpenSearch
                )
            }
            FavoritesBar(
                favorites = favorites,
                apps = allApps,
                groups = groups,
                isDarkTheme = isDarkTheme,
                onLaunchApp = onAppClick,
                onEdit = onEditFavorites
            )
        }

        // Theme animations (v1.14.0): Home screen only, only while their own theme is
        // active — MainScreen decides both, so this just draws what it is told to.
        // The canvas takes no pointer input, so nothing here blocks a tap.
        if (showDiwaliAnimations) {
            DiwaliAnimationLayer(modifier = Modifier.fillMaxSize())
        }
    }
}

/**
 * The Home search bar: a fake search box in the app drawer's style. Tapping it
 * opens Search; tapping its mic opens Search and starts voice input at once.
 */
@Composable
private fun HomeSearchBar(
    isDarkTheme: Boolean,
    onOpenSearch: (autoVoice: Boolean) -> Unit
) {
    val barColor = if (isDarkTheme) Color(0xFF26262A) else Color(0xFFF7EBE5)
    val placeholderColor = if (isDarkTheme) Color(0xFF9E9E9E) else Color(0xFF7D726D)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(barColor)
                .clickable { onOpenSearch(false) }
                .padding(start = 16.dp, end = 4.dp)
                .testTag("home_search_bar"),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Search",
                tint = placeholderColor,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "Search apps, contacts, files…",
                color = placeholderColor,
                fontSize = 15.sp,
                modifier = Modifier.weight(1f)
            )
            IconButton(
                onClick = { onOpenSearch(true) },
                modifier = Modifier.testTag("home_search_mic")
            ) {
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = "Voice search",
                    tint = placeholderColor,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}
