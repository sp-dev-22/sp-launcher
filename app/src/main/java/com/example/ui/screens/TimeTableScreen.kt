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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.SubjectPlanSettings
import com.example.data.model.TimeTableEntry
import com.example.ui.LauncherViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private val REST_OPTIONS = listOf(5, 10, 15, 20, 30, 60)

/** Plan lengths the user can pick — kept short on purpose (a longer plan gets boring). */
private val DAY_OPTIONS = listOf(1, 3, 7)

/** Sessions per subject per day: single / double / triple. */
private val SESSION_OPTIONS = listOf(1 to "Single", 2 to "Double", 3 to "Triple")

@Composable
fun TimeTableScreen(
    viewModel: LauncherViewModel,
    onBack: () -> Unit
) {
    val courses by viewModel.studyCourses.collectAsState()
    val tt by viewModel.timeTable.collectAsState()
    val alertMessage = viewModel.timeTableAlert.collectAsState().value

    // (courseName, subjectId, subjectName) for every subject in the tree
    val subjects = remember(courses) {
        courses.flatMap { c -> c.subjects.map { s -> Triple(c.name, s.id, s.name) } }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp)
            .testTag("timetable_screen")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack, modifier = Modifier.testTag("timetable_back_btn")) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
            }
            Icon(
                imageVector = Icons.Default.Schedule,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Quick Time Table",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
            )
        }

        Text(
            text = "Set how long each subject runs, how many sessions it gets (single / double / " +
                "triple), and your study window — then pick 1, 3 or 7 days and the app lays the " +
                "plan out from your un-completed classes.",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
        )

        Spacer(modifier = Modifier.height(14.dp))

        // ---- Study window ----
        Text("STUDY WINDOW (EACH DAY)", fontWeight = FontWeight.Bold, fontSize = 12.sp, letterSpacing = 1.sp)
        Spacer(modifier = Modifier.height(6.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                WindowRow(
                    label = "From",
                    minutes = tt.windowStartMinutes,
                    onPick = { viewModel.setTimeTableWindow(it, tt.windowEndMinutes) }
                )
                Spacer(modifier = Modifier.height(6.dp))
                WindowRow(
                    label = "To",
                    minutes = tt.windowEndMinutes,
                    onPick = { viewModel.setTimeTableWindow(tt.windowStartMinutes, it) }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ---- Plan length (1 / 3 / 7 days) ----
        Text("PLAN LENGTH", fontWeight = FontWeight.Bold, fontSize = 12.sp, letterSpacing = 1.sp)
        Spacer(modifier = Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DAY_OPTIONS.forEach { opt ->
                val selected = tt.days == opt
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            if (selected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                        .clickable { viewModel.setTimeTableDays(opt) }
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .testTag("timetable_days_$opt")
                ) {
                    Text(
                        text = if (opt == 1) "1 day" else "$opt days",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (selected) MaterialTheme.colorScheme.onPrimary
                        else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ---- Per-subject plan ----
        Text("SUBJECT DURATION, SESSIONS & REST", fontWeight = FontWeight.Bold, fontSize = 12.sp, letterSpacing = 1.sp)
        Spacer(modifier = Modifier.height(6.dp))

        if (subjects.isEmpty()) {
            Text(
                text = "Add a course and a subject in Study Manager first.",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
            )
        } else {
            subjects.forEach { (courseName, subjectId, subjectName) ->
                val cfg = tt.subjectSettings[subjectId] ?: SubjectPlanSettings()
                SubjectPlanCard(
                    courseName = courseName,
                    subjectName = subjectName,
                    cfg = cfg,
                    onDuration = { viewModel.setSubjectPlan(subjectId, it, cfg.restMinutes, cfg.sessionsPerDay) },
                    onRest = { viewModel.setSubjectPlan(subjectId, cfg.durationMinutes, it, cfg.sessionsPerDay) },
                    onSessions = { viewModel.setSubjectPlan(subjectId, cfg.durationMinutes, cfg.restMinutes, it) }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Button(
            onClick = { viewModel.generateTimeTable() },
            enabled = subjects.isNotEmpty(),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .testTag("timetable_generate_btn")
        ) {
            Icon(Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Generate ${tt.days}-day plan")
        }

        Spacer(modifier = Modifier.height(18.dp))

        // ---- Preview ----
        if (tt.entries.isNotEmpty()) {
            Text("YOUR PLAN", fontWeight = FontWeight.Bold, fontSize = 12.sp, letterSpacing = 1.sp)
            Spacer(modifier = Modifier.height(6.dp))
            val byDay = tt.entries.groupBy { it.day }
            byDay.keys.sorted().forEach { day ->
                val dayEntries = byDay[day].orEmpty().sortedBy { it.startMillis }
                val dateMillis = dayEntries.firstOrNull()?.dateMillis ?: 0L
                Text(
                    text = dayLabel(day, dateMillis),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                )
                dayEntries.forEach { e -> EntryRow(e) }
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            }
        } else {
            Text(
                text = "No plan yet — set your window and subjects, then tap Generate.",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
            )
        }

        Spacer(modifier = Modifier.height(30.dp))
    }

    // Shown when the plan could not be built (e.g. not enough time in the day).
    if (alertMessage != null) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissTimeTableAlert() },
            title = { Text("Time table not generated", fontWeight = FontWeight.Bold) },
            text = { Text(alertMessage) },
            confirmButton = {
                TextButton(onClick = { viewModel.dismissTimeTableAlert() }) { Text("OK") }
            }
        )
    }
}

@Composable
private fun WindowRow(label: String, minutes: Int, onPick: (Int) -> Unit) {
    val context = LocalContext.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 15.sp)
        Text(
            text = formatMinutes(minutes),
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .clickable { pickTime(context, minutes, onPick) }
                .padding(horizontal = 10.dp, vertical = 6.dp)
        )
    }
}

@Composable
private fun SubjectPlanCard(
    courseName: String,
    subjectName: String,
    cfg: SubjectPlanSettings,
    onDuration: (Int) -> Unit,
    onRest: (Int) -> Unit,
    onSessions: (Int) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = "$courseName › $subjectName",
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Duration stepper
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Class duration", fontSize = 13.sp, modifier = Modifier.weight(1f))
                IconButton(onClick = { onDuration((cfg.durationMinutes - 5).coerceAtLeast(5)) }, modifier = Modifier.size(30.dp)) {
                    Icon(Icons.Default.Remove, contentDescription = "Less", modifier = Modifier.size(16.dp))
                }
                Text(
                    text = "${cfg.durationMinutes} min",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.width(64.dp)
                )
                IconButton(onClick = { onDuration((cfg.durationMinutes + 5).coerceAtMost(300)) }, modifier = Modifier.size(30.dp)) {
                    Icon(Icons.Default.Add, contentDescription = "More", modifier = Modifier.size(16.dp))
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Sessions per day: single / double / triple
            Text("Sessions per day", fontSize = 13.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                SESSION_OPTIONS.forEach { (count, label) ->
                    val selected = cfg.sessionsPerDay == count
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                if (selected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surface
                            )
                            .clickable { onSessions(count) }
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                            .testTag("subject_sessions_${subjectName}_$count")
                    ) {
                        Text(
                            text = label,
                            fontSize = 12.sp,
                            color = if (selected) MaterialTheme.colorScheme.onPrimary
                            else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text("Rest after each session", fontSize = 13.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                REST_OPTIONS.forEach { opt ->
                    val selected = cfg.restMinutes == opt
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                if (selected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surface
                            )
                            .clickable { onRest(opt) }
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = "$opt",
                            fontSize = 12.sp,
                            color = if (selected) MaterialTheme.colorScheme.onPrimary
                            else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EntryRow(e: TimeTableEntry) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "${formatMillisTime(e.startMillis)} – ${formatMillisTime(e.endMillis)}",
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.85f),
            modifier = Modifier.width(120.dp)
        )
        Text(
            text = "${e.subjectName} · ${e.className}",
            fontSize = 13.sp,
            modifier = Modifier.weight(1f)
        )
        if (e.isFree) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text("free", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

private fun dayLabel(day: Int, dateMillis: Long): String = when (day) {
    0 -> "Today"
    1 -> "Tomorrow"
    else -> SimpleDateFormat("EEE, dd MMM", Locale.getDefault()).format(Date(dateMillis))
}

private fun formatMillisTime(millis: Long): String =
    SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(millis))

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
