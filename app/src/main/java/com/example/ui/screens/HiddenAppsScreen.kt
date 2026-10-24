package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.LauncherViewModel

/**
 * The hidden-apps selector, reached only through the secret "set time" screen. Tick an app to
 * hide it from the drawer; untick to bring it back.
 */
@Composable
fun HiddenAppsScreen(
    viewModel: LauncherViewModel,
    onBack: () -> Unit
) {
    val apps by viewModel.installedApps.collectAsState()
    val hidden by viewModel.hiddenApps.collectAsState()
    var showChangePin by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("hidden_apps_screen")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp, horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack, modifier = Modifier.testTag("hidden_apps_back_btn")) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
            }
            Text(
                text = "Hidden apps",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = { showChangePin = true }, modifier = Modifier.testTag("change_pin_btn")) {
                Text("Change PIN")
            }
        }

        Text(
            text = "Tick an app to hide it from the drawer; untick to show it again.",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.65f),
            modifier = Modifier.padding(horizontal = 18.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp)) {
            items(apps, key = { it.packageName }) { app ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = app.label,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.weight(1f)
                    )
                    Checkbox(
                        checked = hidden.contains(app.packageName),
                        onCheckedChange = { viewModel.setAppHidden(app.packageName, it) },
                        modifier = Modifier.testTag("hide_${app.packageName}")
                    )
                }
            }
        }
    }

    if (showChangePin) {
        ChangePinDialog(
            onSave = { h, m ->
                viewModel.setVaultPin("%02d%02d".format(h, m))
                showChangePin = false
            },
            onDismiss = { showChangePin = false }
        )
    }
}

@Composable
private fun ChangePinDialog(
    onSave: (Int, Int) -> Unit,
    onDismiss: () -> Unit
) {
    var hour by remember { mutableStateOf("") }
    var minute by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Change PIN", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text(
                    "The PIN is entered as a time on the Set time screen (12:34 = 1234).",
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = hour,
                        onValueChange = { hour = it.filter { c -> c.isDigit() }.take(2) },
                        label = { Text("HH") },
                        singleLine = true,
                        modifier = Modifier.width(90.dp)
                    )
                    Text("  :  ", fontSize = 20.sp)
                    OutlinedTextField(
                        value = minute,
                        onValueChange = { minute = it.filter { c -> c.isDigit() }.take(2) },
                        label = { Text("MM") },
                        singleLine = true,
                        modifier = Modifier.width(90.dp)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val h = hour.toIntOrNull()
                val m = minute.toIntOrNull()
                if (h != null && m != null && h in 0..23 && m in 0..59) onSave(h, m)
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
