package com.example.service

import android.Manifest
import android.app.KeyguardManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.R
import com.example.data.model.FlashcardItem
import com.example.data.repository.LauncherPreferences

/**
 * Delivers the Study Popups (cards) and handles the answers — plain action buttons only,
 * no free-form reply.
 *
 *   Notification shows the front  ->  "Show" reveals the back with True / False
 *   True  -> locked (never comes back)
 *   False -> returns after 1, 3, 5, 7 days, then locked
 */
class StudyPopupReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_SHOW -> showBack(context, intent.getStringExtra(EXTRA_ID).orEmpty())
            ACTION_TRUE -> mark(context, intent.getStringExtra(EXTRA_ID).orEmpty(), true)
            ACTION_FALSE -> mark(context, intent.getStringExtra(EXTRA_ID).orEmpty(), false)
            else -> deliver(context)
        }
    }

    // ---------------------------------------------------------------- tick

    private fun deliver(context: Context) {
        val prefs = LauncherPreferences(context)
        val settings = prefs.popupSettings.value
        if (!settings.enabled) return

        // Never on the lock screen / a locked phone.
        if (isLocked(context) || !canNotify(context)) {
            StudyPopupScheduler.reschedule(context, settings)
            return
        }

        val disabled = settings.disabledDecks
        val card = prefs.flashcardBank.value.firstOrNull {
            !it.locked && isDue(it) && !disabled.contains(it.deck.ifBlank { "Ungrouped" })
        }
        if (card != null) {
            ensureChannel(context)
            showFront(context, card)
        }
        StudyPopupScheduler.reschedule(context, settings)
    }

    private fun isDue(card: FlashcardItem): Boolean =
        card.nextDueMillis == null || card.nextDueMillis <= System.currentTimeMillis()

    // ------------------------------------------------------------ cards

    private fun showFront(context: Context, card: FlashcardItem) {
        val showPi = actionPi(context, ACTION_SHOW, card.id, 1)
        val notif = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_focus)
            .setContentTitle(if (card.deck.isBlank()) "Card" else card.deck)
            .setContentText(card.front)
            .setStyle(NotificationCompat.BigTextStyle().bigText(card.front))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_SECRET)
            .setAutoCancel(false)
            .addAction(R.drawable.ic_stat_focus, "Show", showPi)
            .build()
        notify(context, NOTIF_CARD, notif)
    }

    private fun showBack(context: Context, id: String) {
        val prefs = LauncherPreferences(context)
        val card = prefs.flashcardBank.value.firstOrNull { it.id == id }
            ?: prefs.flashcardBank.value.firstOrNull { !it.locked }
            ?: return
        val truePi = actionPi(context, ACTION_TRUE, card.id, 2)
        val falsePi = actionPi(context, ACTION_FALSE, card.id, 3)
        val notif = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_focus)
            .setContentTitle("Answer")
            .setContentText(card.back)
            .setStyle(NotificationCompat.BigTextStyle().bigText(card.back))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_SECRET)
            .setAutoCancel(false)
            .addAction(R.drawable.ic_stat_focus, "True", truePi)
            .addAction(R.drawable.ic_stat_focus, "False", falsePi)
            .build()
        notify(context, NOTIF_CARD, notif)
    }

    private fun mark(context: Context, id: String, correct: Boolean) {
        val prefs = LauncherPreferences(context)
        val card = prefs.flashcardBank.value.firstOrNull { it.id == id }
            ?: prefs.flashcardBank.value.firstOrNull { !it.locked }
            ?: return
        val updated = if (correct) {
            card.copy(locked = true, masteredAtMillis = System.currentTimeMillis())
        } else {
            val stage = card.repeatStage + 1
            if (stage > REPEAT_DAYS.size) {
                card.copy(locked = true, repeatStage = stage, nextDueMillis = null)
            } else {
                card.copy(
                    repeatStage = stage,
                    nextDueMillis = System.currentTimeMillis() + REPEAT_DAYS[stage - 1] * DAY_MS
                )
            }
        }
        prefs.saveFlashcardBank(prefs.flashcardBank.value.map { if (it.id == card.id) updated else it })
        NotificationManagerCompat.from(context).cancel(NOTIF_CARD)
    }

    // ---------------------------------------------------------------- helpers

    private fun actionPi(context: Context, action: String, id: String, code: Int): PendingIntent {
        val intent = Intent(context, StudyPopupReceiver::class.java).apply {
            this.action = action
            putExtra(EXTRA_ID, id)
        }
        return PendingIntent.getBroadcast(
            context, (id + action).hashCode() + code, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun notify(context: Context, id: Int, notif: android.app.Notification) {
        try {
            NotificationManagerCompat.from(context).notify(id, notif)
        } catch (_: SecurityException) {
            // Permission revoked between the check and the notify — nothing to do.
        }
    }

    private fun isLocked(context: Context): Boolean {
        val km = context.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
        return km?.isKeyguardLocked == true
    }

    private fun canNotify(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED

    private fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(NotificationManager::class.java) ?: return
            if (manager.getNotificationChannel(CHANNEL_ID) == null) {
                val channel = NotificationChannel(
                    CHANNEL_ID, "Study Popups", NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Study card questions"
                    lockscreenVisibility = android.app.Notification.VISIBILITY_SECRET
                }
                manager.createNotificationChannel(channel)
            }
        }
    }

    companion object {
        const val CHANNEL_ID = "study_popups_channel"
        const val EXTRA_ID = "sp_id"

        const val ACTION_SHOW = "com.example.sp_launcher.ACTION_CARD_SHOW"
        const val ACTION_TRUE = "com.example.sp_launcher.ACTION_CARD_TRUE"
        const val ACTION_FALSE = "com.example.sp_launcher.ACTION_CARD_FALSE"

        private const val NOTIF_CARD = 1002
        private const val DAY_MS = 86_400_000L
        private val REPEAT_DAYS = listOf(1, 3, 5, 7)
    }
}
