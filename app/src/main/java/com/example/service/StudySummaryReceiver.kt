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
import com.example.data.model.allClassRefs
import com.example.data.repository.LauncherPreferences
import java.util.Calendar

/**
 * Fires the once-a-day study summary notification:
 * "X due today, Y overdue · Z min studied".
 */
class StudySummaryReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != StudyReminderScheduler.ACTION_SUMMARY) return

        val prefs = LauncherPreferences(context)
        if (!prefs.showStudySummary.value) return

        // Re-arm for the next day first, so one quiet day never stops the summary.
        StudyReminderScheduler.applyDailySummary(
            context, true, prefs.studySummaryHour.value, prefs.studySummaryMinute.value
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val now = System.currentTimeMillis()
        val startOfToday = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val endOfToday = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
        }.timeInMillis

        var dueToday = 0
        var overdue = 0
        for (course in prefs.studyCourses.value) {
            for (ref in course.allClassRefs()) {
                if (ref.clazz.isCompleted) continue
                val deadline = ref.clazz.deadlineMillis ?: continue
                if (deadline < now) overdue++ else if (deadline in now..endOfToday) dueToday++
            }
        }
        val studiedToday = prefs.focusHistory.value
            .filter { it.timestamp in startOfToday..endOfToday }
            .sumOf { it.durationMinutes }

        if (dueToday == 0 && overdue == 0 && studiedToday == 0) return

        val message = buildString {
            append("$dueToday due today, $overdue overdue")
            if (studiedToday > 0) append(" · $studiedToday min studied")
        }

        StudyReminderScheduler.ensureChannel(context)

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
            .setContentTitle("Study summary")
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(contentPi)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(SUMMARY_NOTIFICATION_ID, notification)
        } catch (_: SecurityException) {
            // Notification permission revoked — nothing to do.
        }
    }

    companion object {
        private const val SUMMARY_NOTIFICATION_ID = 1003
    }
}
