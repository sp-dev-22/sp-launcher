package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.GoalItem
import com.example.data.model.GoalType
import com.example.util.GoalProgress

private val DONE_COLOR = Color(0xFF4CAF50)

/**
 * Create, edit and delete goals. The Home widget is only a window onto this list.
 */
@Composable
fun GoalsScreen(
    goals: List<GoalItem>,
    onAdd: (GoalItem) -> Unit,
    onUpdate: (GoalItem) -> Unit,
    onDelete: (String) -> Unit,
    onReset: (String) -> Unit,
    onBack: () -> Unit
) {
    var creating by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<GoalItem?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp)
            .testTag("goals_screen")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack, modifier = Modifier.testTag("goals_back_btn")) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
            }
            Text(
                text = "Goals",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
            )
        }

        Text(
            text = "Track a regular activity. Tick it from the Home widget, every day if you like.",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
        )

        Spacer(modifier = Modifier.height(14.dp))

        Button(
            onClick = { creating = true },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("goals_add_btn"),
            shape = RoundedCornerShape(14.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = "New goal", fontSize = 14.sp)
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (goals.isEmpty()) {
            Text(
                text = "No goals yet. Create one to put it on your Home screen.",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f)
            )
        } else {
            goals.forEach { goal ->
                GoalCard(
                    goal = goal,
                    onEdit = { editing = goal },
                    onDelete = { onDelete(goal.id) },
                    onReset = { onReset(goal.id) }
                )
                Spacer(modifier = Modifier.height(10.dp))
            }
        }

        Spacer(modifier = Modifier.height(30.dp))
    }

    if (creating) {
        GoalEditorDialog(
            initial = null,
            onSave = { goal ->
                onAdd(goal)
                creating = false
            },
            onDismiss = { creating = false }
        )
    }

    editing?.let { goal ->
        GoalEditorDialog(
            initial = goal,
            onSave = { updated ->
                onUpdate(updated)
                editing = null
            },
            onDismiss = { editing = null }
        )
    }
}

@Composable
private fun GoalCard(
    goal: GoalItem,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onReset: () -> Unit
) {
    val done = GoalProgress.isComplete(goal)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("goal_card_${goal.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        )
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = goal.title,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (goal.type == GoalType.LIMITED) "Limited activity" else "Unlimited activity",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                    )
                }
                IconButton(onClick = onReset) {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = "Reset progress",
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
                IconButton(onClick = onEdit) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Edit goal",
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete goal",
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = valueLine(goal),
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )

            if (goal.type == GoalType.LIMITED) {
                Spacer(modifier = Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { GoalProgress.fraction(goal) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(5.dp),
                    color = if (done) DONE_COLOR else MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = buildString {
                    append(if (done) goal.endText else goal.startText)
                    append("  |  button: ")
                    append(GoalProgress.buttonLabel(goal))
                    if (goal.resetDaily) append("  |  resets daily")
                    append("  |  total ")
                    append(GoalProgress.formatValue(goal.totalValue))
                },
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

private fun valueLine(goal: GoalItem): String {
    val value = GoalProgress.formatValue(goal.currentValue)
    return if (goal.type == GoalType.LIMITED) {
        "$value of ${GoalProgress.formatValue(goal.targetValue)}" + if (goal.resetDaily) " today" else ""
    } else {
        "$value logged" + if (goal.resetDaily) " today" else ""
    }
}

/**
 * One dialog for both "new goal" and "edit goal". [initial] null means new.
 *
 * LIMITED goals ask for an end text and a target; UNLIMITED goals hide both, because there is
 * no finish line to describe.
 */
@Composable
private fun GoalEditorDialog(
    initial: GoalItem?,
    onSave: (GoalItem) -> Unit,
    onDismiss: () -> Unit
) {
    var title by remember { mutableStateOf(initial?.title ?: "") }
    var type by remember { mutableStateOf(initial?.type ?: GoalType.LIMITED) }
    var startText by remember { mutableStateOf(initial?.startText ?: "Beginner") }
    var endText by remember { mutableStateOf(initial?.endText ?: "Pro") }
    var stepValue by remember { mutableStateOf(GoalProgress.formatValue(initial?.stepValue ?: 1.0)) }
    var stepLabel by remember { mutableStateOf(initial?.stepLabel ?: "") }
    var targetValue by remember { mutableStateOf(GoalProgress.formatValue(initial?.targetValue ?: 100.0)) }
    var resetDaily by remember { mutableStateOf(initial?.resetDaily ?: false) }

    val step = stepValue.trim().toDoubleOrNull()
    val target = targetValue.trim().toDoubleOrNull()
    val limited = type == GoalType.LIMITED

    val error = when {
        title.isBlank() -> "Give the goal a name."
        step == null || step <= 0.0 -> "Per-click value must be more than 0."
        limited && (target == null || target <= 0.0) -> "Target must be more than 0."
        else -> null
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (initial == null) "New goal" else "Edit goal",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Goal name") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("goal_field_title")
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Goal type",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = limited,
                        onClick = { type = GoalType.LIMITED },
                        label = { Text("Limited", fontSize = 12.sp) }
                    )
                    FilterChip(
                        selected = !limited,
                        onClick = { type = GoalType.UNLIMITED },
                        label = { Text("Unlimited", fontSize = 12.sp) }
                    )
                }
                Text(
                    text = if (limited) {
                        "Has a target, e.g. 2 litres of water a day."
                    } else {
                        "No target - it just keeps counting."
                    },
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = startText,
                    onValueChange = { startText = it },
                    label = { Text("Starting text") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                if (limited) {
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = endText,
                        onValueChange = { endText = it },
                        label = { Text("End text (when the target is reached)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = stepValue,
                        onValueChange = { stepValue = it },
                        label = { Text("Per click") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f)
                    )
                    if (limited) {
                        OutlinedTextField(
                            value = targetValue,
                            onValueChange = { targetValue = it },
                            label = { Text("Target") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = stepLabel,
                    onValueChange = { stepLabel = it },
                    label = { Text("Button text (optional)") },
                    placeholder = { Text("e.g. 100 Liter") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "Reset every day", fontSize = 13.sp)
                        Text(
                            text = "Progress starts again each morning; the total is kept.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                        )
                    }
                    Switch(
                        checked = resetDaily,
                        onCheckedChange = { resetDaily = it },
                        modifier = Modifier.testTag("goal_field_reset")
                    )
                }

                if (error != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(text = error, fontSize = 11.sp, color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val base = initial ?: GoalItem(title = title.trim())
                    onSave(
                        base.copy(
                            title = title.trim(),
                            type = type,
                            startText = startText.trim().ifBlank { "Beginner" },
                            endText = endText.trim().ifBlank { "Pro" },
                            stepValue = step ?: 1.0,
                            stepLabel = stepLabel.trim(),
                            targetValue = if (limited) (target ?: 100.0) else 0.0,
                            resetDaily = resetDaily
                        )
                    )
                },
                enabled = error == null,
                modifier = Modifier.testTag("goal_save_btn")
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
