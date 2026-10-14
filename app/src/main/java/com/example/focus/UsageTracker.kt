package com.example.focus

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Process
import android.provider.Settings
import java.util.Calendar

class UsageTracker(private val context: Context) {

    // The limit monitor runs every ~1.5s; caching avoids rescanning the whole day each tick.
    private var cachedAt = 0L
    private var cachedMillis: Map<String, Long> = emptyMap()

    fun hasUsageStatsPermission(): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    fun requestUsageStatsPermission() {
        val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(intent)
        } catch (_: Exception) {}
    }

    /**
     * Start of the current "usage day". The daily counter resets every night at 23:59
     * (not at midnight), so this returns the most recent 23:59 that has already passed.
     */
    private fun startOfToday(): Long {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (calendar.timeInMillis > System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_YEAR, -1)
        }
        return calendar.timeInMillis
    }

    /** Raw foreground milliseconds per app for today (cached for a few seconds). */
    private fun todayUsageMillis(): Map<String, Long> {
        if (!hasUsageStatsPermission()) return emptyMap()

        val now = System.currentTimeMillis()
        if (now - cachedAt < CACHE_TTL_MS) return cachedMillis

        val manager = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
            ?: return emptyMap()

        val millis = foregroundMillis(manager, startOfToday(), now)
        cachedAt = now
        cachedMillis = millis
        return millis
    }

    /**
     * Total foreground minutes per app for today.
     *
     * Minutes are *rounded* (not truncated) so a handful of apps used for a few seconds
     * each no longer collapse to 0 - that truncation was why the reported screen time
     * looked far too low.
     */
    fun getTodayAppUsageMinutes(): Map<String, Int> {
        val result = HashMap<String, Int>()
        for ((pkg, ms) in todayUsageMillis()) {
            val minutes = Math.round(ms / 60_000.0).toInt()
            if (minutes > 0) result[pkg] = minutes
        }
        return result
    }

    /**
     * Today's total screen time in minutes, computed from the raw millisecond totals
     * (so the whole-day figure is not the sum of individually-truncated app values).
     */
    fun getTodayTotalScreenTimeMinutes(): Int =
        Math.round(todayUsageMillis().values.sum() / 60_000.0).toInt()

    /**
     * Foreground milliseconds per package within [from, to], session by session.
     *
     * Events are scanned from a little *before* [from] as well, so a session that was
     * already open when the window began is still credited (clamped to [from, to])
     * instead of being dropped - another reason the daily total used to read low.
     */
    private fun foregroundMillis(
        manager: UsageStatsManager,
        from: Long,
        to: Long
    ): Map<String, Long> {
        val totals = HashMap<String, Long>()
        val events = try {
            manager.queryEvents(from - SEED_LOOKBACK_MS, to)
        } catch (_: Exception) {
            null
        } ?: return totals

        val event = UsageEvents.Event()
        var currentPkg: String? = null
        var currentStart = 0L

        fun credit(pkg: String?, end: Long) {
            if (pkg == null) return
            val start = maxOf(currentStart, from)
            val finish = minOf(end, to)
            if (finish > start) {
                totals[pkg] = (totals[pkg] ?: 0L) + (finish - start)
            }
        }

        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            when (event.eventType) {
                UsageEvents.Event.ACTIVITY_RESUMED -> {
                    credit(currentPkg, event.timeStamp)
                    currentPkg = event.packageName
                    currentStart = event.timeStamp
                }
                UsageEvents.Event.ACTIVITY_PAUSED,
                UsageEvents.Event.ACTIVITY_STOPPED -> {
                    if (event.packageName == currentPkg) {
                        credit(currentPkg, event.timeStamp)
                        currentPkg = null
                    }
                }
            }
        }

        // Whatever is still in the foreground right now.
        credit(currentPkg, to)
        return totals
    }

    /**
     * The package currently in the foreground, if any.
     *
     * Looks back several minutes and returns the app whose last "resumed" has no matching
     * "paused" - far more reliable than a tiny window, which often missed the transition
     * and let a blocked app through.
     */
    fun getForegroundPackage(): String? {
        if (!hasUsageStatsPermission()) return null

        val manager = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
            ?: return null

        val now = System.currentTimeMillis()
        val events = try {
            manager.queryEvents(now - LOOKBACK_MS, now)
        } catch (_: Exception) {
            null
        } ?: return null

        val event = UsageEvents.Event()
        var currentPkg: String? = null
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            when (event.eventType) {
                UsageEvents.Event.ACTIVITY_RESUMED ->
                    currentPkg = event.packageName
                UsageEvents.Event.ACTIVITY_PAUSED,
                UsageEvents.Event.ACTIVITY_STOPPED ->
                    if (event.packageName == currentPkg) currentPkg = null
            }
        }
        return currentPkg
    }

    companion object {
        private const val CACHE_TTL_MS = 10_000L
        private const val LOOKBACK_MS = 10 * 60_000L

        /** How far before the window start to scan so an already-open session is credited. */
        private const val SEED_LOOKBACK_MS = 6 * 60 * 60_000L
    }
}
