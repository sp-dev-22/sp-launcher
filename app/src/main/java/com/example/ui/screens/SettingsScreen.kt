package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.BuildConfig

/**
 * Settings, in two tabs (v1.15.0): "Settings" holds the launcher's own settings,
 * "Apps" holds the launcher's apps (Study Manager, Alerts, Tasks, Study Popups,
 * Goals). Hidden Apps and Locked Apps deliberately live nowhere here — they open
 * from the search screen's menu instead.
 */
@Composable
fun SettingsScreen(
    isDarkTheme: Boolean,
    onOpenThemes: () -> Unit,
    onOpenAutoAnimations: () -> Unit,
    onOpenStudyManager: () -> Unit,
    onOpenAlerts: () -> Unit,
    onOpenTasks: () -> Unit,
    onOpenStudyPopups: () -> Unit,
    onOpenGoals: () -> Unit,
    onOpenWidgetsManagement: () -> Unit,
    onOpenFavorites: () -> Unit,
    onOpenBackupRestore: () -> Unit,
    onOpenSupport: () -> Unit,
    onSetDefaultLauncher: () -> Unit,
    backupWarning: String? = null,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    var selectedTab by remember { mutableIntStateOf(0) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 20.dp)
            .testTag("settings_screen")
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Title
        Text(
            text = "Settings",
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 32.sp
            ),
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Clean underline below Settings title
        Box(
            modifier = Modifier
                .width(180.dp)
                .height(2.dp)
                .background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.2f))
        )

        Spacer(modifier = Modifier.height(16.dp))

        TabRow(
            selectedTabIndex = selectedTab,
            modifier = Modifier.testTag("settings_tabs")
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("Settings") },
                modifier = Modifier.testTag("settings_tab_settings")
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("Apps") },
                modifier = Modifier.testTag("settings_tab_apps")
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
        ) {
            if (selectedTab == 0) {
                // ---- Settings tab: the launcher's own settings ----

                // Themes (Default, Pink, Green, Orange, Grey, Ocean, Lavender, Rainbow) + Night Mode
                SettingsItemRow(
                    title = "Themes",
                    subtitle = "Default, Pink, Green, Orange, Grey, Ocean, Lavender, Rainbow & Night Mode",
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Palette,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    },
                    onClick = onOpenThemes,
                    testTag = "settings_themes"
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Automatics: the schedules (moved off the Themes sheet)
                SettingsItemRow(
                    title = "Automatics",
                    subtitle = "Auto night mode and auto theme change",
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Timer,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    },
                    onClick = onOpenAutoAnimations,
                    testTag = "settings_auto_animations"
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Widgets (Clock, Date, Alerts, Tasks, Study Due)
                SettingsItemRow(
                    title = "Widgets",
                    subtitle = "Manage the Clock, Date, Alerts, Tasks, Study & Goals widgets",
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Checklist,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    },
                    onClick = onOpenWidgetsManagement,
                    testTag = "settings_widgets"
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Favorites (pick apps for the Home Screen + drag to reorder)
                SettingsItemRow(
                    title = "Favorites",
                    subtitle = "Pick apps for the Home Screen and drag to reorder",
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    },
                    onClick = onOpenFavorites,
                    testTag = "settings_favorites"
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Backup & Restore (warning replaces the subtitle when auto-backup is
                // on but its folder is missing or unusable — v1.13 hint)
                SettingsItemRow(
                    title = "Backup & Restore",
                    subtitle = backupWarning ?: "Save or restore all settings, alerts, tasks and study data",
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Backup,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    },
                    onClick = onOpenBackupRestore,
                    testTag = "settings_backup_restore"
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Support the developer (tips + links)
                SettingsItemRow(
                    title = "Support the developer",
                    subtitle = "Send a tip on UPI, or find me on GitHub and YouTube",
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Favorite,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    },
                    onClick = onOpenSupport,
                    testTag = "settings_support"
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Set Default Launcher
                SettingsItemRow(
                    title = "Set Default Launcher",
                    subtitle = "Set SP Launcher as system home screen",
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Home,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    },
                    onClick = onSetDefaultLauncher,
                    testTag = "settings_default_launcher"
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Version marker — makes it obvious which build is installed.
                Text(
                    text = "SP Launcher v${BuildConfig.VERSION_NAME} (build ${BuildConfig.VERSION_CODE})",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.45f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("settings_version")
                )
            } else {
                // ---- Apps tab: the launcher's apps ----

                // Study Manager
                SettingsItemRow(
                    title = "Study Manager",
                    subtitle = "Courses > Subjects > Topics > Classes, deadlines & reminders",
                    icon = {
                        Icon(
                            imageVector = Icons.Default.School,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    },
                    onClick = onOpenStudyManager,
                    testTag = "settings_study_manager"
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Alerts (managed in the Widgets sheet, opened on its Alerts section)
                SettingsItemRow(
                    title = "Alerts",
                    subtitle = "Reminders with optional notifications",
                    icon = {
                        Icon(
                            imageVector = Icons.Default.NotificationsActive,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    },
                    onClick = onOpenAlerts,
                    testTag = "settings_alerts"
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Tasks (managed in the Widgets sheet, opened on its Tasks section)
                SettingsItemRow(
                    title = "Tasks",
                    subtitle = "Things to do, with due dates and reminders",
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Checklist,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    },
                    onClick = onOpenTasks,
                    testTag = "settings_tasks"
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Study Popups (MCQ & flashcards)
                SettingsItemRow(
                    title = "Study Popups",
                    subtitle = "MCQ & flashcard nudges through the day",
                    icon = {
                        Icon(
                            imageVector = Icons.Default.NotificationsActive,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    },
                    onClick = onOpenStudyPopups,
                    testTag = "settings_study_popups"
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Goals (regular activity tracker)
                SettingsItemRow(
                    title = "Goals",
                    subtitle = "Track a regular activity and tick it from the Home widget",
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Flag,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    },
                    onClick = onOpenGoals,
                    testTag = "settings_goals"
                )
            }

            Spacer(modifier = Modifier.height(36.dp))
        }
    }
}

@Composable
private fun SettingsItemRow(
    title: String,
    subtitle: String,
    icon: @Composable () -> Unit,
    onClick: () -> Unit,
    enabled: Boolean = true,
    testTag: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(enabled = enabled) { onClick() }
            .testTag(testTag),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (enabled)
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            else
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.22f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            icon()

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    color = if (enabled) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.45f)
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = if (enabled) 0.65f else 0.35f)
                )
            }
        }
    }
}
