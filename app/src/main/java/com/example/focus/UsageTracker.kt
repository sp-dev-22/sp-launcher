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
     * Accurately calculates total screen time for each app today, including any
     * currently running foreground activity in real-time.
     */
    fun getTodayAppUsageMinutes(): Map<String, Int> {
        if (!hasUsageStatsPermission()) return emptyMap()

        val usageStatsManager =
            context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
                ?: return emptyMap()

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startOfDay = calendar.timeInMillis
        val now = System.currentTimeMillis()

        val usageMapMillis = mutableMapOf<String, Long>()

        // 1. Query batched stats
        val stats = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            startOfDay,
            now
        )
        if (stats != null) {
            for (usage in stats) {
                if (usage.totalTimeInForeground > 0) {
                    usageMapMillis[usage.packageName] = usage.totalTimeInForeground
                }
            }
        }

        // 2. Query recent usage events (last 1 hour) to account for currently active sessions
        try {
            val recentEvents = usageStatsManager.queryEvents(now - 3600_000L, now)
            if (recentEvents != null) {
                var lastResumedPkg: String? = null
                var lastResumedTime = 0L

                val event = UsageEvents.Event()
                while (recentEvents.hasNextEvent()) {
                    recentEvents.getNextEvent(event)
                    if (event.eventType == UsageEvents.Event.ACTIVITY_RESUMED) {
                        lastResumedPkg = event.packageName
                        lastResumedTime = event.timeStamp
                    } else if (event.eventType == UsageEvents.Event.ACTIVITY_PAUSED ||
                        event.eventType == UsageEvents.Event.ACTIVITY_STOPPED
                    ) {
                        if (event.packageName == lastResumedPkg) {
                            lastResumedPkg = null
                        }
                    }
                }

                // If an app is still in foreground right now, add active elapsed duration
                if (lastResumedPkg != null && lastResumedTime > 0) {
                    val activeRunningMillis = (now - lastResumedTime).coerceAtLeast(0L)
                    val current = usageMapMillis[lastResumedPkg] ?: 0L
                    usageMapMillis[lastResumedPkg] = current + activeRunningMillis
                }
            }
        } catch (_: Exception) {}

        // Convert millis to minutes
        val resultMap = mutableMapOf<String, Int>()
        for ((pkg, millis) in usageMapMillis) {
            val minutes = (millis / (1000 * 60)).toInt()
            if (minutes > 0) {
                resultMap[pkg] = minutes
            }
        }
        return resultMap
    }

    /**
     * Returns the currently resumed foreground package name if any.
     */
    fun getForegroundPackage(): String? {
        if (!hasUsageStatsPermission()) return null
        val usageStatsManager =
            context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
                ?: return null

        val now = System.currentTimeMillis()
        val events = usageStatsManager.queryEvents(now - 10_000L, now) ?: return null

        var topPackage: String? = null
        val event = UsageEvents.Event()
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            if (event.eventType == UsageEvents.Event.ACTIVITY_RESUMED) {
                topPackage = event.packageName
            }
        }
        return topPackage
    }
}
