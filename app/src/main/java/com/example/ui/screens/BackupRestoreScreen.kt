package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.LauncherViewModel
import com.example.util.FileIo
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun BackupRestoreScreen(
    viewModel: LauncherViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var message by remember { mutableStateOf<String?>(null) }

    val autoBackup by viewModel.autoBackupEnabled.collectAsState()
    val backupFolder by viewModel.backupFolderUri.collectAsState()
    val backupFolderValid by viewModel.backupFolderValid.collectAsState()
    val lastBackup by viewModel.lastBackupMillis.collectAsState()
    val backupMessage by viewModel.backupMessage.collectAsState()

    val folderPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
            } catch (_: Exception) {}
            viewModel.setBackupFolderUri(uri.toString())
        }
    }

    LaunchedEffect(backupMessage) {
        if (backupMessage != null) {
            delay(4000)
            viewModel.clearBackupMessage()
        }
    }

    val backupLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            val ok = FileIo.writeText(context, uri, viewModel.exportAllData())
            message = if (ok) "Backup saved successfully." else "Could not save the backup file."
        }
    }

    val restoreLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            val text = FileIo.readText(context, uri)
            message = when {
                text == null -> "Could not read the selected file."
                viewModel.importAllData(text) -> "Restore complete — your data is back."
                else -> "That file is not a valid SP Launcher backup."
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 20.dp)
            .testTag("backup_restore_screen")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack, modifier = Modifier.testTag("backup_back_btn")) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
            }
            Text(
                text = "Backup & Restore",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("What gets backed up", fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "• All settings & themes\n• All Alerts (with tags & reminders)\n• All home-screen tasks\n• All Study Manager data",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
                    lineHeight = 19.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = { backupLauncher.launch("sp-launcher-backup.json") },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("backup_btn")
        ) {
            Icon(Icons.Default.Backup, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.size(8.dp))
            Text("Backup to file")
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedButton(
            onClick = { restoreLauncher.launch(arrayOf("application/json", "text/plain", "*/*")) },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("restore_btn")
        ) {
            Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.size(8.dp))
            Text("Restore from file")
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "AUTO-BACKUP",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
        )
        Spacer(modifier = Modifier.height(8.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Auto-backup daily", fontWeight = FontWeight.SemiBold)
                        Text(
                            text = when {
                                backupFolder == null -> "No folder chosen yet"
                                backupFolderValid -> "Folder selected"
                                else -> "Folder not available"
                            },
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                        )
                    }
                    Switch(checked = autoBackup, onCheckedChange = { viewModel.setAutoBackupEnabled(it) })
                }

                // v1.13 hint: auto-backup ON but unusable must never look fine —
                // after a restore the folder permission does not come back, and a
                // deleted folder fails the same way. Say which one it is.
                if (autoBackup && !backupFolderValid) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (backupFolder == null)
                            "Auto backup is on, but no backup folder is selected. Choose a folder so backups can run."
                        else
                            "Backup folder is not available (permission lost or folder deleted). Choose the folder again.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                Row {
                    TextButton(onClick = { folderPicker.launch(null) }) { Text("Choose folder") }
                    TextButton(onClick = { viewModel.runBackupNow() }, enabled = backupFolder != null) {
                        Text("Backup now")
                    }
                }

                Text(
                    text = if (lastBackup > 0L)
                        "Last backup: " + SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(lastBackup))
                    else "No backup yet.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f)
                )

                backupMessage?.let {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(it, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        message?.let { text ->
            Text(
                text = text,
                color = if (text.contains("complete") || text.contains("saved")) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error,
                fontSize = 13.sp
            )
        }
    }
}
