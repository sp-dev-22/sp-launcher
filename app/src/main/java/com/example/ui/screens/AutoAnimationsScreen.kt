package com.example.ui.screens

import android.app.TimePickerDialog
import android.content.Context
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * Settings > Automatics.
 *
 * Everything that runs on a schedule, kept off the Themes sheet where there was no
 * room left for it. The animation toggles left with the animations in v1.11.5 and
 * came back in lighter form in v1.14.0.
 */
@Composable
fun AutoAnimationsScreen(
    autoNightEnabled: Boolean,
    nightStartMinutes: Int,
    nightEndMinutes: Int,
    autoThemeMode: String,
    onToggleAutoNight: (Boolean) -> Unit,
    onSetNightWindow: (Int, Int) -> Unit,
    onSetAutoThemeMode: (String) -> Unit,
    diwaliAnimationsEnabled: Boolean = false,
    onToggleDiwaliAnimations: (Boolean) -> Unit = {},
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(scrollState)
            .testTag("auto_animations_screen")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp, horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack, modifier = Modifier.testTag("auto_animations_back_btn")) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
            }
            Text(
                text = "Automatics",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.weight(1f)
            )
        }

        Column(modifier = Modifier.padding(horizontal = 20.dp)) {

            // ---- Auto night mode ----
            SectionTitle("AUTO NIGHT MODE")

            SwitchRow(
                title = "Auto night mode",
                subtitle = "Go dark between the two times below",
                checked = autoNightEnabled,
                onCheckedChange = onToggleAutoNight,
                testTag = "auto_night_switch"
            )

            if (autoNightEnabled) {
                TimeRow(
                    label = "Night from",
                    minutes = nightStartMinutes,
                    onPick = { onSetNightWindow(it, nightEndMinutes) }
                )
                TimeRow(
                    label = "Night until",
                    minutes = nightEndMinutes,
                    onPick = { onSetNightWindow(nightStartMinutes, it) }
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 14.dp))

            // ---- Auto theme change ----
            SectionTitle("AUTO CHANGE THEME")

            Text(
                text = "Steps through every palette one at a time, Rainbow included.",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OptionChip("Off", autoThemeMode == "off") { onSetAutoThemeMode("off") }
                OptionChip("Every 1 hour", autoThemeMode == "hour") { onSetAutoThemeMode("hour") }
                OptionChip("Every 1 day", autoThemeMode == "day") { onSetAutoThemeMode("day") }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 14.dp))

            // ---- Theme animations (v1.14.0, lighter, OFF by default) ----
            SectionTitle("THEME ANIMATIONS")

            Text(
                text = "Light effects on the Home screen only, and only while their own theme " +
                    "is active. They pause automatically when Battery Saver is on.",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            Spacer(modifier = Modifier.height(8.dp))

            SwitchRow(
                title = "Diwali animations",
                subtitle = "Lamps, rockets and stars — only with the Rainbow theme",
                checked = diwaliAnimationsEnabled,
                onCheckedChange = onToggleDiwaliAnimations,
                testTag = "diwali_animations_switch"
            )

            Spacer(modifier = Modifier.height(36.dp))
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall.copy(
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        ),
        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f),
        modifier = Modifier.padding(bottom = 6.dp)
    )
}

@Composable
private fun SwitchRow(
    title: String,
    subtitle: String?,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.testTag(testTag)
        )
    }
}

@Composable
private fun TimeRow(label: String, minutes: Int, onPick: (Int) -> Unit) {
    val context = LocalContext.current
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(
            text = formatMinutes(minutes),
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .clickable { pickTime(context, minutes, onPick) }
                .padding(horizontal = 10.dp, vertical = 6.dp)
        )
    }
}

@Composable
private fun OptionChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val bg = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
    val fg = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(bg)
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 7.dp)
            .testTag("option_chip_$label")
    ) {
        Text(text = label, fontSize = 12.sp, color = fg, fontWeight = FontWeight.Medium)
    }
}

private fun formatMinutes(minutes: Int): String {
    val cal = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, minutes / 60)
        set(Calendar.MINUTE, minutes % 60)
    }
    return SimpleDateFormat("hh:mm a", Locale.getDefault()).format(cal.time)
}

private fun pickTime(context: Context, initialMinutes: Int, onPicked: (Int) -> Unit) {
    TimePickerDialog(
        context,
        { _, hour, minute -> onPicked(hour * 60 + minute) },
        initialMinutes / 60,
        initialMinutes % 60,
        false
    ).show()
}
