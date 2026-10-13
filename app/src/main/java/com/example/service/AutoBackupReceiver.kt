package com.example.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.data.repository.LauncherPreferences

/** Writes a timestamped backup file into the user-chosen folder, then re-arms for next week. */
class AutoBackupReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != AutoBackupScheduler.ACTION_AUTO_BACKUP) return

        val prefs = LauncherPreferences(context)
        if (!prefs.autoBackupEnabled.value) return
        val uriString = prefs.autoBackupUri.value
        if (uriString.isBlank()) return

        if (AutoBackup.write(context, uriString, prefs.exportAllData())) {
            prefs.setAutoBackupLast(System.currentTimeMillis())
        }

        // Re-arm for next week.
        AutoBackupScheduler.scheduleNextWeekly(context)
    }
}
