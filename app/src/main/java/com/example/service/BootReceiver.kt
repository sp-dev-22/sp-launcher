package com.example.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.data.repository.LauncherPreferences

/** Re-arms all Study Manager reminders after a reboot or an app update. */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_MY_PACKAGE_REPLACED -> {
                val prefs = LauncherPreferences(context)
                StudyReminderScheduler.rescheduleAll(context, prefs.studyCourses.value)
                StudyReminderScheduler.applyDailySummary(
                    context,
                    prefs.showStudySummary.value,
                    prefs.studySummaryHour.value,
                    prefs.studySummaryMinute.value
                )
            }
        }
    }
}
