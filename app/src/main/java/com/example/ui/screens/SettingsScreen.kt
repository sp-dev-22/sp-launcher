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
import androidx.compose.material.icons.filled.Brightness4
import androidx.compose.material.icons.filled.Brightness7
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockClock
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SettingsScreen(
    isDarkTheme: Boolean,
    isFocusModeActive: Boolean,
    onOpenFocusModeSettings: () -> Unit,
    onOpenFocusHistory: () -> Unit,
    onOpenAppLimits: () -> Unit,
    onOpenThemes: () -> Unit,
    onOpenEditFavorites: () -> Unit,
    onOpenArrangeFavorites: () -> Unit,
    onOpenWidgetsManagement: () -> Unit,
    onOpenStudyManager: () -> Unit,
    onOpenBackupRestore: () -> Unit,
    onSetDefaultLauncher: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 24.dp, vertical = 20.dp)
            .testTag("settings_screen")
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Title matching Screenshot 3
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

        Spacer(modifier = Modifier.height(24.dp))

        // 1. Focus Mode Setup & Status
        // During Focus Mode, this option is strictly NON-CLICKABLE as requested!
        SettingsItemRow(
            title = if (isFocusModeActive) "Focus Mode (Locked)" else "Focus Mode",
            subtitle = if (isFocusModeActive) "🔒 Non-clickable during active session • Long-press timer 3s to end" else "Allowed apps, silent alerts & Pomodoro timer",
            icon = {
                Icon(
                    imageVector = if (isFocusModeActive) Icons.Default.Lock else Icons.Default.HourglassTop,
                    contentDescription = null,
                    tint = if (isFocusModeActive) Color(0xFFE53935) else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            },
            enabled = !isFocusModeActive,
            onClick = {
                if (!isFocusModeActive) {
                    onOpenFocusModeSettings()
                }
            },
            testTag = "settings_focus_mode"
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 1b. Focus History (Save & Custom Duration Clear)
        SettingsItemRow(
            title = "Focus History",
            subtitle = "View completed sessions & clear by custom date/time",
            icon = {
                Icon(
                    imageVector = Icons.Default.History,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            },
            onClick = onOpenFocusHistory,
            testTag = "settings_focus_history"
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 2. App Usage Time & Limits
        SettingsItemRow(
            title = "App Use Time & Limits",
            subtitle = "Track daily screen time & instant app limit lock",
            icon = {
                Icon(
                    imageVector = Icons.Default.LockClock,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            },
            onClick = onOpenAppLimits,
            testTag = "settings_app_limits"
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Themes (Pink, Green, Saffron, Seattle) + Night Mode
        SettingsItemRow(
            title = "Themes",
            subtitle = "Pink, Green, Saffron, Seattle & Night Mode",
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

        // 5. Edit Favorites (Select & Checkbox list)
        SettingsItemRow(
            title = "Edit Favorites",
            subtitle = "Select apps to pin on the Home Screen",
            icon = {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            },
            onClick = onOpenEditFavorites,
            testTag = "settings_edit_favorites"
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 6. Arrange / Reorder Favorites
        SettingsItemRow(
            title = "Arrange Favorites",
            subtitle = "Reorder or remove pinned apps",
            icon = {
                Icon(
                    imageVector = Icons.Default.SwapVert,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            },
            onClick = onOpenArrangeFavorites,
            testTag = "settings_arrange_favorites"
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 7. Widgets Configuration (Notes, Reminders, Goals, Tasks, Clock, Date)
        SettingsItemRow(
            title = "Widgets",
            subtitle = "Manage separate Notes, Reminders & Goals widgets",
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

        // 7b. Study Manager
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

        // 7c. Backup & Restore
        SettingsItemRow(
            title = "Backup & Restore",
            subtitle = "Save or restore all settings, todos and study data",
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

        // 8. Set Default Launcher
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

        Spacer(modifier = Modifier.height(36.dp))
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
