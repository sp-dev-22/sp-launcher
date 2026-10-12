package com.example.util

import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import com.example.data.model.StudyCourse
import com.example.data.model.StudyStatus
import com.example.data.model.allClassRefs
import com.example.data.model.status
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Lightweight exporters for the Study Manager. Text and PDF are produced with framework
 * APIs only (no third-party libraries); the PDF uses [PdfDocument].
 */
object StudyExport {

    private fun date(millis: Long?): String {
        if (millis == null) return "—"
        return SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(millis))
    }

    fun toJson(courses: List<StudyCourse>): String {
        val coursesArr = JSONArray()
        for (course in courses) {
            val courseObj = JSONObject()
            courseObj.put("name", course.name)
            val subjectsArr = JSONArray()
            for (subject in course.subjects) {
                val subjectObj = JSONObject()
                subjectObj.put("name", subject.name)
                val topicsArr = JSONArray()
                for (topic in subject.topics) {
                    val topicObj = JSONObject()
                    topicObj.put("name", topic.name)
                    topicObj.put("deadline", topic.deadlineMillis ?: JSONObject.NULL)
                    topicObj.put("alert", topic.alertMillis ?: JSONObject.NULL)
                    val classesArr = JSONArray()
                    for (clazz in topic.classes) {
                        val classObj = JSONObject()
                        classObj.put("name", clazz.name)
                        classObj.put("completed", clazz.isCompleted)
                        classObj.put("deadline", clazz.deadlineMillis ?: JSONObject.NULL)
                        classObj.put("alert", clazz.alertMillis ?: JSONObject.NULL)
                        classesArr.put(classObj)
                    }
                    topicObj.put("classes", classesArr)
                    topicsArr.put(topicObj)
                }
                subjectObj.put("topics", topicsArr)
                subjectsArr.put(subjectObj)
            }
            courseObj.put("subjects", subjectsArr)
            coursesArr.put(courseObj)
        }
        val root = JSONObject()
        root.put("app", "SP Launcher")
        root.put("section", "studyManager")
        root.put("exportedAt", System.currentTimeMillis())
        root.put("courses", coursesArr)
        return root.toString(2)
    }

    fun toPlainText(courses: List<StudyCourse>): String {
        val sb = StringBuilder()
        sb.append("SP Launcher — Study Manager\n")
        sb.append("Exported: ").append(date(System.currentTimeMillis())).append("\n\n")

        var total = 0
        var done = 0
        var missed = 0
        var pending = 0

        for (course in courses) {
            sb.append("COURSE: ").append(course.name).append("\n")
            for (subject in course.subjects) {
                sb.append("  SUBJECT: ").append(subject.name).append("\n")
                for (topic in subject.topics) {
                    sb.append("    TOPIC: ").append(topic.name)
                    if (topic.deadlineMillis != null) {
                        sb.append("  [deadline: ").append(date(topic.deadlineMillis)).append("]")
                    }
                    sb.append("\n")
                    for (clazz in topic.classes) {
                        total++
                        when (clazz.status()) {
                            StudyStatus.COMPLETED -> done++
                            StudyStatus.MISSED -> missed++
                            StudyStatus.PENDING -> pending++
                        }
                        sb.append("      ").append(if (clazz.isCompleted) "[x]" else "[ ]").append(" ").append(clazz.name)
                        if (clazz.deadlineMillis != null) {
                            sb.append("  (deadline: ").append(date(clazz.deadlineMillis)).append(")")
                        }
                        sb.append("\n")
                    }
                }
            }
        }
        sb.append("\nSummary: ").append(done).append("/").append(total)
            .append(" completed, ").append(missed).append(" missed, ").append(pending).append(" pending\n")
        return sb.toString()
    }

    fun toPdf(courses: List<StudyCourse>): ByteArray {
        val doc = PdfDocument()
        val pageWidth = 595
        val pageHeight = 842
        val margin = 40f
        val lineHeight = 16f
        val headingPaint = Paint().apply {
            color = Color.BLACK
            textSize = 16f
            isFakeBoldText = true
        }
        val bodyPaint = Paint().apply { color = Color.BLACK; textSize = 11f }

        var pageNumber = 1
        var page = doc.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
        var canvas = page.canvas
        var y = margin + 10f

        fun newPage() {
            doc.finishPage(page)
            pageNumber++
            page = doc.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
            canvas = page.canvas
            y = margin + 10f
        }

        fun line(text: String, paint: Paint = bodyPaint) {
            if (y > pageHeight - margin) newPage()
            canvas.drawText(text, margin, y, paint)
            y += lineHeight
        }

        line("SP Launcher — Study Manager", headingPaint)
        line("Exported: ${date(System.currentTimeMillis())}")
        y += lineHeight / 2

        var total = 0
        var done = 0
        var missed = 0
        var pending = 0

        for (course in courses) {
            line(course.name, headingPaint)
            for (subject in course.subjects) {
                line("  ${subject.name}")
                for (topic in subject.topics) {
                    var topicLine = "    ${topic.name}"
                    if (topic.deadlineMillis != null) topicLine += "   [deadline: ${date(topic.deadlineMillis)}]"
                    line(topicLine)
                    for (clazz in topic.classes) {
                        total++
                        when (clazz.status()) {
                            StudyStatus.COMPLETED -> done++
                            StudyStatus.MISSED -> missed++
                            StudyStatus.PENDING -> pending++
                        }
                        var classLine = "        ${if (clazz.isCompleted) "[x]" else "[ ]"} ${clazz.name}"
                        if (clazz.deadlineMillis != null) classLine += "  (due ${date(clazz.deadlineMillis)})"
                        line(classLine)
                    }
                }
            }
        }

        y += lineHeight / 2
        line("Summary: $done/$total completed, $missed missed, $pending pending")

        doc.finishPage(page)
        val out = ByteArrayOutputStream()
        doc.writeTo(out)
        doc.close()
        return out.toByteArray()
    }
}
