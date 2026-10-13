package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.data.repository.LauncherPreferences
import com.example.focus.UsageTracker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Keeps the daily app limits honest while the launcher is not in the foreground.
 *
 * The launcher itself checks a limit the moment you tap an app (see
 * LauncherViewModel.tryLaunchApp), but apps opened from anywhere else would otherwise slip
 * through. This lightweight foreground service watches the foreground app and, once a limit is
 * reached, drops a full-screen overlay that covers the app until you go home.
 */
class AppLimitService : Service() {

    private var windowManager: WindowManager? = null
    private var appBlockOverlayView: View? = null

    private var appMonitorJob: Job? = null
    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())

    private lateinit var preferences: LauncherPreferences
    private lateinit var usageTracker: UsageTracker

    override fun onCreate() {
        super.onCreate()
        preferences = LauncherPreferences(this)
        usageTracker = UsageTracker(this)
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (preferences.appLimits.value.isEmpty()) {
            stopSelf()
            return START_NOT_STICKY
        }
        startForeground(NOTIFICATION_ID, buildNotification("SP Launcher: App Limits Active"))
        startAppMonitor()
        return START_STICKY
    }

    /**
     * Real-time App Limit Monitoring Loop.
     * Evaluates the foreground app every 1.5 seconds and immediately hides/blocks
     * apps that have reached their daily limit.
     */
    private fun startAppMonitor() {
        appMonitorJob?.cancel()
        appMonitorJob = serviceScope.launch(Dispatchers.IO) {
            while (isActive) {
                delay(1500L)
                checkAndEnforceForegroundApp()
            }
        }
    }

    private suspend fun checkAndEnforceForegroundApp() {
        val limits = preferences.appLimits.value
        if (limits.isEmpty()) {
            withContext(Dispatchers.Main) { stopSelf() }
            return
        }
        if (!usageTracker.hasUsageStatsPermission()) return

        val topPackage = usageTracker.getForegroundPackage() ?: return
        if (topPackage == packageName ||
            topPackage == "com.android.systemui" ||
            topPackage.startsWith("com.google.android.inputmethod")
        ) {
            return
        }

        val usageMap = usageTracker.getTodayAppUsageMinutes()
        val usedMinutes = usageMap[topPackage] ?: 0
        val limitMinutes = limits[topPackage] ?: 0

        // Daily limit reached while the app is running -> lock it for today.
        if (limitMinutes in 1..usedMinutes) {
            val appLabel = getAppLabel(topPackage)
            withContext(Dispatchers.Main) {
                showLiveAppBlockOverlay(
                    title = "Time's Up! Daily Limit Reached",
                    message = "You've used $appLabel for $usedMinutes min today (daily limit: $limitMinutes min).\nThis app is now locked for today."
                )
            }
        }
    }

    private fun getAppLabel(pkg: String): String {
        return try {
            val pm = packageManager
            val info = pm.getApplicationInfo(pkg, 0)
            pm.getApplicationLabel(info).toString()
        } catch (_: Exception) {
            pkg
        }
    }

    /**
     * Full-screen blocking pop-up that completely covers and hides the running app.
     * Prevents touches from reaching the app underneath.
     */
    private fun showLiveAppBlockOverlay(title: String, message: String) {
        if (!Settings.canDrawOverlays(this) || appBlockOverlayView != null) return

        val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            layoutType,
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_FULLSCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.CENTER
        }

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(Color.parseColor("#F5121216"))
            setPadding(48, 48, 48, 48)
            isClickable = true
            isFocusable = true
        }

        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            val bg = GradientDrawable().apply {
                setColor(Color.parseColor("#1E1E24"))
                cornerRadius = 36f
                setStroke(3, Color.parseColor("#E53935"))
            }
            background = bg
            setPadding(48, 40, 48, 40)
        }

        val titleView = TextView(this).apply {
            text = title
            setTextColor(Color.parseColor("#FF5252"))
            textSize = 20f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            gravity = Gravity.CENTER
        }
        card.addView(titleView)

        val msgView = TextView(this).apply {
            text = message
            setTextColor(Color.WHITE)
            textSize = 14f
            gravity = Gravity.CENTER
            setPadding(0, 18, 0, 28)
        }
        card.addView(msgView)

        val homeBtn = Button(this).apply {
            text = "Return to Home Screen"
            setBackgroundColor(Color.parseColor("#E53935"))
            setTextColor(Color.WHITE)
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            setPadding(36, 16, 36, 16)
            setOnClickListener {
                dismissLiveAppBlockOverlay()
                val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                    addCategory(Intent.CATEGORY_HOME)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                startActivity(homeIntent)
            }
        }
        card.addView(homeBtn)
        root.addView(card)

        appBlockOverlayView = root
        try {
            windowManager?.addView(appBlockOverlayView, params)
        } catch (_: Exception) {}
    }

    private fun dismissLiveAppBlockOverlay() {
        appBlockOverlayView?.let {
            try {
                windowManager?.removeView(it)
            } catch (_: Exception) {}
            appBlockOverlayView = null
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        appMonitorJob?.cancel()
        dismissLiveAppBlockOverlay()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "SP Launcher App Limits",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps your daily app limits enforced"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(contentText: String): Notification {
        val openIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("SP Launcher")
            .setContentText(contentText)
            .setSmallIcon(R.drawable.ic_stat_focus)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    companion object {
        const val ACTION_START = "com.example.service.ACTION_START_LIMITS"
        private const val CHANNEL_ID = "app_limits_channel"
        private const val NOTIFICATION_ID = 1001

        /** Starts (or nudges) the limit monitor. Safe to call repeatedly. */
        fun start(context: Context) {
            val intent = Intent(context, AppLimitService::class.java).apply {
                action = ACTION_START
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }
}
