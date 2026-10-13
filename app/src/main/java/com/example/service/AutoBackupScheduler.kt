package com.example.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent

/**
 * Schedules the weekly automatic backup. The backup file is written into the folder the
 * user picked in Settings > Backup & Restore.
 */
object AutoBackupScheduler {

    const val ACTION_AUTO_BACKUP = "com.example.service.ACTION_AUTO_BACKUP"
    private const val REQUEST_CODE = 8100

    private fun pendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, AutoBackupReceiver::class.java).apply {
            action = ACTION_AUTO_BACKUP
        }
        return PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    /** Enables or cancels the weekly auto-backup. */
    fun apply(context: Context, enabled: Boolean) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val pi = pendingIntent(context)
        am.cancel(pi)
        pi.cancel()
        if (!enabled) return
        // First backup ~5 minutes after enabling, then weekly (re-armed by the receiver).
        scheduleAt(context, System.currentTimeMillis() + 5 * 60_000L)
    }

    /** Schedules the next weekly backup — called by the receiver after each run. */
    fun scheduleNextWeekly(context: Context) {
        scheduleAt(context, System.currentTimeMillis() + AlarmManager.INTERVAL_DAY * 7)
    }

    private fun scheduleAt(context: Context, triggerAt: Long) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        // One-shot, Doze-friendly alarm — the same mechanism the working reminders use.
        am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent(context))
    }
}
