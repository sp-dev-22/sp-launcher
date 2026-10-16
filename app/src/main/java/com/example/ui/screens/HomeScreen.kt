package com.example.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AlertItem
import com.example.data.model.AppInfo
import com.example.data.model.CardStats
import com.example.data.model.StudyDueItem
import com.example.data.model.TaskItem
import com.example.ui.components.AlertsWidget
import com.example.ui.components.AppFavoriteChip
import com.example.ui.components.ClockWidget
import com.example.ui.components.StudyDueWidget
import com.example.ui.components.TasksWidget

@OptIn(ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    favoriteApps: List<AppInfo>,
    showTime: Boolean,
    showDate: Boolean,
    showAlerts: Boolean,
    showTasks: Boolean,
    alerts: List<AlertItem>,
    tasks: List<TaskItem>,
    showStudyDue: Boolean,
    studyDueItems: List<StudyDueItem>,
    cardsStats: CardStats,
    isDarkTheme: Boolean,
    onAppClick: (AppInfo) -> Unit,
    onAppLongClick: (AppInfo) -> Unit,
    onToggleTask: (String) -> Unit,
    onAddTask: (String) -> Unit,
    onOpenWidgetsManagement: () -> Unit,
    onOpenStudyManager: () -> Unit,
    onOpenCards: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(bottom = 24.dp)
            .testTag("home_screen"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
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
                modifier = Modifier.fillMaxWidth()
            )

            // Study classes due today / overdue
            if (showStudyDue && studyDueItems.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                StudyDueWidget(
                    items = studyDueItems,
                    isDarkTheme = isDarkTheme,
                    onOpenStudyManager = onOpenStudyManager,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Study cards progress
            if (cardsStats.total > 0) {
                Spacer(modifier = Modifier.height(8.dp))
                CardsProgressCard(
                    stats = cardsStats,
                    onClick = onOpenCards,
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
        }

        // Favorite Apps (Colorful Text Pills)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (favoriteApps.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Swipe right to App Drawer\nor open Settings to add Favorite apps",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    maxItemsInEachRow = 3
                ) {
                    favoriteApps.forEach { app ->
                        AppFavoriteChip(
                            app = app,
                            isDarkTheme = isDarkTheme,
                            onClick = { onAppClick(app) },
                            onLongClick = { onAppLongClick(app) },
                            modifier = Modifier.padding(2.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CardsProgressCard(stats: CardStats, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable { onClick() }
            .testTag("cards_progress_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
            Text(
                text = "STUDY CARDS",
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                letterSpacing = 1.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "${stats.mastered} of ${stats.total} mastered · ${stats.due} due · ${stats.masteredToday} today",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
    }
}
