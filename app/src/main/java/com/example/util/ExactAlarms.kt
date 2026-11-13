package com.example.util

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings

/**
 * Exact alarms for reminders (v1.17.0).
 *
 * Before Android 12 (API 31) exact alarms needed no permission at all, so they
 * are used directly. From API 31 they need SCHEDULE_EXACT_ALARM, a special
 * permission the user grants in Settings — there is no runtime prompt for it.
 * Everything here degrades gracefully: when the permission is missing, reminders
 * fall back to the old inexact alarm instead of crashing or blocking.
 */
object ExactAlarms {

    /** True when an exact alarm scheduled now would actually fire exactly. */
    fun canScheduleExact(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
        val am = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
            ?: return false
        return am.canScheduleExactAlarms()
    }

    /**
     * Schedules [pi] at [triggerAtMillis]: exact (and Doze-proof) when allowed,
     * the old inexact alarm otherwise. Never throws for a missing permission —
     * on API 31+ calling setExact* without it would raise a SecurityException,
     * which is exactly what the guard prevents.
     */
    fun schedule(am: AlarmManager, triggerAtMillis: Long, pi: PendingIntent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !am.canScheduleExactAlarms()) {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pi)
        } else {
            // setExactAndAllowWhileIdle is API 23+; minSdk is 24, so no guard needed.
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pi)
        }
    }

    /**
     * Opens the system exact-alarm settings page for this app, so the user can
     * allow exact alarms with one tap. No-op below API 31 and when the page
     * cannot be opened — the caller never has to guard it.
     */
    fun openSettings(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
        val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
            data = Uri.fromParts("package", context.packageName, null)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(intent)
        } catch (_: Exception) {
        }
    }
}
