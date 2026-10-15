package com.example.service

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import com.example.data.model.AlertItem

/**
 * Schedules the reminder notifications for the home-screen Alerts using [AlarmManager].
 *
 * Uses [AlarmManager.setAndAllowWhileIdle] (inexact but works in Doze) so the app
 * needs no SCHEDULE_EXACT_ALARM permission.
 */
object AlertReminderScheduler {

    const val CHANNEL_ID = "alerts_channel"

    const val EXTRA_TITLE = "alert_extra_title"
    const val EXTRA_MESSAGE = "alert_extra_message"
    const val EXTRA_ID = "alert_extra_id"

    private const val SCHEME = "spalert"

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(NotificationManager::class.java) ?: return
            if (manager.getNotificationChannel(CHANNEL_ID) == null) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    "Alerts",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Reminders for your home-screen alerts"
                }
                manager.createNotificationChannel(channel)
            }
        }
    }

    private fun alarmManager(context: Context): AlarmManager? =
        context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager

    private fun buildPendingIntent(
        context: Context,
        key: String,
        title: String,
        message: String
    ): PendingIntent {
        val intent = Intent(context, AlertReminderReceiver::class.java).apply {
            data = Uri.parse("$SCHEME://alert/$key")
            putExtra(EXTRA_TITLE, title)
            putExtra(EXTRA_MESSAGE, message)
            putExtra(EXTRA_ID, key)
        }
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        return PendingIntent.getBroadcast(context, key.hashCode(), intent, flags)
    }

    /** Cancels the pending reminder for a single alert id. */
    fun cancel(context: Context, id: String) {
        val am = alarmManager(context) ?: return
        val pi = buildPendingIntent(context, id, "", "")
        am.cancel(pi)
        pi.cancel()
    }

    /** Cancels and re-schedules every alert reminder from the current data. */
    fun rescheduleAll(context: Context, alerts: List<AlertItem>) {
        ensureChannel(context)
        val am = alarmManager(context) ?: return
        for (alert in alerts) {
            cancel(context, alert.id)
            val whenMillis = alert.reminderMillis ?: continue
            if (whenMillis <= System.currentTimeMillis()) continue
            val pi = buildPendingIntent(context, alert.id, alert.title, alert.content)
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, whenMillis, pi)
        }
    }
}
