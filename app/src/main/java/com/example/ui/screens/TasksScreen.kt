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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.RepeatRule
import com.example.data.model.TaskItem
import com.example.data.model.TaskPriority
import com.example.ui.components.ChoiceChip
import com.example.ui.components.ExactAlarmHint
import com.example.ui.components.formatDateTime
import com.example.ui.components.rememberDragStep
import com.example.ui.components.showDateTimePicker

/**
 * Tasks, as a proper screen (v1.16.0): the full list, the add row, the edit dialog
 * and the complete toggle — all moved out of the Widgets sheet. Same data, same
 * ViewModel functions, same reminders; only the home changed. The one addition over
 * the sheet is the complete toggle (the circle before each task), which the sheet
 * never had — completion used to need the Home widget.
 */
@Composable
fun TasksScreen(
    tasks: List<TaskItem>,
    onAddTask: (String) -> Unit,
    onRemoveTask: (String) -> Unit,
    onEditTaskItem: (TaskItem) -> Unit,
    onMoveTask: (String, Int) -> Unit,
    onToggleTask: (String) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val stepPx = with(LocalDensity.current) { 56.dp.toPx() }

    var editingTask by remember { mutableStateOf<TaskItem?>(null) }
    var newTaskInput by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            // The background was missing here (v1.16.0 bug 1): the screen was
            // transparent and the Home/Drawer showed through underneath.
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 18.dp)
            .testTag("tasks_screen")
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack, modifier = Modifier.testTag("tasks_back_btn")) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
                Text(
                    text = "Tasks",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
                )
            }

            Text(
                text = "Things to do, with due dates and reminders. Tap the circle to tick one off.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Add new task row
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
                    },
                    modifier = Modifier.testTag("tasks_add_btn")
                ) {
                    Text("Add")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "TASKS (TAP TO EDIT)",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            Spacer(modifier = Modifier.height(10.dp))
        }

        if (tasks.isEmpty()) {
            item {
                Text(
                    text = "No tasks yet. Add one above to put it on your Home screen.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f)
                )
            }
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
                    text = if (task.isCompleted) "✓" else "○",
                    fontSize = 18.sp,
                    color = if (task.isCompleted) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier
                        .clickable { onToggleTask(task.id) }
                        .padding(2.dp)
                        .testTag("task_toggle_${task.id}")
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = task.text,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                    ),
                    color = if (task.isCompleted) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    else MaterialTheme.colorScheme.onSurface,
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

        item {
            Spacer(modifier = Modifier.height(30.dp))
        }
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
