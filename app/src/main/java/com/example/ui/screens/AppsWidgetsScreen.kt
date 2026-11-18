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
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * The right page (v1.20.0): the launcher's apps and its widgets, in one place.
 *
 * This page replaces the old "Apps" tab that used to sit inside Settings, and it
 * absorbs the widget switches that used to hide behind Settings > Widgets. The
 * split is now: Settings (left) is how the launcher *looks and behaves*, this
 * page (right) is what the launcher *gives you* — its apps, its widgets, and
 * the apps on your phone.
 */
@Composable
fun AppsWidgetsScreen(
    appCount: Int,
    isDarkTheme: Boolean,
    showTime: Boolean,
    showDate: Boolean,
    showAlerts: Boolean,
    showTasks: Boolean,
    showStudyDue: Boolean,
    showGoalsWidget: Boolean,
    badgesEnabled: Boolean,
    onToggleShowTime: (Boolean) -> Unit,
    onToggleShowDate: (Boolean) -> Unit,
    onToggleShowAlerts: (Boolean) -> Unit,
    onToggleShowTasks: (Boolean) -> Unit,
    onToggleShowStudyDue: (Boolean) -> Unit,
    onToggleShowGoalsWidget: (Boolean) -> Unit,
    onOpenDrawer: () -> Unit,
    onOpenSearch: () -> Unit,
    onOpenStudyManager: () -> Unit,
    onOpenAlerts: () -> Unit,
    onOpenTasks: () -> Unit,
    onOpenStudyPopups: () -> Unit,
    onOpenGoals: () -> Unit,
    onOpenFavorites: () -> Unit,
    onOpenHiddenApps: () -> Unit,
    onOpenAppLock: () -> Unit,
    onOpenWidgetSettings: () -> Unit,
    onOpenThemes: () -> Unit,
    onOpenBadgeSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 24.dp, vertical = 20.dp)
            .testTag("apps_widgets_screen")
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Apps & Widgets",
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 32.sp
            ),
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(8.dp))

        Box(
            modifier = Modifier
                .width(180.dp)
                .height(2.dp)
                .background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.2f))
        )

        Spacer(modifier = Modifier.height(18.dp))

        // ---- All apps: the drawer lives on a swipe-up now, this is the visible door ----
        SectionTitle("All apps")

        AppRow(
            title = "App drawer",
            subtitle = if (appCount > 0) "$appCount apps - swipe up on Home, or tap here"
            else "Swipe up on Home, or tap here",
            icon = Icons.Default.Apps,
            onClick = onOpenDrawer,
            testTag = "aw_drawer"
        )

        Spacer(modifier = Modifier.height(10.dp))

        AppRow(
            title = "Search everything",
            subtitle = "Apps, contacts, files, tasks, notes, cards and maths",
            icon = Icons.Default.Search,
            onClick = onOpenSearch,
            testTag = "aw_search"
        )

        Spacer(modifier = Modifier.height(24.dp))

        // ---- The launcher's own apps ----
        SectionTitle("Launcher apps")

        AppRow(
            title = "Study Manager",
            subtitle = "Courses > Subjects > Topics > Classes, deadlines & reminders",
            icon = Icons.Default.School,
            onClick = onOpenStudyManager,
            testTag = "aw_study_manager"
        )
        Spacer(modifier = Modifier.height(10.dp))
        AppRow(
            title = "Alerts",
            subtitle = "Reminders with optional notifications",
            icon = Icons.Default.NotificationsActive,
            onClick = onOpenAlerts,
            testTag = "aw_alerts"
        )
        Spacer(modifier = Modifier.height(10.dp))
        AppRow(
            title = "Tasks",
            subtitle = "Things to do, with due dates and reminders",
            icon = Icons.Default.Checklist,
            onClick = onOpenTasks,
            testTag = "aw_tasks"
        )
        Spacer(modifier = Modifier.height(10.dp))
        AppRow(
            title = "Study Popups",
            subtitle = "MCQ & flashcard nudges through the day",
            icon = Icons.Default.NotificationsActive,
            onClick = onOpenStudyPopups,
            testTag = "aw_study_popups"
        )
        Spacer(modifier = Modifier.height(10.dp))
        AppRow(
            title = "Goals",
            subtitle = "Track a regular activity and tick it from the Home widget",
            icon = Icons.Default.Flag,
            onClick = onOpenGoals,
            testTag = "aw_goals"
        )

        Spacer(modifier = Modifier.height(24.dp))

        // ---- Widgets on Home ----
        SectionTitle("Widgets on Home")

        WidgetRow(
            title = "Clock",
            subtitle = "Time, and the date under it",
            checked = showTime,
            onCheckedChange = onToggleShowTime,
            testTag = "aw_widget_clock"
        )
        Spacer(modifier = Modifier.height(10.dp))
        WidgetRow(
            title = "Date",
            subtitle = "Tap it to open the Calendar",
            checked = showDate,
            onCheckedChange = onToggleShowDate,
            testTag = "aw_widget_date"
        )
        Spacer(modifier = Modifier.height(10.dp))
        WidgetRow(
            title = "Alerts",
            subtitle = "Your tags and reminders",
            checked = showAlerts,
            onCheckedChange = onToggleShowAlerts,
            testTag = "aw_widget_alerts"
        )
        Spacer(modifier = Modifier.height(10.dp))
        WidgetRow(
            title = "Tasks",
            subtitle = "The checklist, tickable from Home",
            checked = showTasks,
            onCheckedChange = onToggleShowTasks,
            testTag = "aw_widget_tasks"
        )
        Spacer(modifier = Modifier.height(10.dp))
        WidgetRow(
            title = "Study",
            subtitle = "Progress, and what is due in the next 7 days",
            checked = showStudyDue,
            onCheckedChange = onToggleShowStudyDue,
            testTag = "aw_widget_study"
        )
        Spacer(modifier = Modifier.height(10.dp))
        WidgetRow(
            title = "Goals",
            subtitle = "Tick a goal straight from Home",
            checked = showGoalsWidget,
            onCheckedChange = onToggleShowGoalsWidget,
            testTag = "aw_widget_goals"
        )

        Spacer(modifier = Modifier.height(10.dp))

        AppRow(
            title = "Widget settings",
            subtitle = "Where the date tap goes, and the rest",
            icon = Icons.Default.Widgets,
            onClick = onOpenWidgetSettings,
            testTag = "aw_widget_settings"
        )

        Spacer(modifier = Modifier.height(24.dp))

        // ---- Apps on your phone ----
        SectionTitle("Apps on your phone")

        AppRow(
            title = "Favorites & groups",
            subtitle = "Pick the five slots of the bottom bar, and pack apps into groups",
            icon = Icons.Default.Star,
            onClick = onOpenFavorites,
            testTag = "aw_favorites"
        )
        Spacer(modifier = Modifier.height(10.dp))
        AppRow(
            title = "Hidden apps",
            subtitle = "Keep apps out of the drawer, behind a PIN",
            icon = Icons.Default.Apps,
            onClick = onOpenHiddenApps,
            testTag = "aw_hidden_apps"
        )
        Spacer(modifier = Modifier.height(10.dp))
        AppRow(
            title = "Locked apps",
            subtitle = "Ask for the PIN before an app opens",
            icon = Icons.Default.Lock,
            onClick = onOpenAppLock,
            testTag = "aw_locked_apps"
        )
        Spacer(modifier = Modifier.height(10.dp))
        AppRow(
            title = "Notification badges",
            subtitle = if (badgesEnabled)
                "On - unread counts on app icons"
            else
                "Off - tap to allow notification access",
            icon = Icons.Default.NotificationsActive,
            onClick = onOpenBadgeSettings,
            testTag = "aw_badges"
        )

        Spacer(modifier = Modifier.height(24.dp))

        // ---- Appearance, one tap away from the page it affects ----
        SectionTitle("Appearance")

        AppRow(
            title = "Themes",
            subtitle = "Eight palettes, night mode, and the animations",
            icon = Icons.Default.Palette,
            onClick = onOpenThemes,
            testTag = "aw_themes"
        )

        Spacer(modifier = Modifier.height(36.dp))
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelMedium.copy(
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.sp
        ),
        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
        modifier = Modifier.padding(bottom = 10.dp)
    )
}

@Composable
private fun AppRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit,
    testTag: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .testTag(testTag),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.65f)
                )
            }
        }
    }
}

@Composable
private fun WidgetRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onCheckedChange(!checked) }
            .testTag(testTag),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.65f)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                modifier = Modifier.testTag("${testTag}_switch")
            )
        }
    }
}
