package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AlertItem
import com.example.data.model.RepeatRule
import com.example.ui.components.ChoiceChip
import com.example.ui.components.ExactAlarmHint
import com.example.ui.components.formatDateTime
import com.example.ui.components.rememberDragStep
import com.example.ui.components.showDateTimePicker

/**
 * Alerts, as a proper screen (v1.16.0): the full list, the add form, the edit dialog
 * and the archived section — all moved out of the Widgets sheet unchanged. Same data,
 * same ViewModel functions, same reminders; only the home changed.
 */
@Composable
fun AlertsScreen(
    alerts: List<AlertItem>,
    onAddAlert: (AlertItem) -> Unit,
    onEditAlert: (AlertItem) -> Unit,
    onRemoveAlert: (String) -> Unit,
    onMoveAlert: (String, Int) -> Unit,
    onPinAlert: (String, Boolean) -> Unit,
    onArchiveAlert: (String, Boolean) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val stepPx = with(LocalDensity.current) { 56.dp.toPx() }

    var newAlertTitle by remember { mutableStateOf("") }
    var newAlertContent by remember { mutableStateOf("") }
    var newAlertTag by remember { mutableStateOf("") }
    var newAlertReminder by remember { mutableStateOf<Long?>(null) }
    var newAlertRepeat by remember { mutableStateOf(RepeatRule.NONE) }

    var editingAlert by remember { mutableStateOf<AlertItem?>(null) }

    val activeAlerts = alerts.filter { !it.archived }
    val archivedAlerts = alerts.filter { it.archived }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            // The background was missing here (v1.16.0 bug 1): the screen was
            // transparent and the Home/Drawer showed through underneath.
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 18.dp)
            .testTag("alerts_screen")
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack, modifier = Modifier.testTag("alerts_back_btn")) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
                Text(
                    text = "Alerts",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
                )
            }

            Text(
                text = "Reminders with optional notifications. Pin the important ones; archive the done ones.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Add new alert form
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("Add New Alert", fontWeight = FontWeight.Medium, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = newAlertTitle,
                        onValueChange = { newAlertTitle = it },
                        label = { Text("Title") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = newAlertContent,
                        onValueChange = { newAlertContent = it },
                        label = { Text("Details") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = newAlertTag,
                        onValueChange = { newAlertTag = it },
                        label = { Text("Tag (optional, custom)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text("Reminder (notification)", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    Text(
                        text = newAlertReminder?.let { formatDateTime(it) } ?: "Not set",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                    Row {
                        TextButton(onClick = { showDateTimePicker(context, newAlertReminder) { newAlertReminder = it } }) {
                            Text("Set date & time")
                        }
                        if (newAlertReminder != null) {
                            TextButton(onClick = { newAlertReminder = null }) { Text("Clear") }
                        }
                    }
                    ExactAlarmHint(reminderSet = newAlertReminder != null)
                    if (newAlertReminder != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Repeat", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            ChoiceChip("Once", newAlertRepeat == RepeatRule.NONE) { newAlertRepeat = RepeatRule.NONE }
                            ChoiceChip("Daily", newAlertRepeat == RepeatRule.DAILY) { newAlertRepeat = RepeatRule.DAILY }
                            ChoiceChip("Weekly", newAlertRepeat == RepeatRule.WEEKLY) { newAlertRepeat = RepeatRule.WEEKLY }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = {
                            if (newAlertTitle.isNotBlank()) {
                                onAddAlert(
                                    AlertItem(
                                        title = newAlertTitle.trim(),
                                        content = newAlertContent.trim(),
                                        category = newAlertTag.trim(),
                                        reminderMillis = newAlertReminder,
                                        repeat = if (newAlertReminder != null) newAlertRepeat else RepeatRule.NONE
                                    )
                                )
                                newAlertTitle = ""
                                newAlertContent = ""
                                newAlertTag = ""
                                newAlertReminder = null
                                newAlertRepeat = RepeatRule.NONE
                            }
                        },
                        modifier = Modifier
                            .align(Alignment.End)
                            .testTag("alerts_add_btn")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add")
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "ALERTS (TAP TO EDIT)",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            Spacer(modifier = Modifier.height(10.dp))
        }

        if (activeAlerts.isEmpty()) {
            item {
                Text(
                    text = "No alerts yet. Add one above to put it on your Home screen.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f)
                )
            }
        }

        // Existing alerts — tap a card to edit it
        items(activeAlerts, key = { it.id }) { alert ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clickable { editingAlert = alert }
                    .testTag("alert_row_${alert.id}"),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val alertDrag = rememberDragStep(stepPx) { dir -> onMoveAlert(alert.id, dir) }
                    Icon(
                        imageVector = Icons.Default.Menu,
                        contentDescription = "Long-press to drag",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                        modifier = Modifier
                            .size(20.dp)
                            .then(alertDrag.modifier)
                            .testTag("alert_drag_${alert.id}")
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (alert.category.isNotBlank()) {
                                Text(
                                    text = "[${alert.category.uppercase()}]",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                            }
                            Text(
                                text = alert.title,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                            )
                        }
                        Text(
                            text = alert.content,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                        )
                        alert.reminderMillis?.let { millis ->
                            Row(
                                modifier = Modifier.padding(top = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.NotificationsActive,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = formatDateTime(millis),
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    IconButton(
                        onClick = { onPinAlert(alert.id, !alert.pinned) },
                        modifier = Modifier.size(34.dp).testTag("alert_pin_${alert.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.PushPin,
                            contentDescription = if (alert.pinned) "Unpin" else "Pin",
                            tint = if (alert.pinned) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(
                        onClick = { onArchiveAlert(alert.id, true) },
                        modifier = Modifier.size(34.dp).testTag("alert_archive_${alert.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Archive,
                            contentDescription = "Archive",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit Alert",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    IconButton(onClick = { onRemoveAlert(alert.id) }) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete Alert",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        // Archived notes — kept, just out of the way
        if (archivedAlerts.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "ARCHIVED (${archivedAlerts.size})",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
            items(archivedAlerts, key = { "arch-" + it.id }) { alert ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = alert.title,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
                        )
                        if (alert.content.isNotBlank()) {
                            Text(
                                text = alert.content,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                maxLines = 1
                            )
                        }
                    }
                    IconButton(
                        onClick = { onArchiveAlert(alert.id, false) },
                        modifier = Modifier.size(34.dp).testTag("alert_unarchive_${alert.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Unarchive,
                            contentDescription = "Unarchive",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(30.dp))
        }
    }

    // Edit an existing alert
    editingAlert?.let { original ->
        AlertEditDialog(
            original = original,
            onSave = { updated ->
                onEditAlert(updated)
                editingAlert = null
            },
            onDismiss = { editingAlert = null }
        )
    }
}

@Composable
private fun AlertEditDialog(
    original: AlertItem,
    onSave: (AlertItem) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var title by remember { mutableStateOf(original.title) }
    var content by remember { mutableStateOf(original.content) }
    var tag by remember { mutableStateOf(original.category) }
    var reminder by remember { mutableStateOf(original.reminderMillis) }
    var repeat by remember { mutableStateOf(original.repeat) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Alert", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("Details") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = tag,
                    onValueChange = { tag = it },
                    label = { Text("Tag (optional, custom)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text("Reminder (notification)", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                Text(
                    text = reminder?.let { formatDateTime(it) } ?: "Not set",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                Row {
                    TextButton(onClick = { showDateTimePicker(context, reminder) { reminder = it } }) {
                        Text("Set date & time")
                    }
                    if (reminder != null) {
                        TextButton(onClick = { reminder = null }) { Text("Clear") }
                    }
                }
                ExactAlarmHint(reminderSet = reminder != null)
                if (reminder != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Repeat", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        ChoiceChip("Once", repeat == RepeatRule.NONE) { repeat = RepeatRule.NONE }
                        ChoiceChip("Daily", repeat == RepeatRule.DAILY) { repeat = RepeatRule.DAILY }
                        ChoiceChip("Weekly", repeat == RepeatRule.WEEKLY) { repeat = RepeatRule.WEEKLY }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (title.isNotBlank()) {
                    onSave(
                        original.copy(
                            title = title.trim(),
                            content = content.trim(),
                            category = tag.trim(),
                            reminderMillis = reminder,
                            repeat = if (reminder != null) repeat else RepeatRule.NONE
                        )
                    )
                }
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
