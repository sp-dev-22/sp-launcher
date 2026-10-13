package com.example.service

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import java.util.Calendar

/**
 * Schedules a friendly once-a-day reminder ("Please Backup Your Data Now.") at 19:00.
 *
 * It only nudges the user — the actual backup is still the manual export/import in
 * Settings > Backup & Restore. Uses [AlarmManager.setInexactRepeating] so the app needs no
 * exact-alarm permission and the alarm survives Doze.
 */
object BackupReminderScheduler {

    const val CHANNEL_ID = "backup_reminder_channel"
    const val ACTION_BACKUP_REMINDER = "com.example.service.ACTION_BACKUP_REMINDER"

    /** Evening reminder time. */
    private const val REMINDER_HOUR = 19
    private const val REMINDER_MINUTE = 0

    private const val REQUEST_CODE = 8200

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(NotificationManager::class.java) ?: return
            if (manager.getNotificationChannel(CHANNEL_ID) == null) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    "Backup Reminder",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "A daily nudge to back up your data"
                }
                manager.createNotificationChannel(channel)
            }
        }
    }

    private fun pendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, BackupReminderReceiver::class.java).apply {
            action = ACTION_BACKUP_REMINDER
        }
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        return PendingIntent.getBroadcast(context, REQUEST_CODE, intent, flags)
    }

    /** (Re)arms the daily 19:00 backup reminder. Safe to call on every launch / after boot. */
    fun apply(context: Context) {
        ensureChannel(context)
        val am = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val pi = pendingIntent(context)
        am.cancel(pi)
        pi.cancel()

        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, REMINDER_HOUR)
            set(Calendar.MINUTE, REMINDER_MINUTE)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        // If 19:00 has already passed today, start from tomorrow.
        if (cal.timeInMillis <= System.currentTimeMillis()) {
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }

        am.setInexactRepeating(
            AlarmManager.RTC_WAKEUP,
            cal.timeInMillis,
            AlarmManager.INTERVAL_DAY,
            pi
        )
    }
}
