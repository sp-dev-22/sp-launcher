package com.example.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.GoalItem
import com.example.data.model.GoalType
import com.example.util.GoalProgress

private val DONE_COLOR = Color(0xFF4CAF50)

/**
 * The Home-screen goals card: every goal on one line, with its own tap button, so the user can
 * tick an activity without opening anything. Tapping the card itself opens the Goals screen.
 *
 * Kept deliberately small — Home already stacks the clock, the study card, alerts and the task
 * checklist — so only [MAX_ROWS] goals are listed and the rest collapse into a "+N more" line.
 */
@Composable
fun GoalsWidget(
    goals: List<GoalItem>,
    isDarkTheme: Boolean,
    onTick: (String) -> Unit,
    onOpenGoals: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (goals.isEmpty()) return
    val shown = goals.take(MAX_ROWS)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable { onOpenGoals() }
            .testTag("goals_widget"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (isDarkTheme) 0.35f else 0.5f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Goals",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = if (goals.size == 1) "1 goal" else "${goals.size} goals",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            shown.forEach { goal ->
                GoalRow(goal = goal, onTick = onTick)
                Spacer(modifier = Modifier.height(8.dp))
            }

            if (goals.size > MAX_ROWS) {
                Text(
                    text = "+${goals.size - MAX_ROWS} more",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                )
            }
        }
    }
}

@Composable
private fun GoalRow(goal: GoalItem, onTick: (String) -> Unit) {
    val done = GoalProgress.isComplete(goal)

    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = goal.title,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(3.dp))

            // One line, the user's own format: "<value> <start text> / <target> <end text>",
            // e.g. "1 दिन / 15 दिन". No arrow glyph: the phone's XOS font does not have it
            // and renders garbage instead (seen on device, v1.11.3).
            Text(
                text = if (goal.type == GoalType.LIMITED) {
                    "${valueWithText(goal, goal.currentValue, goal.startText)} / " +
                        valueWithText(goal, goal.targetValue, goal.endText)
                } else {
                    valueWithText(goal, goal.currentValue, goal.startText)
                },
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = if (done) DONE_COLOR else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
            )

            if (goal.type == GoalType.LIMITED) {
                Spacer(modifier = Modifier.height(5.dp))
                LinearProgressIndicator(
                    progress = { GoalProgress.fraction(goal) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp),
                    color = if (done) DONE_COLOR else MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                )
            }
        }

        Spacer(modifier = Modifier.width(10.dp))

        Button(
            onClick = { onTick(goal.id) },
            enabled = !done,
            shape = RoundedCornerShape(10.dp),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
            modifier = Modifier
                .height(32.dp)
                .testTag("goal_tick_${goal.id}"),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (done) DONE_COLOR.copy(alpha = 0.25f) else MaterialTheme.colorScheme.primary
            )
        ) {
            Text(
                text = if (done) "Done" else GoalProgress.buttonLabel(goal),
                fontSize = 11.sp,
                maxLines = 1
            )
        }
    }
}

/**
 * The unit a goal counts in, taken from the tap button's own label: "100 Liter" -> "Liter",
 * "1 Pushup" -> "Pushup". Falls back to the plain number when no label was given.
 */
private fun unitOf(goal: GoalItem): String =
    goal.stepLabel.filterNot { it.isDigit() }.replace("/", " ").trim()

/**
 * "1 दिन", "100 मीटर" — a value plus the text that belongs to its side of the goal: the
 * start text goes with the current value, the end text with the target. Falls back to
 * the tap button's unit when that side's text was left blank, so older goals still
 * show a unit.
 */
private fun valueWithText(goal: GoalItem, value: Double, text: String): String {
    val label = text.ifBlank { unitOf(goal) }
    val number = GoalProgress.formatValue(value)
    return if (label.isBlank()) number else "$number $label"
}

private const val MAX_ROWS = 3
