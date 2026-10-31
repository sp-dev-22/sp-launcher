package com.example.ui.components

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AlertItem
import com.example.data.model.RepeatRule
import com.example.data.model.TaskItem
import com.example.data.model.TaskPriority
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WidgetsManagementSheet(
    showTime: Boolean,
    showDate: Boolean,
    showAlerts: Boolean,
    showTasks: Boolean,
    showStudyDue: Boolean,
    showGoals: Boolean,
    alerts: List<AlertItem>,
    tasks: List<TaskItem>,
    onToggleShowTime: (Boolean) -> Unit,
    onToggleShowDate: (Boolean) -> Unit,
    onToggleShowAlerts: (Boolean) -> Unit,
    onToggleShowTasks: (Boolean) -> Unit,
    onToggleShowStudyDue: (Boolean) -> Unit,
    onToggleShowGoals: (Boolean) -> Unit,
    onOpenGoals: () -> Unit,
    onAddAlert: (AlertItem) -> Unit,
    onEditAlert: (AlertItem) -> Unit,
    onRemoveAlert: (String) -> Unit,
    onMoveAlert: (String, Int) -> Unit,
    onPinAlert: (String, Boolean) -> Unit,
    onArchiveAlert: (String, Boolean) -> Unit,
    onAddTask: (String) -> Unit,
    onRemoveTask: (String) -> Unit,
    onEditTaskItem: (TaskItem) -> Unit,
    onMoveTask: (String, Int) -> Unit,
    undoMessage: String?,
    onUndo: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current
    val stepPx = with(LocalDensity.current) { 56.dp.toPx() }

    var newAlertTitle by remember { mutableStateOf("") }
    var newAlertContent by remember { mutableStateOf("") }
    var newAlertTag by remember { mutableStateOf("") }
    var newAlertReminder by remember { mutableStateOf<Long?>(null) }
    var newAlertRepeat by remember { mutableStateOf(RepeatRule.NONE) }

    var editingAlert by remember { mutableStateOf<AlertItem?>(null) }
    var editingTask by remember { mutableStateOf<TaskItem?>(null) }

    var newTaskInput by remember { mutableStateOf("") }

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
        Box(modifier = Modifier.fillMaxWidth()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .testTag("widgets_management_sheet")
        ) {
            item {
                Text(
                    text = "Widgets Configuration",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                Text(
                    text = "WIDGETS VISIBILITY TOGGLES",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )

                Spacer(modifier = Modifier.height(8.dp))

                ToggleRow("Show Clock (Time)", showTime, onToggleShowTime)
                ToggleRow("Show Date", showDate, onToggleShowDate)
                ToggleRow("Show Alerts Widget", showAlerts, onToggleShowAlerts)
                ToggleRow("Show Tasks Checklist", showTasks, onToggleShowTasks)
                ToggleRow("Show Study Manager Status", showStudyDue, onToggleShowStudyDue)
                ToggleRow("Show Goals Widget", showGoals, onToggleShowGoals)

                TextButton(onClick = onOpenGoals) {
                    Text(text = "Manage goals", fontSize = 13.sp)
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

                Text(
                    text = "MANAGE ALERTS (TAP TO EDIT)",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                Spacer(modifier = Modifier.height(10.dp))
            }

            // Existing alerts — tap a card to edit it
            items(alerts.filter { !it.archived }, key = { it.id }) { alert ->
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
            val archivedAlerts = alerts.filter { it.archived }
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

            // Add new alert form
            item {
                Spacer(modifier = Modifier.height(10.dp))
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
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add")
                        }
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

                Text(
                    text = "TASKS MANAGEMENT",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                Spacer(modifier = Modifier.height(10.dp))
            }

            // Existing tasks
            items(tasks, key = { it.id }) { task ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val taskDrag = rememberDragStep(stepPx) { dir -> onMoveTask(task.id, dir) }
                    Icon(
                        imageVector = Icons.Default.Menu,
                        contentDescription = "Long-press to drag",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                        modifier = Modifier
                            .size(20.dp)
                            .then(taskDrag.modifier)
                            .testTag("task_drag_${task.id}")
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = if (task.isCompleted) "✓ ${task.text}" else "○ ${task.text}",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { editingTask = task }
                            .testTag("task_row_${task.id}")
                    )

                    IconButton(
                        onClick = { editingTask = task },
                        modifier = Modifier
                            .size(34.dp)
                            .testTag("task_edit_${task.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Task",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(onClick = { onRemoveTask(task.id) }) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete Task",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Add new task row
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = newTaskInput,
                        onValueChange = { newTaskInput = it },
                        placeholder = { Text("Add task item...") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (newTaskInput.isNotBlank()) {
                                onAddTask(newTaskInput.trim())
                                newTaskInput = ""
                            }
                        }
                    ) {
                        Text("Add")
                    }
                }

                Spacer(modifier = Modifier.height(40.dp))
            }
            }

            UndoBar(
                message = undoMessage,
                onUndo = onUndo,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
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

    // Edit an existing task
    editingTask?.let { original ->
        TaskEditDialog(
            original = original,
            onSave = { updated ->
                onEditTaskItem(updated)
                editingTask = null
            },
            onDismiss = { editingTask = null }
        )
    }
}

@Composable
private fun TaskEditDialog(
    original: TaskItem,
    onSave: (TaskItem) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var text by remember { mutableStateOf(original.text) }
    var due by remember { mutableStateOf(original.dueMillis) }
    var reminder by remember { mutableStateOf(original.reminderMillis) }
    var repeat by remember { mutableStateOf(original.repeat) }
    var priority by remember { mutableStateOf(original.priority) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Task", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("Task") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text("Priority", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    ChoiceChip("None", priority == TaskPriority.NONE) { priority = TaskPriority.NONE }
                    ChoiceChip("Low", priority == TaskPriority.LOW) { priority = TaskPriority.LOW }
                    ChoiceChip("Med", priority == TaskPriority.MEDIUM) { priority = TaskPriority.MEDIUM }
                    ChoiceChip("High", priority == TaskPriority.HIGH) { priority = TaskPriority.HIGH }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text("Due date", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                Text(
                    text = due?.let { formatDateTime(it) } ?: "Not set",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                Row {
                    TextButton(onClick = { showDateTimePicker(context, due) { due = it } }) {
                        Text("Set due date")
                    }
                    if (due != null) {
                        TextButton(onClick = { due = null }) { Text("Clear") }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text("Reminder (notification)", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                Text(
                    text = reminder?.let { formatDateTime(it) } ?: "Not set",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                Row {
                    TextButton(onClick = { showDateTimePicker(context, reminder) { reminder = it } }) {
                        Text("Set reminder")
                    }
                    if (reminder != null) {
                        TextButton(onClick = { reminder = null }) { Text("Clear") }
                    }
                }

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
                if (text.isNotBlank()) {
                    onSave(
                        original.copy(
                            text = text.trim(),
                            dueMillis = due,
                            reminderMillis = reminder,
                            repeat = if (reminder != null) repeat else RepeatRule.NONE,
                            priority = priority
                        )
                    )
                }
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun ChoiceChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(
                if (selected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.surfaceVariant
            )
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = if (selected) MaterialTheme.colorScheme.onPrimary
            else MaterialTheme.colorScheme.onSurfaceVariant
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

@Composable
private fun ToggleRow(label: String, checked: Boolean, onToggle: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        Switch(checked = checked, onCheckedChange = onToggle)
    }
}

private fun formatDateTime(millis: Long): String =
    SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(millis))

private fun showDateTimePicker(context: Context, initial: Long?, onPicked: (Long) -> Unit) {
    val base = Calendar.getInstance().apply { if (initial != null) timeInMillis = initial }
    DatePickerDialog(
        context,
        { _, year, month, day ->
            val picked = Calendar.getInstance().apply {
                timeInMillis = base.timeInMillis
                set(Calendar.YEAR, year)
                set(Calendar.MONTH, month)
                set(Calendar.DAY_OF_MONTH, day)
            }
            TimePickerDialog(
                context,
                { _, hour, minute ->
                    picked.set(Calendar.HOUR_OF_DAY, hour)
                    picked.set(Calendar.MINUTE, minute)
                    picked.set(Calendar.SECOND, 0)
                    picked.set(Calendar.MILLISECOND, 0)
                    onPicked(picked.timeInMillis)
                },
                base.get(Calendar.HOUR_OF_DAY),
                base.get(Calendar.MINUTE),
                false
            ).show()
        },
        base.get(Calendar.YEAR),
        base.get(Calendar.MONTH),
        base.get(Calendar.DAY_OF_MONTH)
    ).show()
}
