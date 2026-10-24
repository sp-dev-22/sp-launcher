package com.example.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.data.repository.LauncherPreferences
import com.example.util.AutoBackup

/** Runs the daily backup, then re-arms itself. */
class AutoBackupReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val prefs = LauncherPreferences(context)
        if (prefs.autoBackupEnabled.value) {
            AutoBackup.perform(context, prefs)
        }
        AutoBackupScheduler.reschedule(context, prefs.autoBackupEnabled.value)
    }
}
