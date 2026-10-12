package com.example.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AppInfo
import com.example.data.model.Badge
import com.example.data.model.BannerItem
import com.example.data.model.TaskItem
import com.example.ui.components.AppFavoriteChip
import com.example.ui.components.CategorySwipeWidget
import com.example.ui.components.ClockWidget
import com.example.ui.components.LatestBadgeBanner
import com.example.ui.components.TasksWidget

@OptIn(ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    favoriteApps: List<AppInfo>,
    showTime: Boolean,
    showDate: Boolean,
    showNotes: Boolean,
    showReminders: Boolean,
    showGoals: Boolean,
    showTasks: Boolean,
    isFocusModeActive: Boolean,
    notes: List<BannerItem>,
    reminders: List<BannerItem>,
    goals: List<BannerItem>,
    tasks: List<TaskItem>,
    latestBadge: Badge?,
    completedSessions: Int,
    isDarkTheme: Boolean,
    onAppClick: (AppInfo) -> Unit,
    onAppLongClick: (AppInfo) -> Unit,
    onToggleTask: (String) -> Unit,
    onAddTask: (String) -> Unit,
    onOpenWidgetsManagement: () -> Unit,
    onPromptEndFocus: () -> Unit,
    onOpenBadgesSheet: () -> Unit,
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

            // 1. Latest Badge shown at top of Home Screen
            LatestBadgeBanner(
                badge = latestBadge,
                completedSessions = completedSessions,
                onClick = onOpenBadgesSheet
            )

            // 2. Digital Clock & Date Widget
            ClockWidget(
                showTime = showTime,
                showDate = showDate,
                modifier = Modifier.fillMaxWidth()
            )

            // Focus Mode Active Banner
            // Long press for 3s / click prompts confirmation to close session early
            if (isFocusModeActive) {
                Spacer(modifier = Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFFE53935).copy(alpha = 0.15f))
                        .combinedClickable(
                            onClick = onPromptEndFocus,
                            onLongClick = onPromptEndFocus
                        )
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .testTag("home_focus_mode_indicator")
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.HourglassTop,
                            contentDescription = null,
                            tint = Color(0xFFE53935),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Focus Mode Active • Long-press 3s to end",
                            color = Color(0xFFE53935),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 3. Separate Swipeable Widget for Notes
            if (showNotes) {
                CategorySwipeWidget(
                    category = "Note",
                    items = notes,
                    isDarkTheme = isDarkTheme,
                    onManageCategory = onOpenWidgetsManagement,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            // 4. Separate Swipeable Widget for Reminders
            if (showReminders) {
                CategorySwipeWidget(
                    category = "Reminder",
                    items = reminders,
                    isDarkTheme = isDarkTheme,
                    onManageCategory = onOpenWidgetsManagement,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            // 5. Separate Swipeable Widget for Goals
            if (showGoals) {
                CategorySwipeWidget(
                    category = "Goal",
                    items = goals,
                    isDarkTheme = isDarkTheme,
                    onManageCategory = onOpenWidgetsManagement,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            // 6. Interactive Tasks Widget
            if (showTasks) {
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

        // 7. Favorite Apps (Colorful Text Pills)
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
                        text = if (isFocusModeActive) "Only allowed apps are shown in Focus Mode" else "Swipe right to App Drawer\nor open Settings to add Favorite apps",
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
