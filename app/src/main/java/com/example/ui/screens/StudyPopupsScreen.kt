package com.example.ui.screens

import android.app.TimePickerDialog
import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.LauncherViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

private val INTERVAL_OPTIONS = listOf(5, 10, 15, 20, 30)

@Composable
fun StudyPopupsScreen(
    viewModel: LauncherViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val settings by viewModel.popupSettings.collectAsState()
    val cards by viewModel.flashcardBank.collectAsState()
    val importCount by viewModel.cardImportCount.collectAsState()

    var confirmClear by remember { mutableStateOf(false) }

    val filePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (!uris.isNullOrEmpty()) viewModel.importCardFiles(uris)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp)
            .testTag("study_popups_screen")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack, modifier = Modifier.testTag("popups_back_btn")) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
            }
            Icon(
                imageVector = Icons.Default.NotificationsActive,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Study Popups",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
            )
        }

        // ---- master switch ----
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Study Popups", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                    Text(
                        "Study cards through the day",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.65f)
                    )
                }
                Switch(
                    checked = settings.enabled,
                    onCheckedChange = { viewModel.setPopupEnabled(it) },
                    modifier = Modifier.testTag("popups_enabled_switch")
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // ---- schedule ----
        Text("SCHEDULE", fontWeight = FontWeight.Bold, fontSize = 12.sp, letterSpacing = 1.sp)
        Spacer(modifier = Modifier.height(6.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                TimeRow("Start", settings.startMinutes) { viewModel.setPopupWindow(it, settings.stopMinutes) }
                Spacer(modifier = Modifier.height(6.dp))
                TimeRow("Stop", settings.stopMinutes) { viewModel.setPopupWindow(settings.startMinutes, it) }
                Spacer(modifier = Modifier.height(10.dp))
                Text("One card every", fontSize = 13.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    INTERVAL_OPTIONS.forEach { opt ->
                        val selected = settings.intervalMinutes == opt
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface)
                                .clickable { viewModel.setPopupInterval(opt) }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                "$opt min",
                                fontSize = 12.sp,
                                color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Never shows on the lock screen or a locked phone.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ---- cards ----
        Text(
            "CARDS — ${cards.size} loaded, ${cards.count { it.locked }} mastered",
            fontWeight = FontWeight.Bold, fontSize = 12.sp, letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(6.dp))

        Button(
            onClick = { filePicker.launch(arrayOf("application/json", "text/plain", "*/*")) },
            modifier = Modifier.fillMaxWidth().height(50.dp).testTag("add_card_files_btn")
        ) {
            Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Add card files")
        }

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedButton(
            onClick = { viewModel.setShowCards(true) },
            enabled = cards.isNotEmpty(),
            modifier = Modifier.fillMaxWidth().height(50.dp).testTag("manage_cards_btn")
        ) {
            Text("Manage cards (browse · edit · decks)")
        }

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedButton(
            onClick = { confirmClear = true },
            enabled = cards.isNotEmpty(),
            modifier = Modifier.fillMaxWidth().height(50.dp).testTag("clear_cards_btn")
        ) {
            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Clear all cards")
        }

        importCount?.let {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                if (it > 0) "Added $it cards." else "No cards found in those files.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.primary
            )
        }

        // decks already loaded
        val decks = remember(cards) { cards.groupBy { it.deck }.filterKeys { it.isNotBlank() } }
        if (decks.isNotEmpty()) {
            Spacer(modifier = Modifier.height(10.dp))
            decks.forEach { (deck, list) ->
                Text(
                    "• $deck — ${list.size} cards",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                )
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

        Text("FILE FORMAT", fontWeight = FontWeight.Bold, fontSize = 12.sp, letterSpacing = 1.sp)
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Each file is JSON — either a plain list, or an object with a \"cards\" list. " +
                "A card needs \"front\" and \"back\"; for an MCQ add \"options\" and \"correct\" (1-based) " +
                "instead of \"back\".",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "{ \"deck\": \"RRB GK\", \"cards\": [\n" +
                "  { \"front\": \"Capital of India?\", \"back\": \"New Delhi\" },\n" +
                "  { \"front\": \"Largest planet?\",\n" +
                "    \"options\": [\"Jupiter\",\"Saturn\",\"Earth\",\"Mars\"], \"correct\": 1 }\n" +
                "] }",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
        )

        Spacer(modifier = Modifier.height(30.dp))
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text("Clear all cards?", fontWeight = FontWeight.Bold) },
            text = { Text("This removes every loaded card. Your other data stays.") },
            confirmButton = {
                TextButton(onClick = { viewModel.clearCards(); confirmClear = false }) { Text("Clear") }
            },
            dismissButton = { TextButton(onClick = { confirmClear = false }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun TimeRow(label: String, minutes: Int, onPick: (Int) -> Unit) {
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
