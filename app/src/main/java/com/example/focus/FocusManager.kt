package com.example.focus

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import com.example.data.repository.LauncherPreferences
import com.example.service.FocusOverlayService

class FocusManager(private val context: Context) {

    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private val preferences = LauncherPreferences(context)

    fun canDrawOverlays(): Boolean {
        return Settings.canDrawOverlays(context)
    }

    fun requestOverlayPermission() {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:${context.packageName}")
        ).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(intent)
        } catch (_: Exception) {
            val fallback = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(fallback)
        }
    }

    fun isNotificationPolicyAccessGranted(): Boolean {
        return notificationManager.isNotificationPolicyAccessGranted
    }

    fun requestNotificationPolicyAccess() {
        val intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    fun startFocusMode() {
        // 1. Silent notifications & hide notification banners (Do Not Disturb)
        if (isNotificationPolicyAccessGranted()) {
            val currentFilter = notificationManager.currentInterruptionFilter
            preferences.setSavedInterruptionFilter(currentFilter)
            try {
                notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_NONE)
            } catch (_: Exception) {}
        }

        // 2. Start FocusOverlayService
        val serviceIntent = Intent(context, FocusOverlayService::class.java).apply {
            action = FocusOverlayService.ACTION_START
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent)
        } else {
            context.startService(serviceIntent)
        }

        preferences.setFocusModeActive(true)
    }

    fun stopFocusMode() {
        // 1. Restore normal notifications (everything back to normal)
        if (isNotificationPolicyAccessGranted()) {
            val savedFilter = preferences.getSavedInterruptionFilter()
            try {
                notificationManager.setInterruptionFilter(
                    if (savedFilter == NotificationManager.INTERRUPTION_FILTER_NONE) {
                        NotificationManager.INTERRUPTION_FILTER_ALL
                    } else {
                        savedFilter
                    }
                )
            } catch (_: Exception) {
                try {
                    notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL)
                } catch (_: Exception) {}
            }
        }

        // 2. Stop FocusOverlayService
        val serviceIntent = Intent(context, FocusOverlayService::class.java).apply {
            action = FocusOverlayService.ACTION_STOP
        }
        context.startService(serviceIntent)

        preferences.setFocusModeActive(false)
        preferences.setFocusBreakPhase(false)
    }

    fun updateLimitMonitoring() {
        val serviceIntent = Intent(context, FocusOverlayService::class.java).apply {
            action = FocusOverlayService.ACTION_UPDATE_LIMITS
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent)
        } else {
            context.startService(serviceIntent)
        }
    }
}
