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
    private var cachedUsage: Map<String, Int> = emptyMap()

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

    private fun startOfToday(): Long {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return calendar.timeInMillis
    }

    /**
     * Total foreground minutes per app for today.
     *
     * Computed from usage *events* rather than queryUsageStats(INTERVAL_DAILY). The daily
     * buckets are not aligned to midnight, so they could credit an app with time from before
     * today; and the old code then added the running session on top, so an app could be
     * counted twice. Both made daily limits fire early or late.
     */
    fun getTodayAppUsageMinutes(): Map<String, Int> {
        if (!hasUsageStatsPermission()) return emptyMap()

        val now = System.currentTimeMillis()
        if (now - cachedAt < CACHE_TTL_MS) return cachedUsage

        val manager = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
            ?: return emptyMap()

        val millis = foregroundMillis(manager, startOfToday(), now)
        val result = HashMap<String, Int>()
        for ((pkg, ms) in millis) {
            val minutes = (ms / 60_000L).toInt()
            if (minutes > 0) result[pkg] = minutes
        }

        cachedAt = now
        cachedUsage = result
        return result
    }

    /** Foreground milliseconds per package within [from, to], session by session. */
    private fun foregroundMillis(
        manager: UsageStatsManager,
        from: Long,
        to: Long
    ): Map<String, Long> {
        val totals = HashMap<String, Long>()
        val events = try {
            manager.queryEvents(from, to)
        } catch (_: Exception) {
            null
        } ?: return totals

        val event = UsageEvents.Event()
        var currentPkg: String? = null
        var currentStart = 0L

        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            when (event.eventType) {
                UsageEvents.Event.ACTIVITY_RESUMED -> {
                    val previous = currentPkg
                    if (previous != null && previous != event.packageName &&
                        event.timeStamp > currentStart
                    ) {
                        totals[previous] = (totals[previous] ?: 0L) + (event.timeStamp - currentStart)
                    }
                    currentPkg = event.packageName
                    currentStart = event.timeStamp
                }
                UsageEvents.Event.ACTIVITY_PAUSED,
                UsageEvents.Event.ACTIVITY_STOPPED -> {
                    val current = currentPkg
                    if (current != null && event.packageName == current) {
                        if (event.timeStamp > currentStart) {
                            totals[current] = (totals[current] ?: 0L) + (event.timeStamp - currentStart)
                        }
                        currentPkg = null
                    }
                }
            }
        }

        // Whatever is still in the foreground right now.
        val stillOpen = currentPkg
        if (stillOpen != null && to > currentStart) {
            totals[stillOpen] = (totals[stillOpen] ?: 0L) + (to - currentStart)
        }
        return totals
    }

    /**
     * The package currently in the foreground, if any.
     *
     * Looks back several minutes and returns the app whose last "resumed" has no matching
     * "paused" - far more reliable than the previous 10-second window, which often missed
     * the transition and let a blocked app through.
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
    }
}
