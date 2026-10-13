package com.example.service

import android.Manifest
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R

/** Shows the daily "Please Backup Your Data Now." reminder scheduled by [BackupReminderScheduler]. */
class BackupReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != BackupReminderScheduler.ACTION_BACKUP_REMINDER) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        BackupReminderScheduler.ensureChannel(context)

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPi = PendingIntent.getActivity(
            context,
            0,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, BackupReminderScheduler.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_focus)
            .setContentTitle("Please Backup Your Data Now.")
            .setContentText("Open SP Launcher → Backup & Restore to save your data to a file.")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("Open SP Launcher → Settings → Backup & Restore to save all your settings, tasks and Study Manager data to a file.")
            )
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(contentPi)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(BACKUP_REMINDER_NOTIFICATION_ID, notification)
        } catch (_: SecurityException) {
            // Notification permission revoked — nothing to do.
        }
    }

    companion object {
        private const val BACKUP_REMINDER_NOTIFICATION_ID = 1004
    }
}
