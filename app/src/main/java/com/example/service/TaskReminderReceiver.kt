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
import com.example.data.model.RepeatRule
import com.example.data.repository.LauncherPreferences
import com.example.util.RepeatSchedule

/**
 * Receives the alarms scheduled by [TaskReminderScheduler] and shows a task reminder.
 *
 * The notification carries a "Done" action (ticks the task off without opening the app) and
 * "Snooze 10 min" / "Snooze 1 hour". A task whose reminder repeats (daily / weekly) re-arms
 * itself for the next occurrence as soon as it fires.
 */
class TaskReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getStringExtra(TaskReminderScheduler.EXTRA_ID).orEmpty()
        if (id.isEmpty()) return

        when (intent.action) {
            ACTION_DONE -> {
                markDone(context, id)
                return
            }
            ACTION_SNOOZE -> {
                snooze(context, id, intent.getIntExtra(EXTRA_SNOOZE_MIN, 10))
                return
            }
        }

        val text = intent.getStringExtra(TaskReminderScheduler.EXTRA_TEXT).orEmpty()
        showReminder(context, id, text)
        advanceIfRepeating(context, id)
    }

    private fun markDone(context: Context, id: String) {
        val prefs = LauncherPreferences(context)
        val task = prefs.tasks.value.firstOrNull { it.id == id } ?: return
        prefs.updateTaskItem(
            task.copy(
                isCompleted = true,
                completedAtMillis = System.currentTimeMillis(),
                reminderMillis = null
            )
        )
        TaskReminderScheduler.rescheduleAll(context, prefs.tasks.value)
        NotificationManagerCompat.from(context).cancel(notifId(id))
    }

    private fun snooze(context: Context, id: String, minutes: Int) {
        val prefs = LauncherPreferences(context)
        val task = prefs.tasks.value.firstOrNull { it.id == id } ?: return
        prefs.updateTaskItem(
            task.copy(reminderMillis = System.currentTimeMillis() + minutes * 60_000L)
        )
        TaskReminderScheduler.rescheduleAll(context, prefs.tasks.value)
        NotificationManagerCompat.from(context).cancel(notifId(id))
    }

    private fun advanceIfRepeating(context: Context, id: String) {
        val prefs = LauncherPreferences(context)
        val task = prefs.tasks.value.firstOrNull { it.id == id } ?: return
        if (task.repeat == RepeatRule.NONE) return
        val next = RepeatSchedule.next(task.repeat, System.currentTimeMillis()) ?: return
        prefs.updateTaskItem(task.copy(reminderMillis = next))
        TaskReminderScheduler.rescheduleAll(context, prefs.tasks.value)
    }

    private fun showReminder(context: Context, id: String, text: String) {
        TaskReminderScheduler.ensureChannel(context)

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

        val notification = NotificationCompat.Builder(context, TaskReminderScheduler.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_focus)
            .setContentTitle("Task")
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(contentPi)
            .addAction(0, "Done", actionPi(context, ACTION_DONE, id, 1))
            .addAction(0, "Snooze 10 min", snoozePi(context, id, 10))
            .addAction(0, "Snooze 1 hour", snoozePi(context, id, 60))
            .build()

        try {
            NotificationManagerCompat.from(context).notify(notifId(id), notification)
        } catch (_: SecurityException) {
            // Permission revoked between the check and the notify call - nothing to do.
        }
    }

    private fun actionPi(context: Context, action: String, id: String, code: Int): PendingIntent {
        val intent = Intent(context, TaskReminderReceiver::class.java).apply {
            this.action = action
            putExtra(TaskReminderScheduler.EXTRA_ID, id)
        }
        return PendingIntent.getBroadcast(
            context,
            (id + action).hashCode() + code,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun snoozePi(context: Context, id: String, minutes: Int): PendingIntent {
        val intent = Intent(context, TaskReminderReceiver::class.java).apply {
            action = ACTION_SNOOZE
            putExtra(TaskReminderScheduler.EXTRA_ID, id)
            putExtra(EXTRA_SNOOZE_MIN, minutes)
        }
        return PendingIntent.getBroadcast(
            context,
            "$id-task-snooze-$minutes".hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun notifId(id: String): Int = "task-$id".hashCode()

    companion object {
        const val ACTION_DONE = "com.example.sp_launcher.ACTION_TASK_DONE"
        const val ACTION_SNOOZE = "com.example.sp_launcher.ACTION_TASK_SNOOZE"
        const val EXTRA_SNOOZE_MIN = "task_snooze_min"
    }
}
