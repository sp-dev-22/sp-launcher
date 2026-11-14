package com.example.service

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Notification badges (v1.18.0): unread counts on app icons in the drawer and
 * the favourites bar.
 *
 * Counts only, never content: the listener reads each notification's package
 * name and nothing else — no title, no text, no extras — and keeps only a
 * per-package count in memory. No polling: the counts refresh when a
 * notification is posted or removed, and once when the listener connects.
 *
 * The user grants Notification access lazily, from Settings > Notification
 * badges, which opens the system Notification-access screen. Until then the
 * service never runs and no badge can show. The service runs in the app's own
 * process, so the UI reads the counts straight from the shared flow below.
 */
class BadgeListenerService : NotificationListenerService() {

    companion object {
        private val _counts = MutableStateFlow<Map<String, Int>>(emptyMap())

        /** Package name -> number of unread notifications, right now. */
        val counts: StateFlow<Map<String, Int>> = _counts.asStateFlow()
    }

    override fun onListenerConnected() {
        refreshCounts()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        refreshCounts()
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        refreshCounts()
    }

    /** Rebuilds the counts from the currently active notifications. */
    private fun refreshCounts() {
        val active = try {
            activeNotifications
        } catch (_: Exception) {
            null
        } ?: return
        _counts.value = active
            .filter { !it.isOngoing && it.packageName != packageName }
            .groupingBy { it.packageName }
            .eachCount()
    }
}

/** True when the user has granted this app Notification access. */
fun isBadgeListenerEnabled(context: Context): Boolean {
    val flat = Settings.Secure.getString(
        context.contentResolver,
        "enabled_notification_listeners"
    ) ?: return false
    val mine = ComponentName(context, BadgeListenerService::class.java).flattenToString()
    return flat.split(":").any { it == mine }
}

/** Opens the system Notification-access screen, where the user grants access. */
fun openBadgeListenerSettings(context: Context) {
    val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    try {
        context.startActivity(intent)
    } catch (_: Exception) {
        // No settings app to open — the row just does nothing.
    }
}
