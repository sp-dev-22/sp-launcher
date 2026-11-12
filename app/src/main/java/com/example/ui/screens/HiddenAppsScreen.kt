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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AppInfo
import com.example.ui.HiddenAppsMode
import com.example.ui.LauncherViewModel
import com.example.ui.components.RealAppIcon

/**
 * The vault, reached from the search screen's menu (Hidden Apps, PIN first).
 *
 * Two tabs switch [HiddenAppsMode]:
 * - [HiddenAppsMode.MANAGE]: tick an app to hide it, untick to bring it back,
 *   and change the PIN from here.
 * - [HiddenAppsMode.LAUNCH]: tap a hidden app to start it, without unhiding it first.
 * Closing the screen resets the tab to Manage.
 */
@Composable
fun HiddenAppsScreen(
    viewModel: LauncherViewModel,
    onBack: () -> Unit
) {
    val apps by viewModel.installedApps.collectAsState()
    val hidden by viewModel.hiddenApps.collectAsState()
    val hiddenAppInfos by viewModel.hiddenAppInfos.collectAsState()
    val mode by viewModel.hiddenAppsMode.collectAsState()
    var showChangePin by remember { mutableStateOf(false) }
    var pinMessage by remember { mutableStateOf<String?>(null) }

    val isLaunchMode = mode == HiddenAppsMode.LAUNCH

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
            TextButton(
                onClick = {
                    pinMessage = null
                    showChangePin = true
                },
                modifier = Modifier.testTag("change_pin_btn")
            ) {
                Text("Change PIN")
            }
        }

        TabRow(
            selectedTabIndex = if (isLaunchMode) 1 else 0,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
                .testTag("hidden_apps_tabs")
        ) {
            Tab(
                selected = !isLaunchMode,
                onClick = { viewModel.setHiddenAppsMode(HiddenAppsMode.MANAGE) },
                text = { Text("Manage") },
                modifier = Modifier.testTag("hidden_apps_tab_manage")
            )
            Tab(
                selected = isLaunchMode,
                onClick = { viewModel.setHiddenAppsMode(HiddenAppsMode.LAUNCH) },
                text = { Text("Launch") },
                modifier = Modifier.testTag("hidden_apps_tab_launch")
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = if (isLaunchMode) {
                "Tap an app to open it. It stays hidden from the drawer."
            } else {
                "Tick an app to hide it from the drawer; untick to show it again."
            },
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.65f),
            modifier = Modifier.padding(horizontal = 18.dp)
        )

        pinMessage?.let { message ->
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = message,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 18.dp)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (isLaunchMode) {
            if (hiddenAppInfos.isEmpty()) {
                Text(
                    text = "No hidden apps yet.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp)
                )
            }
            LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp)) {
                items(hiddenAppInfos, key = { it.packageName }) { app ->
                    HiddenLaunchRow(app = app, onLaunch = { viewModel.launchApp(app) })
                }
            }
        } else {
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
    }

    if (showChangePin) {
        ChangePinDialog(
            currentPin = viewModel.vaultPin.value,
            onSave = { newPin ->
                viewModel.setVaultPin(newPin)
                pinMessage = "PIN changed to $newPin."
                showChangePin = false
            },
            onDismiss = { showChangePin = false }
        )
    }
}

@Composable
private fun HiddenLaunchRow(app: AppInfo, onLaunch: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onLaunch() }
            .padding(horizontal = 8.dp, vertical = 10.dp)
            .testTag("launch_hidden_${app.packageName}"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RealAppIcon(app = app, size = 34.dp, isDarkTheme = false)
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = app.label,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = app.packageName,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f)
            )
        }
    }
}

/**
 * Change PIN.
 *
 * The old dialog asked for an hour and a minute, which is why a PIN with a leading zero (or a
 * one-digit minute) could be saved but never typed back in. The PIN is now a plain four-digit
 * string, checked against the current one before it is replaced.
 */
@Composable
private fun ChangePinDialog(
    currentPin: String,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var current by remember { mutableStateOf("") }
    var newPin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Change PIN", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text(
                    "Four digits. Hidden Apps opens from the search screen menu.",
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = current,
                    onValueChange = { current = it.filter { c -> c.isDigit() }.take(4) },
                    label = { Text("Current PIN") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("pin_current_input")
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = newPin,
                    onValueChange = { newPin = it.filter { c -> c.isDigit() }.take(4) },
                    label = { Text("New PIN") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("pin_new_input")
                )
                error?.let {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(it, fontSize = 12.sp, color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    error = when {
                        current != currentPin -> "Current PIN is wrong."
                        newPin.length != 4 -> "The new PIN must be four digits."
                        else -> {
                            onSave(newPin)
                            null
                        }
                    }
                },
                modifier = Modifier.testTag("pin_save_btn")
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
