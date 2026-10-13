package com.example.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.data.repository.LauncherPreferences

/** Handles the "Mark done" action on the post-session notification (Study <-> Focus link). */
class StudyFocusActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_MARK_STUDY_DONE) return
        val courseId = intent.getStringExtra(EXTRA_COURSE_ID) ?: return
        val subjectId = intent.getStringExtra(EXTRA_SUBJECT_ID) ?: return
        val topicId = intent.getStringExtra(EXTRA_TOPIC_ID) ?: return
        val classId = intent.getStringExtra(EXTRA_CLASS_ID) ?: return
        LauncherPreferences(context).markStudyClassDone(courseId, subjectId, topicId, classId)
    }

    companion object {
        const val ACTION_MARK_STUDY_DONE = "com.example.service.ACTION_MARK_STUDY_DONE"
        const val EXTRA_COURSE_ID = "extra_study_course_id"
        const val EXTRA_SUBJECT_ID = "extra_study_subject_id"
        const val EXTRA_TOPIC_ID = "extra_study_topic_id"
        const val EXTRA_CLASS_ID = "extra_study_class_id"
    }
}
