package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.FocusSessionRecord
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FocusHistorySheet(
    historyList: List<FocusSessionRecord>,
    totalSessions: Int,
    totalMinutes: Int,
    onClearAll: () -> Unit,
    onClearOlderThan: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showClearDropdown by remember { mutableStateOf(false) }

    // Dialog state for Custom Duration & Custom Date/Time
    var showCustomDurationDialog by remember { mutableStateOf(false) }
    var showCustomDateTimeDialog by remember { mutableStateOf(false) }
    var confirmClearPrompt by remember { mutableStateOf<Pair<String, Long>?>(null) } // description to cutoff timestamp

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 8.dp)
                    .width(42.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .testTag("focus_history_sheet")
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(26.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Focus History",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                if (historyList.isNotEmpty()) {
                    Box {
                        OutlinedButton(
                            onClick = { showClearDropdown = true },
                            modifier = Modifier.height(36.dp)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Clear", fontSize = 12.sp)
                        }

                        DropdownMenu(
                            expanded = showClearDropdown,
                            onDismissRequest = { showClearDropdown = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Clear All History") },
                                onClick = {
                                    showClearDropdown = false
                                    confirmClearPrompt = Pair("All recorded sessions", Long.MAX_VALUE)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Older than 24 Hours") },
                                onClick = {
                                    showClearDropdown = false
                                    val cutoff = System.currentTimeMillis() - (24 * 60 * 60 * 1000L)
                                    confirmClearPrompt = Pair("Sessions older than 24 hours", cutoff)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Older than 7 Days") },
                                onClick = {
                                    showClearDropdown = false
                                    val cutoff = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000L)
                                    confirmClearPrompt = Pair("Sessions older than 7 days", cutoff)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Custom Duration (Days/Hours)...") },
                                onClick = {
                                    showClearDropdown = false
                                    showCustomDurationDialog = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Custom Date & Time...") },
                                onClick = {
                                    showClearDropdown = false
                                    showCustomDateTimeDialog = true
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Total Logged: $totalMinutes mins across $totalSessions sessions",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

            if (historyList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 36.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No focus history yet.\nComplete a Focus session to record your achievements!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(historyList, key = { it.id }) { record ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.HourglassTop,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Column {
                                        Text(
                                            text = "${record.durationMinutes} min ${record.sessionType}",
                                            style = MaterialTheme.typography.bodyLarge.copy(
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 15.sp
                                            ),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "${record.dateFormatted} • ${record.timeFormatted}",
                                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                                        )
                                    }
                                }

                                Text(
                                    text = "+${record.durationMinutes}m",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = Color(0xFF2E7D32)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    // 1. Custom Duration Dialog (Select Days & Hours)
    if (showCustomDurationDialog) {
        var selectedDays by remember { mutableIntStateOf(1) }
        var selectedHours by remember { mutableIntStateOf(0) }

        AlertDialog(
            onDismissRequest = { showCustomDurationDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Schedule, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Clear by Custom Duration")
                }
            },
            text = {
                Column {
                    Text(
                        "Clear all sessions older than:",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text("Days: $selectedDays", fontWeight = FontWeight.SemiBold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(0, 1, 3, 7, 14, 30).forEach { d ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (selectedDays == d) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                                    .clickable { selectedDays = d }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "$d d",
                                    color = if (selectedDays == d) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text("Additional Hours: $selectedHours", fontWeight = FontWeight.SemiBold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(0, 2, 6, 12, 18).forEach { h ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (selectedHours == h) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                                    .clickable { selectedHours = h }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "$h h",
                                    color = if (selectedHours == h) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    val totalMillis = (selectedDays * 24L * 60 * 60 * 1000L) + (selectedHours * 60L * 60 * 1000L)
                    val cutoff = System.currentTimeMillis() - totalMillis
                    val matchingCount = historyList.count { it.timestamp < cutoff }

                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "Will remove $matchingCount session(s)",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFC62828),
                        fontWeight = FontWeight.Medium
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val totalMillis = (selectedDays * 24L * 60 * 60 * 1000L) + (selectedHours * 60L * 60 * 1000L)
                        val cutoff = (System.currentTimeMillis() - totalMillis).coerceAtLeast(0L)
                        showCustomDurationDialog = false
                        confirmClearPrompt = Pair("Sessions older than $selectedDays days & $selectedHours hours", cutoff)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828))
                ) {
                    Text("Proceed to Clear")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCustomDurationDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // 2. Custom Date & Time Dialog (Pick cutoff timestamp directly)
    if (showCustomDateTimeDialog) {
        var daysAgo by remember { mutableIntStateOf(1) }
        var hourOfDay by remember { mutableIntStateOf(12) }

        val cal = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, -daysAgo)
            set(Calendar.HOUR_OF_DAY, hourOfDay)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
        }
        val chosenCutoff = cal.timeInMillis
        val formattedDate = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(chosenCutoff))
        val matchingCount = historyList.count { it.timestamp < chosenCutoff }

        AlertDialog(
            onDismissRequest = { showCustomDateTimeDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Clear Before Date & Time")
                }
            },
            text = {
                Column {
                    Text(
                        "Delete all sessions recorded before:",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = formattedDate,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(12.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text("Pick Days in Past:", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(1, 2, 3, 5, 7, 14, 30).forEach { d ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (daysAgo == d) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                                    .clickable { daysAgo = d }
                                    .padding(horizontal = 8.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "$d d ago",
                                    color = if (daysAgo == d) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text("Cutoff Hour:", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(0, 6, 12, 18, 23).forEach { h ->
                            val label = when (h) {
                                0 -> "12 AM"
                                6 -> "6 AM"
                                12 -> "12 PM"
                                18 -> "6 PM"
                                else -> "11 PM"
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (hourOfDay == h) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                                    .clickable { hourOfDay = h }
                                    .padding(horizontal = 8.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = label,
                                    color = if (hourOfDay == h) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "This will delete $matchingCount record(s)",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFC62828),
                        fontWeight = FontWeight.Medium
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showCustomDateTimeDialog = false
                        confirmClearPrompt = Pair("Sessions recorded prior to $formattedDate", chosenCutoff)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828))
                ) {
                    Text("Proceed to Clear")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCustomDateTimeDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // 3. Final Confirmation Prompt
    confirmClearPrompt?.let { (description, cutoff) ->
        val count = if (cutoff == Long.MAX_VALUE) historyList.size else historyList.count { it.timestamp < cutoff }

        AlertDialog(
            onDismissRequest = { confirmClearPrompt = null },
            title = { Text("Confirm Clear History", fontWeight = FontWeight.Bold) },
            text = {
                Text("Are you sure you want to permanently clear $description?\n\nThis will delete $count session record(s). This action cannot be undone.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (cutoff == Long.MAX_VALUE) {
                            onClearAll()
                        } else {
                            onClearOlderThan(cutoff)
                        }
                        confirmClearPrompt = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828))
                ) {
                    Text("Delete $count Record(s)")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { confirmClearPrompt = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}
