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
import com.example.data.repository.LauncherPreferences

/**
 * Receives the alarms scheduled by [AlertReminderScheduler] and shows the reminder
 * notification for a home-screen Alert. The notification carries "Snooze 10 min" and
 * "Snooze 1 hour" actions that push the alert's reminder forward and re-arm it.
 */
class AlertReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getStringExtra(AlertReminderScheduler.EXTRA_ID).orEmpty()

        // A snooze tap from the notification: move this alert's reminder forward.
        if (intent.action == ACTION_SNOOZE) {
            if (id.isEmpty()) return
            val minutes = intent.getIntExtra(EXTRA_SNOOZE_MIN, 10)
            val prefs = LauncherPreferences(context)
            val alert = prefs.alerts.value.firstOrNull { it.id == id } ?: return
            prefs.updateAlert(alert.copy(reminderMillis = System.currentTimeMillis() + minutes * 60_000L))
            AlertReminderScheduler.rescheduleAll(context, prefs.alerts.value)
            NotificationManagerCompat.from(context).cancel("alert-$id".hashCode())
            return
        }

        val title = intent.getStringExtra(AlertReminderScheduler.EXTRA_TITLE) ?: "Alert"
        val message = intent.getStringExtra(AlertReminderScheduler.EXTRA_MESSAGE).orEmpty()

        AlertReminderScheduler.ensureChannel(context)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPi = PendingIntent.getActivity(
            context,
            0,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, AlertReminderScheduler.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_focus)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(contentPi)
            .addAction(0, "Snooze 10 min", snoozePendingIntent(context, id, 10))
            .addAction(0, "Snooze 1 hour", snoozePendingIntent(context, id, 60))
            .build()

        try {
            NotificationManagerCompat.from(context).notify("alert-$id".hashCode(), notification)
        } catch (_: SecurityException) {
            // Permission revoked between the check and the notify call — nothing to do.
        }
    }

    private fun snoozePendingIntent(context: Context, id: String, minutes: Int): PendingIntent {
        val intent = Intent(context, AlertReminderReceiver::class.java).apply {
            action = ACTION_SNOOZE
            putExtra(AlertReminderScheduler.EXTRA_ID, id)
            putExtra(EXTRA_SNOOZE_MIN, minutes)
        }
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        return PendingIntent.getBroadcast(context, "$id-snooze-$minutes".hashCode(), intent, flags)
    }

    companion object {
        const val ACTION_SNOOZE = "com.example.sp_launcher.ACTION_ALERT_SNOOZE"
        const val EXTRA_SNOOZE_MIN = "alert_snooze_min"
    }
}
