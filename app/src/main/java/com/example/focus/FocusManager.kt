package com.example.focus

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import com.example.data.repository.LauncherPreferences
import com.example.service.FocusOverlayService

class FocusManager(private val context: Context) {

    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private val preferences = LauncherPreferences(context)
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager

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
        // 1. Remember the phone's sound state, then silence it for the study block
        silenceForStudy()

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
        // 1. Put the phone's sound state back exactly as it was before the study block
        restoreAudio()

        // 2. Stop FocusOverlayService
        val serviceIntent = Intent(context, FocusOverlayService::class.java).apply {
            action = FocusOverlayService.ACTION_STOP
        }
        context.startService(serviceIntent)

        preferences.setFocusModeActive(false)
        preferences.setFocusBreakPhase(false)
    }

    /**
     * Saves the phone's current sound state once, then silences it (Do Not Disturb + silent
     * ringer) for a study block. Both Focus Mode and the overlay service go through here, so
     * they can no longer disagree about what to restore.
     */
    fun silenceForStudy() {
        if (!preferences.isAudioStateSaved()) {
            preferences.setSavedInterruptionFilter(
                try {
                    notificationManager.currentInterruptionFilter
                } catch (_: Exception) {
                    NotificationManager.INTERRUPTION_FILTER_ALL
                }
            )
            preferences.setSavedRingerMode(
                try {
                    audioManager?.ringerMode ?: AudioManager.RINGER_MODE_NORMAL
                } catch (_: Exception) {
                    AudioManager.RINGER_MODE_NORMAL
                }
            )
            preferences.setAudioStateSaved(true)
        }

        try {
            if (notificationManager.isNotificationPolicyAccessGranted) {
                notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_NONE)
            }
        } catch (_: Exception) {}
        try {
            audioManager?.ringerMode = AudioManager.RINGER_MODE_SILENT
        } catch (_: Exception) {}
    }

    /**
     * Restores exactly the sound state saved by [silenceForStudy] - so a phone that was
     * already on vibrate or already in Do Not Disturb ends up back the way it was.
     */
    fun restoreAudio() {
        if (!preferences.isAudioStateSaved()) return

        try {
            if (notificationManager.isNotificationPolicyAccessGranted) {
                notificationManager.setInterruptionFilter(preferences.getSavedInterruptionFilter())
            }
        } catch (_: Exception) {}

        try {
            val savedMode = preferences.getSavedRingerMode()
            if (savedMode >= 0) audioManager?.ringerMode = savedMode
        } catch (_: Exception) {}

        preferences.setAudioStateSaved(false)
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
