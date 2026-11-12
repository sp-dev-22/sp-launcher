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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.LauncherViewModel
import com.example.ui.components.RealAppIcon

/**
 * App Lock management (v1.14.0), from Settings > App Lock.
 *
 * Lists the locked apps — each one can be unlocked from here (the current PIN is
 * asked first) — and offers Change PIN. Locking itself happens from the drawer:
 * long-press an app and tap "Lock app". The PIN is set lazily, the first time an
 * app is locked, and it is stored only as a secure hash.
 */
@Composable
fun AppLockScreen(
    viewModel: LauncherViewModel,
    onBack: () -> Unit
) {
    val apps by viewModel.installedApps.collectAsState()
    val locked by viewModel.lockedApps.collectAsState()
    val hasPin by viewModel.hasAppLockPin.collectAsState()
    val lockedInfos = remember(apps, locked) { apps.filter { it.packageName in locked } }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("app_lock_screen")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp, horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack, modifier = Modifier.testTag("app_lock_back_btn")) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
            }
            Text(
                text = "App Lock",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.weight(1f)
            )
            if (hasPin) {
                TextButton(
                    onClick = { viewModel.requestChangeAppLockPin() },
                    modifier = Modifier.testTag("app_lock_change_pin_btn")
                ) {
                    Text("Change PIN")
                }
            }
        }

        Text(
            text = "Locked apps ask for your PIN every time they are opened. " +
                "To lock an app, long-press it in the drawer and tap \"Lock app\".",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.65f),
            modifier = Modifier.padding(horizontal = 18.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (lockedInfos.isEmpty()) {
            Text(
                text = if (hasPin) {
                    "No locked apps yet."
                } else {
                    "No PIN set yet. Lock your first app from the drawer and you " +
                        "will be asked to set one then."
                },
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp)
            )
        }

        LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp)) {
            items(lockedInfos, key = { it.packageName }) { app ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 10.dp)
                        .testTag("locked_app_${app.packageName}"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RealAppIcon(app = app, size = 34.dp, isDarkTheme = false)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
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
                    TextButton(
                        onClick = { viewModel.requestUnlockApp(app.packageName) },
                        modifier = Modifier.testTag("unlock_${app.packageName}")
                    ) {
                        Text("Unlock")
                    }
                }
            }
        }
    }
}
