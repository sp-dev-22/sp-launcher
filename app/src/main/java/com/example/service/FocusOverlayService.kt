package com.example.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.media.AudioManager
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
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

class FocusOverlayService : Service() {

    private var windowManager: WindowManager? = null
    private var notificationManager: NotificationManager? = null
    private var audioManager: AudioManager? = null
    private var vibrator: Vibrator? = null

    private var overlayView: View? = null
    private var confirmDialogView: View? = null
    private var appBlockOverlayView: View? = null

    private var timerJob: Job? = null
    private var appMonitorJob: Job? = null
    private var longPressCheckJob: Job? = null
    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())

    private lateinit var preferences: LauncherPreferences
    private lateinit var usageTracker: UsageTracker

    private var isStudying = true
    private var currentSession = 1
    private var isPaused = false
    private var secondsRemaining = 25 * 60

    private var studyMinutes = 25
    private var breakMinutes = 5
    private var longBreakMinutes = 15
    private var targetSessions = 4

    private var initialX = 0
    private var initialY = 0
    private var initialTouchX = 0f
    private var initialTouchY = 0f
    private var touchDownTime = 0L
    private var isLongPressTriggered = false

    override fun onCreate() {
        super.onCreate()
        preferences = LauncherPreferences(this)
        usageTracker = UsageTracker(this)
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        audioManager = getSystemService(Context.AUDIO_SERVICE) as? AudioManager

        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }

        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: ACTION_START

        when (action) {
            ACTION_STOP -> {
                stopFocusSession()
                // If there are still app limits, keep monitor alive quietly
                if (preferences.appLimits.value.isEmpty()) {
                    stopSelf()
                    return START_NOT_STICKY
                } else {
                    startAppMonitor()
                    return START_STICKY
                }
            }
            ACTION_UPDATE_LIMITS -> {
                // Background limit monitoring without timer overlay
                if (!preferences.isFocusModeActive.value) {
                    removeOverlayView()
                    startForeground(NOTIFICATION_ID, buildMonitorNotification("SP Launcher: App Limits Active"))
                }
                startAppMonitor()
                return START_STICKY
            }
            ACTION_START -> {
                studyMinutes = preferences.studyDurationMinutes.value
                breakMinutes = preferences.breakDurationMinutes.value
                longBreakMinutes = preferences.longBreakDurationMinutes.value
                targetSessions = preferences.targetSessions.value

                // A null intent means the system restarted us (START_STICKY), not a user start.
                val isSystemRestart = intent == null

                if (isSystemRestart && !preferences.isFocusModeActive.value) {
                    // We were only running to enforce app limits. Don't start a focus session.
                    removeOverlayView()
                    startForeground(NOTIFICATION_ID, buildMonitorNotification("SP Launcher: App Limits Active"))
                    startAppMonitor()
                    return START_STICKY
                }

                val canResume = isSystemRestart &&
                        preferences.isFocusModeActive.value &&
                        preferences.getFocusSecondsRemaining() >= 0

                if (canResume) {
                    // Resume the in-progress session instead of resetting the timer.
                    isStudying = preferences.getFocusIsStudying()
                    currentSession = preferences.getFocusCurrentSession()
                    secondsRemaining = preferences.getFocusSecondsRemaining()
                    isPaused = false
                } else {
                    isStudying = true
                    currentSession = 1
                    isPaused = false
                    secondsRemaining = studyMinutes * 60
                }

                preferences.setFocusModeActive(true)
                preferences.setFocusBreakPhase(!isStudying)

                // Silence notifications & ringer only while studying
                if (isStudying) {
                    silencePhone()
                }

                startForeground(NOTIFICATION_ID, buildNotification("Focus Mode: ${formatTime(secondsRemaining)}"))
                showOverlayView()
                startTimer()
                startAppMonitor()
                persistTimerState()
                return START_STICKY
            }
            else -> return START_STICKY
        }
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = serviceScope.launch {
            while (isActive) {
                delay(1000L)
                if (!isPaused && preferences.isFocusModeActive.value) {
                    if (secondsRemaining > 0) {
                        secondsRemaining--
                        updateOverlayText()
                        // Persist periodically so a restart can resume close to where we were.
                        if (secondsRemaining % 5 == 0) persistTimerState()
                    } else {
                        onPhaseCompleted()
                    }
                }
            }
        }
    }

    private fun onPhaseCompleted() {
        if (isStudying) {
            // Completed a study session -> Transition to BREAK
            preferences.recordCompletedFocusSession(studyMinutes, "Study Session")
            isStudying = false
            preferences.setFocusBreakPhase(true)

            // During Break: Un-silent phone & unhide/unlock all apps!
            unSilencePhone()
            dismissLiveAppBlockOverlay()

            val isLongBreak = currentSession >= targetSessions
            val duration = if (isLongBreak) longBreakMinutes else breakMinutes
            secondsRemaining = duration * 60
        } else {
            // Completed a break session -> Transition back to STUDY
            isStudying = true
            preferences.setFocusBreakPhase(false)

            // Re-silent phone for Study phase
            silencePhone()

            if (currentSession >= targetSessions) {
                currentSession = 1
            } else {
                currentSession++
            }
            secondsRemaining = studyMinutes * 60
        }
        updateOverlayText()
        persistTimerState()
    }

    private fun silencePhone() {
        try {
            if (notificationManager?.isNotificationPolicyAccessGranted == true) {
                notificationManager?.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_NONE)
            }
            audioManager?.ringerMode = AudioManager.RINGER_MODE_SILENT
        } catch (_: Exception) {}
    }

    private fun unSilencePhone() {
        try {
            if (notificationManager?.isNotificationPolicyAccessGranted == true) {
                notificationManager?.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL)
            }
            audioManager?.ringerMode = AudioManager.RINGER_MODE_NORMAL
        } catch (_: Exception) {}
    }

    /**
     * Real-time App Limit & Restricted App Monitoring Loop.
     * Evaluates foreground app every 1.5 seconds and immediately hides/blocks
     * apps that have reached their limit or are restricted.
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
        if (!usageTracker.hasUsageStatsPermission()) return

        val topPackage = usageTracker.getForegroundPackage() ?: return
        if (topPackage == packageName ||
            topPackage == "com.android.systemui" ||
            topPackage.startsWith("com.google.android.inputmethod")
        ) {
            return
        }

        val limits = preferences.appLimits.value
        val usageMap = usageTracker.getTodayAppUsageMinutes()
        val usedMinutes = usageMap[topPackage] ?: 0
        val limitMinutes = limits[topPackage] ?: 0

        // 1. Check if daily limit is reached while app is running
        if (limitMinutes in 1..usedMinutes) {
            val appLabel = getAppLabel(topPackage)
            withContext(Dispatchers.Main) {
                showLiveAppBlockOverlay(
                    pkg = topPackage,
                    title = "Time's Up! Daily Limit Reached",
                    message = "You've used $appLabel for $usedMinutes min today (daily limit: $limitMinutes min).\nThis app is now locked for today."
                )
            }
            return
        }

        // 2. Check if Study Focus mode is active (not break phase) and app is not allowed
        if (isStudying && preferences.isFocusModeActive.value && !preferences.isFocusBreakPhase.value) {
            val allowedApps = preferences.allowedFocusApps.value
            if (!allowedApps.contains(topPackage)) {
                val appLabel = getAppLabel(topPackage)
                withContext(Dispatchers.Main) {
                    showLiveAppBlockOverlay(
                        pkg = topPackage,
                        title = "Focus Mode Active",
                        message = "$appLabel is locked during your study session to help you concentrate."
                    )
                }
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
    private fun showLiveAppBlockOverlay(pkg: String, title: String, message: String) {
        if (!Settings.canDrawOverlays(this) || appBlockOverlayView != null) return

        val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        // Must intercept all touch events, covering full screen completely
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
            setBackgroundColor(Color.parseColor("#F5121216")) // Near solid dark scrim
            setPadding(48, 48, 48, 48)
            // Intercept touches so nothing passes to background app
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

    /**
     * Floating Pomodoro Timer Over other apps (top left corner).
     * Supports:
     * - Dragging to position
     * - Short tap (< 400ms): pause/resume
     * - Long press (3 seconds): triggers confirmation dialog to end session early!
     */
    @SuppressLint("ClickableViewAccessibility")
    private fun showOverlayView() {
        if (!Settings.canDrawOverlays(this) || overlayView != null) return

        val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 24
            y = 90
        }

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            val bg = GradientDrawable().apply {
                setColor(Color.parseColor("#E6121214"))
                cornerRadius = 24f
                setStroke(2, Color.parseColor("#33FFFFFF"))
            }
            background = bg
            setPadding(24, 12, 24, 12)
        }

        val timerText = TextView(this).apply {
            id = View.generateViewId()
            setTextColor(Color.WHITE)
            textSize = 13f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            text = getStatusString()
        }
        container.addView(timerText)

        // Touch Listener: Drag + Tap (Pause) + 3-SECOND LONG PRESS (Confirm Close)
        container.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    touchDownTime = System.currentTimeMillis()
                    isLongPressTriggered = false

                    // 3-second countdown for long press
                    longPressCheckJob?.cancel()
                    longPressCheckJob = serviceScope.launch {
                        delay(1000L)
                        timerText.text = "Hold 2s to close..."
                        delay(1000L)
                        timerText.text = "Hold 1s to close..."
                        delay(1000L)
                        // 3 seconds held!
                        isLongPressTriggered = true
                        triggerVibration()
                        timerText.text = getStatusString()
                        showConfirmCloseDialog()
                    }
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.rawX - initialTouchX).toInt()
                    val dy = (event.rawY - initialTouchY).toInt()

                    if (Math.abs(dx) > 18 || Math.abs(dy) > 18) {
                        // Cancel 3-second timer if moving
                        longPressCheckJob?.cancel()
                        timerText.text = getStatusString()
                    }

                    params.x = (initialX + dx).coerceAtLeast(0)
                    params.y = (initialY + dy).coerceAtLeast(0)
                    windowManager?.updateViewLayout(overlayView, params)
                    true
                }
                MotionEvent.ACTION_UP -> {
                    longPressCheckJob?.cancel()
                    timerText.text = getStatusString()

                    val duration = System.currentTimeMillis() - touchDownTime
                    val diffX = Math.abs(event.rawX - initialTouchX)
                    val diffY = Math.abs(event.rawY - initialTouchY)

                    // If simple tap without long press and without drag, toggle pause
                    if (!isLongPressTriggered && duration < 400 && diffX < 15 && diffY < 15) {
                        isPaused = !isPaused
                        updateOverlayText()
                        persistTimerState()
                    }
                    true
                }
                MotionEvent.ACTION_CANCEL -> {
                    longPressCheckJob?.cancel()
                    timerText.text = getStatusString()
                    true
                }
                else -> false
            }
        }

        overlayView = container
        try {
            windowManager?.addView(overlayView, params)
        } catch (_: Exception) {}
    }

    private fun triggerVibration() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(120, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(120)
            }
        } catch (_: Exception) {}
    }

    /**
     * Confirmation dialog upon 3-second long press.
     * Asks user if they want to end focus mode early.
     */
    private fun showConfirmCloseDialog() {
        if (!Settings.canDrawOverlays(this) || confirmDialogView != null) return

        val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.CENTER
        }

        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            val bg = GradientDrawable().apply {
                setColor(Color.parseColor("#1E1E24"))
                cornerRadius = 28f
                setStroke(2, Color.parseColor("#44FFFFFF"))
            }
            background = bg
            setPadding(40, 32, 40, 32)
        }

        val title = TextView(this).apply {
            text = "End Focus Mode Early?"
            setTextColor(Color.WHITE)
            textSize = 18f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            gravity = Gravity.CENTER
        }
        card.addView(title)

        val desc = TextView(this).apply {
            text = "Ending will un-silent your phone and restore normal launcher apps & settings."
            setTextColor(Color.parseColor("#B0B0B0"))
            textSize = 13f
            gravity = Gravity.CENTER
            setPadding(0, 12, 0, 24)
        }
        card.addView(desc)

        val btnRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }

        val cancelBtn = Button(this).apply {
            text = "Keep Focusing"
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.parseColor("#38383E"))
            setOnClickListener { dismissConfirmCloseDialog() }
        }
        btnRow.addView(cancelBtn)

        val space = View(this).apply { layoutParams = LinearLayout.LayoutParams(20, 1) }
        btnRow.addView(space)

        val endBtn = Button(this).apply {
            text = "End Session"
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.parseColor("#C62828"))
            setOnClickListener {
                dismissConfirmCloseDialog()
                stopFocusSession()
            }
        }
        btnRow.addView(endBtn)
        card.addView(btnRow)

        confirmDialogView = card
        try {
            windowManager?.addView(confirmDialogView, params)
        } catch (_: Exception) {}
    }

    private fun dismissConfirmCloseDialog() {
        confirmDialogView?.let {
            try {
                windowManager?.removeView(it)
            } catch (_: Exception) {}
            confirmDialogView = null
        }
    }

    private fun updateOverlayText() {
        overlayView?.let { root ->
            val textView = (root as? LinearLayout)?.getChildAt(0) as? TextView
            textView?.text = getStatusString()
        }
    }

    private fun getStatusString(): String {
        val phaseLabel = when {
            isPaused -> "Paused"
            !isStudying -> "Break (Apps Unlocked)"
            currentSession >= targetSessions -> "Final Focus"
            else -> "Focus"
        }
        return "$phaseLabel ${formatTime(secondsRemaining)} [$currentSession/$targetSessions]"
    }

    private fun formatTime(seconds: Int): String {
        val m = seconds / 60
        val s = seconds % 60
        return String.format("%02d:%02d", m, s)
    }

    /** Saves the current timer position so a service restart can resume it. */
    private fun persistTimerState() {
        preferences.setFocusSecondsRemaining(secondsRemaining)
        preferences.setFocusIsStudying(isStudying)
        preferences.setFocusCurrentSession(currentSession)
    }

    private fun removeOverlayView() {
        overlayView?.let {
            try {
                windowManager?.removeView(it)
            } catch (_: Exception) {}
            overlayView = null
        }
    }

    private fun stopFocusSession() {
        timerJob?.cancel()
        longPressCheckJob?.cancel()
        dismissConfirmCloseDialog()
        dismissLiveAppBlockOverlay()
        removeOverlayView()

        preferences.setFocusModeActive(false)
        preferences.setFocusBreakPhase(false)
        // Clear the saved timer so the next start begins a fresh session.
        preferences.setFocusSecondsRemaining(-1)

        // Restore normal notifications & ringer
        unSilencePhone()
    }

    override fun onDestroy() {
        super.onDestroy()
        timerJob?.cancel()
        longPressCheckJob?.cancel()
        appMonitorJob?.cancel()
        dismissConfirmCloseDialog()
        dismissLiveAppBlockOverlay()
        removeOverlayView()
        // Restore sound so a destroyed service never leaves the phone silent, but keep the
        // saved timer state so a START_STICKY restart can resume the session. A user-initiated
        // stop goes through ACTION_STOP -> stopFocusSession(), which clears that state.
        unSilencePhone()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "SP Launcher Focus & Limits",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows active Pomodoro focus timer and daily app limits"
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
            .setContentTitle("SP Launcher Focus Mode")
            .setContentText(contentText)
            .setSmallIcon(R.drawable.ic_stat_focus)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun buildMonitorNotification(contentText: String): Notification {
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
            .setOngoing(false)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    companion object {
        const val ACTION_START = "com.example.service.ACTION_START"
        const val ACTION_STOP = "com.example.service.ACTION_STOP"
        const val ACTION_UPDATE_LIMITS = "com.example.service.ACTION_UPDATE_LIMITS"
        private const val CHANNEL_ID = "focus_overlay_channel"
        private const val NOTIFICATION_ID = 1001
    }
}
