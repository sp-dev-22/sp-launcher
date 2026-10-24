package com.example.service

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import com.example.data.model.TaskItem

/**
 * Schedules reminder notifications for tasks using [AlarmManager].
 *
 * Uses [AlarmManager.setAndAllowWhileIdle] (inexact but works in Doze), so the app needs no
 * SCHEDULE_EXACT_ALARM permission. Completed tasks and past reminders are skipped.
 */
object TaskReminderScheduler {

    const val CHANNEL_ID = "tasks_channel"

    const val EXTRA_ID = "task_extra_id"
    const val EXTRA_TEXT = "task_extra_text"

    private const val SCHEME = "sptask"

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(NotificationManager::class.java) ?: return
            if (manager.getNotificationChannel(CHANNEL_ID) == null) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    "Tasks",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Reminders for your tasks"
                }
                manager.createNotificationChannel(channel)
            }
        }
    }

    private fun alarmManager(context: Context): AlarmManager? =
        context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager

    private fun buildPendingIntent(context: Context, key: String, text: String): PendingIntent {
        val intent = Intent(context, TaskReminderReceiver::class.java).apply {
            data = Uri.parse("$SCHEME://task/$key")
            putExtra(EXTRA_ID, key)
            putExtra(EXTRA_TEXT, text)
        }
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        return PendingIntent.getBroadcast(context, key.hashCode(), intent, flags)
    }

    /** Cancels the pending reminder for a single task id. */
    fun cancel(context: Context, id: String) {
        val am = alarmManager(context) ?: return
        val pi = buildPendingIntent(context, id, "")
        am.cancel(pi)
        pi.cancel()
    }

    /** Cancels and re-schedules every task reminder from the current data. */
    fun rescheduleAll(context: Context, tasks: List<TaskItem>) {
        ensureChannel(context)
        val am = alarmManager(context) ?: return
        for (task in tasks) {
            cancel(context, task.id)
            if (task.isCompleted) continue
            val whenMillis = task.reminderMillis ?: continue
            if (whenMillis <= System.currentTimeMillis()) continue
            am.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                whenMillis,
                buildPendingIntent(context, task.id, task.text)
            )
        }
    }
}
