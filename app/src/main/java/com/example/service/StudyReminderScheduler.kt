package com.example.service

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import com.example.data.model.StudyCourse
import com.example.data.model.allClassRefs

/**
 * Schedules the two levels of Study Manager reminders using [AlarmManager]:
 *
 *  - ALERT    — a gentle "you still have to complete this" nudge.
 *  - DEADLINE — fired after the deadline passes without completion
 *               ("tum itne aalsi ho gaye ho").
 *
 * Uses [AlarmManager.setAndAllowWhileIdle] (inexact but works in Doze) so the app
 * needs no SCHEDULE_EXACT_ALARM permission.
 */
object StudyReminderScheduler {

    const val CHANNEL_ID = "study_reminders_channel"

    const val EXTRA_TITLE = "study_extra_title"
    const val EXTRA_MESSAGE = "study_extra_message"
    const val EXTRA_TYPE = "study_extra_type"
    const val EXTRA_ITEM_ID = "study_extra_item_id"
    const val EXTRA_LEVEL = "study_extra_level"

    const val TYPE_ALERT = "alert"
    const val TYPE_DEADLINE = "deadline"

    const val LEVEL_TOPIC = "topic"
    const val LEVEL_CLASS = "class"

    private const val SCHEME = "spstudy"

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(NotificationManager::class.java) ?: return
            if (manager.getNotificationChannel(CHANNEL_ID) == null) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    "Study Reminders",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Study task alerts and missed-deadline reminders"
                }
                manager.createNotificationChannel(channel)
            }
        }
    }

    private fun alarmManager(context: Context): AlarmManager? =
        context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager

    private fun buildPendingIntent(
        context: Context,
        uniqueKey: String,
        title: String,
        message: String,
        type: String,
        itemId: String,
        level: String
    ): PendingIntent {
        val intent = Intent(context, StudyReminderReceiver::class.java).apply {
            // The data URI makes each alarm unique; extras are ignored for matching.
            data = Uri.parse("$SCHEME://reminder/$uniqueKey")
            putExtra(EXTRA_TITLE, title)
            putExtra(EXTRA_MESSAGE, message)
            putExtra(EXTRA_TYPE, type)
            putExtra(EXTRA_ITEM_ID, itemId)
            putExtra(EXTRA_LEVEL, level)
        }
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        return PendingIntent.getBroadcast(context, uniqueKey.hashCode(), intent, flags)
    }

    private fun cancel(context: Context, uniqueKey: String) {
        val am = alarmManager(context) ?: return
        val pi = buildPendingIntent(context, uniqueKey, "", "", TYPE_ALERT, "", "")
        am.cancel(pi)
        pi.cancel()
    }

    private fun schedule(
        context: Context,
        uniqueKey: String,
        triggerAt: Long,
        title: String,
        message: String,
        type: String,
        itemId: String,
        level: String
    ) {
        if (triggerAt <= System.currentTimeMillis()) return
        val am = alarmManager(context) ?: return
        val pi = buildPendingIntent(context, uniqueKey, title, message, type, itemId, level)
        am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
    }

    /** Cancels and re-schedules every pending study reminder from the current data. */
    fun rescheduleAll(context: Context, courses: List<StudyCourse>) {
        ensureChannel(context)
        val now = System.currentTimeMillis()

        for (course in courses) {
            for (ref in course.allClassRefs()) {
                val topic = ref.topic
                val clazz = ref.clazz

                // ---- Topic-level schedule ----
                val topicBase = "topic_${topic.id}"
                cancel(context, "${topicBase}_alert")
                cancel(context, "${topicBase}_deadline")
                topic.alertMillis?.let { t ->
                    schedule(
                        context, "${topicBase}_alert", t,
                        "Study reminder: ${topic.name}",
                        "Topic \"${topic.name}\" (${course.name} > ${ref.subject.name}) — ise pura karna hai.",
                        TYPE_ALERT, topic.id, LEVEL_TOPIC
                    )
                }
                topic.deadlineMillis?.let { t ->
                    schedule(
                        context, "${topicBase}_deadline", t,
                        "Deadline missed: ${topic.name}",
                        "Aap itne aalsi ho gaye ho 😜 — \"${topic.name}\" ka deadline nikal gaya!",
                        TYPE_DEADLINE, topic.id, LEVEL_TOPIC
                    )
                }

                // ---- Class-level schedule ----
                val classBase = "class_${clazz.id}"
                cancel(context, "${classBase}_alert")
                cancel(context, "${classBase}_deadline")
                clazz.alertMillis?.let { t ->
                    schedule(
                        context, "${classBase}_alert", t,
                        "Study reminder: ${clazz.name}",
                        "\"${clazz.name}\" ko complete karna hai (${course.name} > ${ref.subject.name} > ${topic.name}).",
                        TYPE_ALERT, clazz.id, LEVEL_CLASS
                    )
                }
                clazz.deadlineMillis?.let { t ->
                    schedule(
                        context, "${classBase}_deadline", t,
                        "Deadline missed: ${clazz.name}",
                        "Aap itne aalsi ho gaye ho 😜 — \"${clazz.name}\" ka deadline nikal gaya!",
                        TYPE_DEADLINE, clazz.id, LEVEL_CLASS
                    )
                }
            }
        }
    }

}
