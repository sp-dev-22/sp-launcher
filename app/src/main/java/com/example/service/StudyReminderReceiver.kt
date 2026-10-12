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
 * Receives the alarms scheduled by [StudyReminderScheduler] and shows the notification.
 * A missed-deadline alarm first re-checks the store, so it stays silent if the task was
 * completed before the alarm fired.
 */
class StudyReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val title = intent.getStringExtra(StudyReminderScheduler.EXTRA_TITLE) ?: "Study reminder"
        val message = intent.getStringExtra(StudyReminderScheduler.EXTRA_MESSAGE).orEmpty()
        val type = intent.getStringExtra(StudyReminderScheduler.EXTRA_TYPE) ?: StudyReminderScheduler.TYPE_ALERT
        val itemId = intent.getStringExtra(StudyReminderScheduler.EXTRA_ITEM_ID).orEmpty()
        val level = intent.getStringExtra(StudyReminderScheduler.EXTRA_LEVEL) ?: StudyReminderScheduler.LEVEL_CLASS

        if (type == StudyReminderScheduler.TYPE_DEADLINE &&
            isItemCompleted(context, itemId, level)
        ) {
            return
        }

        StudyReminderScheduler.ensureChannel(context)

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

        val notification = NotificationCompat.Builder(context, StudyReminderScheduler.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_focus)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(contentPi)
            .build()

        try {
            NotificationManagerCompat.from(context).notify("$itemId-$type".hashCode(), notification)
        } catch (_: SecurityException) {
            // Permission revoked between the check and the notify call — nothing to do.
        }
    }

    private fun isItemCompleted(context: Context, itemId: String, level: String): Boolean {
        if (itemId.isEmpty()) return false
        val courses = LauncherPreferences(context).studyCourses.value
        for (course in courses) {
            for (subject in course.subjects) {
                for (topic in subject.topics) {
                    if (level == StudyReminderScheduler.LEVEL_TOPIC && topic.id == itemId) {
                        return topic.classes.isNotEmpty() && topic.classes.all { it.isCompleted }
                    }
                    if (level == StudyReminderScheduler.LEVEL_CLASS) {
                        val clazz = topic.classes.firstOrNull { it.id == itemId }
                        if (clazz != null) return clazz.isCompleted
                    }
                }
            }
        }
        return false
    }
}
