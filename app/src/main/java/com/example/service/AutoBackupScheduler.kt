package com.example.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri

/** Fires a daily auto-backup. One alarm at a time; each fire asks for the next. */
object AutoBackupScheduler {

    private const val SCHEME = "spbackup"
    private const val DAY_MS = 24 * 60 * 60 * 1000L

    private fun alarmManager(context: Context): AlarmManager? =
        context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager

    private fun pendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, AutoBackupReceiver::class.java).apply {
            data = Uri.parse("$SCHEME://next")
        }
        return PendingIntent.getBroadcast(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    fun cancel(context: Context) {
        val am = alarmManager(context) ?: return
        val pi = pendingIntent(context)
        am.cancel(pi)
        pi.cancel()
    }

    fun reschedule(context: Context, enabled: Boolean) {
        cancel(context)
        if (!enabled) return
        val am = alarmManager(context) ?: return
        am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + DAY_MS, pendingIntent(context))
    }
}
