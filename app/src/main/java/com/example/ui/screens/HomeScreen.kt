package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.data.model.AlertItem
import com.example.data.model.AppGroup
import com.example.data.model.AppInfo
import com.example.data.model.GoalItem
import com.example.data.model.StudyDueItem
import com.example.data.model.StudyProgress
import com.example.data.model.TaskItem
import com.example.ui.components.AlertsWidget
import com.example.ui.components.ClockWidget
import com.example.ui.components.FavoritesBar
import com.example.ui.components.GoalsWidget
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
    onOpenWidgetsManagement: () -> Unit,
    onOpenStudyManager: () -> Unit,
    onOpenCalendar: () -> Unit,
    onTimeClick: () -> Unit,
    clockOverrideMillis: Long?,
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
                .padding(bottom = 84.dp),
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
                        onManage = onOpenWidgetsManagement,
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
                        onManageTasks = onOpenWidgetsManagement,
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
                        Text(
                            text = "Swipe right to the App Drawer,\nor tap + in the bar below to add favourites",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        FavoritesBar(
            favorites = favorites,
            apps = allApps,
            groups = groups,
            isDarkTheme = isDarkTheme,
            onLaunchApp = onAppClick,
            onEdit = onEditFavorites,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}
