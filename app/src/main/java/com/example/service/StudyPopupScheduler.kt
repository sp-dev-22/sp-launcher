package com.example.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.example.data.model.StudyPopupSettings
import java.util.Calendar

/**
 * Fires the Study Popups at a fixed interval inside the user's start-stop window.
 * One alarm is chained at a time: each fire asks for the next slot.
 */
object StudyPopupScheduler {

    const val ACTION_POPUP = "com.example.sp_launcher.ACTION_STUDY_POPUP"
    private const val SCHEME = "sppopup"

    private fun alarmManager(context: Context): AlarmManager? =
        context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager

    private fun pendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, StudyPopupReceiver::class.java).apply {
            action = ACTION_POPUP
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

    /** Cancels any pending tick and, if enabled, schedules the next one. */
    fun reschedule(context: Context, settings: StudyPopupSettings) {
        cancel(context)
        if (!settings.enabled) return
        val trigger = nextTrigger(settings) ?: return
        alarmManager(context)?.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger, pendingIntent(context))
    }

    /**
     * The next slot time: the next interval boundary inside today's window, or
     * tomorrow's start once today's window is over.
     */
    fun nextTrigger(settings: StudyPopupSettings): Long? {
        val now = System.currentTimeMillis()
        val start = todayAt(settings.startMinutes)
        val stop = todayAt(settings.stopMinutes)
        if (stop <= start) return null

        val step = settings.intervalMinutes.coerceAtLeast(1) * 60_000L
        return when {
            now < start -> start
            now >= stop -> tomorrowAt(settings.startMinutes)
            else -> {
                val elapsed = now - start
                val next = start + (elapsed / step + 1) * step
                if (next < stop) next else tomorrowAt(settings.startMinutes)
            }
        }
    }

    private fun todayAt(minutesOfDay: Int): Long = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, minutesOfDay / 60)
        set(Calendar.MINUTE, minutesOfDay % 60)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    private fun tomorrowAt(minutesOfDay: Int): Long = Calendar.getInstance().apply {
        add(Calendar.DAY_OF_YEAR, 1)
        set(Calendar.HOUR_OF_DAY, minutesOfDay / 60)
        set(Calendar.MINUTE, minutesOfDay % 60)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
}
