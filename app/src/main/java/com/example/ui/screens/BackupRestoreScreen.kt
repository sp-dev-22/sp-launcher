package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import android.content.Intent
import android.net.Uri
import androidx.compose.material3.Switch
import androidx.compose.runtime.collectAsState
import com.example.ui.LauncherViewModel
import com.example.util.FileIo
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

    val autoBackupEnabled by viewModel.autoBackupEnabled.collectAsState()
    val autoBackupUri by viewModel.autoBackupUri.collectAsState()
    val autoBackupLast by viewModel.autoBackupLast.collectAsState()

    val folderLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
            } catch (_: Exception) {
                // Some providers don't grant persistable permission; the backup still tries.
            }
            viewModel.setAutoBackupFolder(uri.toString())
            message = "Auto backup folder set. First backup runs within a few minutes."
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
                    text = "• All settings\n• All home-screen todos (notes, reminders, goals)\n• All Study Manager data\n• Focus history and app limits",
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

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Auto backup (weekly)", fontWeight = FontWeight.SemiBold)
                        Text(
                            text = if (autoBackupUri.isBlank()) "No folder chosen yet"
                            else "Folder: " + Uri.parse(autoBackupUri).lastPathSegment,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                        )
                        if (autoBackupLast > 0) {
                            Text(
                                text = "Last backup: " + SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date(autoBackupLast)),
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                            )
                        }
                    }
                    Switch(
                        checked = autoBackupEnabled,
                        onCheckedChange = { viewModel.setAutoBackup(it) }
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedButton(
                    onClick = { folderLauncher.launch(null) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("auto_backup_folder_btn")
                ) {
                    Text("Choose backup folder")
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
