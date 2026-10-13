package com.example.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import com.example.data.repository.LauncherPreferences
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Writes a timestamped backup file into the user-chosen folder. */
class AutoBackupReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != AutoBackupScheduler.ACTION_AUTO_BACKUP) return

        val prefs = LauncherPreferences(context)
        if (!prefs.autoBackupEnabled.value) return
        val uriString = prefs.autoBackupUri.value
        if (uriString.isBlank()) return

        val treeUri = Uri.parse(uriString)
        val json = prefs.exportAllData()
        val name = "sp-launcher-backup-" +
            SimpleDateFormat("yyyy-MM-dd_HHmm", Locale.US).format(Date()) + ".json"

        try {
            val docUri = DocumentsContract.createDocument(
                context.contentResolver, treeUri, "application/json", name
            ) ?: return
            context.contentResolver.openOutputStream(docUri)?.use {
                it.write(json.toByteArray(Charsets.UTF_8))
            }
            prefs.setAutoBackupLast(System.currentTimeMillis())
        } catch (_: Exception) {
            // Folder permission revoked or storage unavailable — try again next week.
        }
    }
}
