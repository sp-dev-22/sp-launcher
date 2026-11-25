package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import com.example.MainActivity
import com.example.ui.components.AvatarFace
import com.example.ui.components.AvatarMood
import com.example.ui.theme.FocusLauncherTheme
import kotlin.math.abs

/**
 * Floating AI avatar (v1.20.0): a draggable bubble that sits over ANY app.
 *
 * The service only hosts the view — no polling, no timers, no battery drain
 * beyond the one breathing animation inside the bubble itself. Tap the bubble
 * to open the AI chat (MainActivity handles the "open_ai_chat" extra);
 * the persistent notification has a "Band karo" action that stops the service.
 *
 * Requires the SYSTEM_ALERT_WINDOW permission (the user grants it from
 * Settings > Display over other apps) and the manifest entries listed in
 * the coordinator notes — without them the service cannot start on
 * Android 14+, where a foreground-service type is mandatory.
 */
class FloatingAvatarService : Service() {

    companion object {
        private const val TAG = "FloatingAvatar"
        private const val CHANNEL_ID = "avatar_channel"
        private const val NOTIFICATION_ID = 1001

        /** Sent to MainActivity so it opens the AI chat. */
        const val ACTION_OPEN_CHAT = "com.example.action.OPEN_CHAT"

        /** Extra on the MainActivity intent: open the AI chat on launch. */
        const val EXTRA_OPEN_AI_CHAT = "open_ai_chat"

        private const val ACTION_STOP_AVATAR = "com.example.action.STOP_AVATAR"

        @Volatile
        private var running = false

        /** Start the bubble. Silently does nothing without the overlay permission. */
        fun start(context: Context) {
            if (running) return
            if (!Settings.canDrawOverlays(context)) return
            try {
                val intent = Intent(context, FloatingAvatarService::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: SecurityException) {
                Log.w(TAG, "Cannot start floating avatar", e)
            } catch (e: IllegalStateException) {
                // Foreground-service start not allowed from the background (API 31+).
                Log.w(TAG, "Cannot start floating avatar from background", e)
            } catch (e: Exception) {
                Log.w(TAG, "Cannot start floating avatar", e)
            }
        }

        /** Stop the bubble, if it is running. */
        fun stop(context: Context) {
            try {
                context.stopService(Intent(context, FloatingAvatarService::class.java))
            } catch (e: Exception) {
                Log.w(TAG, "Cannot stop floating avatar", e)
            }
        }

        fun isRunning(context: Context): Boolean = running
    }

    private var windowManager: WindowManager? = null
    private var bubbleView: ComposeView? = null
    private var bubbleParams: WindowManager.LayoutParams? = null
    private var lifecycleOwner: OverlayLifecycleOwner? = null
    private var viewModelStoreOwner: OverlayViewModelStoreOwner? = null

    override fun onCreate() {
        super.onCreate()
        running = true
        try {
            createChannel()
            startForegroundCompat()
            showBubble()
        } catch (e: SecurityException) {
            Log.w(TAG, "Overlay permission lost; stopping", e)
            stopSelf()
        } catch (e: Exception) {
            Log.w(TAG, "Could not show floating avatar", e)
            stopSelf()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP_AVATAR) {
            stopSelf()
            return START_NOT_STICKY
        }
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        try {
            bubbleView?.let { windowManager?.removeView(it) }
        } catch (e: Exception) {
            Log.w(TAG, "Could not remove bubble view", e)
        }
        try {
            lifecycleOwner?.onDestroy()
        } catch (e: Exception) {
            // Lifecycle teardown must never crash the service.
        }
        try {
            viewModelStoreOwner?.viewModelStore?.clear()
        } catch (e: Exception) {
            // Same: never crash on teardown.
        }
        bubbleView = null
        bubbleParams = null
        lifecycleOwner = null
        viewModelStoreOwner = null
        windowManager = null
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                stopForeground(Service.STOP_FOREGROUND_REMOVE)
            } else {
                @Suppress("DEPRECATION")
                stopForeground(true)
            }
        } catch (e: Exception) {
            // Already stopped; ignore.
        }
        running = false
        super.onDestroy()
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = getSystemService(NotificationManager::class.java) ?: return
        val channel = NotificationChannel(
            CHANNEL_ID,
            "AI Avatar",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "AI Avatar bubble ke liye notification"
        }
        try {
            manager.createNotificationChannel(channel)
        } catch (e: Exception) {
            Log.w(TAG, "Could not create avatar channel", e)
        }
    }

    private fun buildNotification(): Notification {
        val stopIntent = Intent(this, FloatingAvatarService::class.java).apply {
            action = ACTION_STOP_AVATAR
        }
        val stopPending = PendingIntent.getService(
            this,
            0,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, CHANNEL_ID)
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
        }
        return builder
            .setContentTitle("AI Avatar")
            .setContentText("AI Avatar chal raha hai — band karne ke liye tap karo")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setOngoing(true)
            .setContentIntent(stopPending)
            // Deprecated icon overload is deliberate: the Builder(Icon, …)
            // form needs a non-null Icon, and a system delete icon is fine here.
            .addAction(android.R.drawable.ic_delete, "Band karo", stopPending)
            .build()
    }

    private fun startForegroundCompat() {
        val notification = buildNotification()
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                )
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        } catch (e: SecurityException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "startForeground failed", e)
            throw e
        }
    }

    private fun showBubble() {
        val wm = getSystemService(WINDOW_SERVICE) as? WindowManager ?: return
        windowManager = wm
        if (!Settings.canDrawOverlays(this)) {
            throw SecurityException("Overlay permission not granted")
        }

        val density = resources.displayMetrics.density
        val bubblePx = (64.dp.value * density).toInt()
        val marginPx = (12.dp.value * density).toInt()
        val screenWidth = resources.displayMetrics.widthPixels
        val screenHeight = resources.displayMetrics.heightPixels

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            },
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            // Right edge, vertically centred.
            x = screenWidth - bubblePx - marginPx
            y = screenHeight / 2 - bubblePx / 2
        }
        bubbleParams = params

        // A ComposeView outside an Activity has no lifecycle/view-model owners
        // of its own, so give it lightweight ones (standard overlay pattern).
        val owner = OverlayLifecycleOwner().also { it.onCreate(); it.onStart(); it.onResume() }
        val storeOwner = OverlayViewModelStoreOwner()
        lifecycleOwner = owner
        viewModelStoreOwner = storeOwner

        val view = ComposeView(this).apply {
            setViewTreeLifecycleOwner(owner)
            setViewTreeSavedStateRegistryOwner(owner)
            setViewTreeViewModelStoreOwner(storeOwner)
            setContent {
                FocusLauncherTheme(darkTheme = isSystemInDarkTheme()) {
                    AvatarFace(
                        mood = AvatarMood.IDLE,
                        modifier = Modifier.size(64.dp)
                    )
                }
            }
        }
        view.setOnTouchListener(BubbleTouchListener(wm, params, density) { openChat() })
        bubbleView = view

        wm.addView(view, params)
    }

    /** Tap the bubble -> open the AI chat in MainActivity. */
    private fun openChat() {
        try {
            val intent = Intent(this, MainActivity::class.java).apply {
                action = ACTION_OPEN_CHAT
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra(EXTRA_OPEN_AI_CHAT, true)
            }
            startActivity(intent)
        } catch (e: Exception) {
            Log.w(TAG, "Could not open AI chat", e)
        }
    }

    /**
     * Drag the bubble with one finger; a short tap (under ~10 dp of movement)
     * opens the chat instead of moving it.
     */
    private class BubbleTouchListener(
        private val windowManager: WindowManager,
        private val params: WindowManager.LayoutParams,
        private val density: Float,
        private val onTap: () -> Unit
    ) : View.OnTouchListener {
        private var downRawX = 0f
        private var downRawY = 0f
        private var downX = 0
        private var downY = 0
        private var moved = false

        override fun onTouch(view: View, event: MotionEvent): Boolean {
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    downRawX = event.rawX
                    downRawY = event.rawY
                    downX = params.x
                    downY = params.y
                    moved = false
                    return true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX - downRawX
                    val dy = event.rawY - downRawY
                    if (abs(dx) > 10f * density || abs(dy) > 10f * density) {
                        moved = true
                    }
                    params.x = downX + dx.toInt()
                    params.y = downY + dy.toInt()
                    try {
                        windowManager.updateViewLayout(view, params)
                    } catch (e: Exception) {
                        Log.w(TAG, "Could not move bubble", e)
                    }
                    return true
                }
                MotionEvent.ACTION_UP -> {
                    if (!moved) {
                        onTap()
                    }
                    return true
                }
                MotionEvent.ACTION_CANCEL -> {
                    return true
                }
            }
            return false
        }
    }

    /** Minimal lifecycle + saved-state owner for a ComposeView hosted in a Service. */
    private class OverlayLifecycleOwner : LifecycleOwner, SavedStateRegistryOwner {
        private val lifecycleRegistry = LifecycleRegistry(this)
        private val savedStateRegistryController = SavedStateRegistryController.create(this)

        override val lifecycle: Lifecycle get() = lifecycleRegistry
        override val savedStateRegistry: SavedStateRegistry
            get() = savedStateRegistryController.savedStateRegistry

        fun onCreate() {
            savedStateRegistryController.performAttach()
            savedStateRegistryController.performRestore(null)
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        }

        fun onStart() {
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        }

        fun onResume() {
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
        }

        fun onDestroy() {
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        }
    }

    /** Minimal ViewModelStore owner for a ComposeView hosted in a Service. */
    private class OverlayViewModelStoreOwner : ViewModelStoreOwner {
        override val viewModelStore: ViewModelStore = ViewModelStore()
    }
}
