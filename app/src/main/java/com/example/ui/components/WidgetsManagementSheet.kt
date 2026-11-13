package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Widget settings only: which Home widgets show, and what the Date widget opens.
 * Alerts and Tasks used to be managed here too, but they are proper screens now
 * (v1.16.0) — see [com.example.ui.screens.AlertsScreen] and
 * [com.example.ui.screens.TasksScreen].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WidgetsManagementSheet(
    showTime: Boolean,
    showDate: Boolean,
    showAlerts: Boolean,
    showTasks: Boolean,
    showStudyDue: Boolean,
    showGoals: Boolean,
    showNowNextClass: Boolean = true,
    onToggleShowTime: (Boolean) -> Unit,
    onToggleShowDate: (Boolean) -> Unit,
    onToggleShowAlerts: (Boolean) -> Unit,
    onToggleShowTasks: (Boolean) -> Unit,
    onToggleShowStudyDue: (Boolean) -> Unit,
    onToggleShowGoals: (Boolean) -> Unit,
    onToggleShowNowNextClass: (Boolean) -> Unit = {},
    onOpenGoals: () -> Unit,
    onDismiss: () -> Unit,
    // Date widget tap target (v1.15.0): false = in-app Calendar, true = system Calendar.
    dateOpensSystem: Boolean = false,
    onSetDateOpensSystem: (Boolean) -> Unit = {}
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

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
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .testTag("widgets_management_sheet")
        ) {
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
            // Date widget tap target (v1.15.0): the in-app Calendar, or the
            // system Calendar app. Only shown while the Date widget is on —
            // the choice means nothing when the widget is hidden.
            if (showDate) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Date opens",
                    style = MaterialTheme.typography.bodyLarge
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.testTag("date_opens_row")
                ) {
                    ChoiceChip("In-app Calendar", !dateOpensSystem) { onSetDateOpensSystem(false) }
                    ChoiceChip("System Calendar", dateOpensSystem) { onSetDateOpensSystem(true) }
                }
                Spacer(modifier = Modifier.height(4.dp))
            }
            ToggleRow("Show Alerts Widget", showAlerts, onToggleShowAlerts)
            ToggleRow("Show Tasks Widget", showTasks, onToggleShowTasks)
            ToggleRow("Show Study Manager Status", showStudyDue, onToggleShowStudyDue)
            ToggleRow("Show Goals Widget", showGoals, onToggleShowGoals)
            ToggleRow("Show Now/Next Class", showNowNextClass, onToggleShowNowNextClass)

            TextButton(onClick = onOpenGoals) {
                Text(text = "Manage goals", fontSize = 13.sp)
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
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
