package com.example.ui.screens

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.StudyClass
import com.example.data.model.StudyCourse
import com.example.data.model.StudyStatus
import com.example.data.model.allClassRefs
import com.example.data.model.completedCount
import com.example.data.model.status
import com.example.data.model.totalCount
import com.example.service.StudyReminderScheduler
import com.example.ui.LauncherViewModel
import com.example.ui.components.rememberDragStep
import com.example.util.FileIo
import com.example.util.StudyExport
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private enum class NodeType { COURSE, SUBJECT, TOPIC, CLASS }

private data class StudyRow(
    val type: NodeType,
    val rowKey: String,
    val courseId: String,
    val subjectId: String? = null,
    val topicId: String? = null,
    val classId: String? = null,
    val name: String,
    val level: Int,
    val completed: Int,
    val total: Int,
    val status: StudyStatus = StudyStatus.PENDING,
    val isCompleted: Boolean = false,
    val hasChildren: Boolean = false,
    val isExpanded: Boolean = false,
    val deadlineMillis: Long? = null,
    val alertMillis: Long? = null,
    val studiedMinutes: Int = 0
)

private data class AddTarget(
    val type: NodeType,
    val courseId: String? = null,
    val subjectId: String? = null,
    val topicId: String? = null,
    val label: String
)

private data class ScheduleTarget(
    val level: String,
    val courseId: String,
    val subjectId: String,
    val topicId: String,
    val classId: String? = null,
    val name: String,
    val deadlineMillis: Long?,
    val alertMillis: Long?
)

@Composable
fun StudyManagerScreen(
    viewModel: LauncherViewModel,
    isDarkTheme: Boolean,
    onBack: () -> Unit,
    onOpenTimeTable: () -> Unit
) {
    val courses by viewModel.studyCourses.collectAsState()
    val context = LocalContext.current

    var expanded by remember { mutableStateOf(setOf<String>()) }
    var addTarget by remember { mutableStateOf<AddTarget?>(null) }
    var scheduleTarget by remember { mutableStateOf<ScheduleTarget?>(null) }
    var renameRow by remember { mutableStateOf<StudyRow?>(null) }
    var exportMenu by remember { mutableStateOf(false) }
    val jsonExport = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri -> if (uri != null) FileIo.writeText(context, uri, StudyExport.toJson(courses)) }

    val textExport = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/plain")
    ) { uri -> if (uri != null) FileIo.writeText(context, uri, StudyExport.toPlainText(courses)) }

    val pdfExport = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri -> if (uri != null) FileIo.writeBytes(context, uri, StudyExport.toPdf(courses)) }

    val rows = remember(courses, expanded) { buildStudyRows(courses, expanded) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("study_manager_screen")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack, modifier = Modifier.testTag("study_back_btn")) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
            }
            Text(
                text = "Study Manager",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.weight(1f)
            )
            IconButton(
                onClick = onOpenTimeTable,
                modifier = Modifier.testTag("study_timetable_btn")
            ) {
                Icon(Icons.Default.Schedule, contentDescription = "Quick Time Table")
            }
            IconButton(
                onClick = { addTarget = AddTarget(NodeType.COURSE, label = "Course") },
                modifier = Modifier.testTag("study_add_course_btn")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add course")
            }
            Box {
                IconButton(onClick = { exportMenu = true }, modifier = Modifier.testTag("study_export_btn")) {
                    Icon(Icons.Default.FileDownload, contentDescription = "Export")
                }
                DropdownMenu(expanded = exportMenu, onDismissRequest = { exportMenu = false }) {
                    DropdownMenuItem(
                        text = { Text("Export as JSON") },
                        onClick = { exportMenu = false; jsonExport.launch("sp-launcher-study.json") }
                    )
                    DropdownMenuItem(
                        text = { Text("Export as Text") },
                        onClick = { exportMenu = false; textExport.launch("sp-launcher-study.txt") }
                    )
                    DropdownMenuItem(
                        text = { Text("Export as PDF") },
                        onClick = { exportMenu = false; pdfExport.launch("sp-launcher-study.pdf") }
                    )
                }
            }
        }

        StudyProgressGraph(courses, isDarkTheme, Modifier.fillMaxWidth().padding(horizontal = 16.dp))

        if (courses.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "No courses yet.\nTap + to add your first course.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp)
            ) {
                items(rows, key = { it.rowKey }) { row ->
                    StudyTreeRow(
                        row = row,
                        isDarkTheme = isDarkTheme,
                        onToggleExpand = {
                            expanded = if (expanded.contains(row.rowKey)) expanded - row.rowKey else expanded + row.rowKey
                        },
                        onToggleClass = {
                            val s = row.subjectId
                            val t = row.topicId
                            val c = row.classId
                            if (s != null && t != null && c != null) {
                                viewModel.toggleStudyClass(row.courseId, s, t, c)
                            }
                        },
                        onAddChild = {
                            val childType = when (row.type) {
                                NodeType.COURSE -> NodeType.SUBJECT
                                NodeType.SUBJECT -> NodeType.TOPIC
                                else -> NodeType.CLASS
                            }
                            addTarget = AddTarget(childType, row.courseId, row.subjectId, row.topicId, label = childLabel(row.type))
                        },
                        onSetSchedule = {
                            val s = row.subjectId ?: return@StudyTreeRow
                            val t = row.topicId ?: return@StudyTreeRow
                            scheduleTarget = ScheduleTarget(
                                level = if (row.type == NodeType.TOPIC)
                                    StudyReminderScheduler.LEVEL_TOPIC else StudyReminderScheduler.LEVEL_CLASS,
                                courseId = row.courseId,
                                subjectId = s,
                                topicId = t,
                                classId = row.classId,
                                name = row.name,
                                deadlineMillis = row.deadlineMillis,
                                alertMillis = row.alertMillis
                            )
                        },
                        onEditName = { renameRow = row },
                        onMoveStep = { dir ->
                            when (row.type) {
                                NodeType.COURSE -> viewModel.moveStudyCourse(row.courseId, dir)
                                NodeType.SUBJECT -> row.subjectId?.let { viewModel.moveStudySubject(row.courseId, it, dir) }
                                NodeType.TOPIC -> {
                                    val s = row.subjectId
                                    val t = row.topicId
                                    if (s != null && t != null) viewModel.moveStudyTopic(row.courseId, s, t, dir)
                                }
                                NodeType.CLASS -> {
                                    val s = row.subjectId
                                    val t = row.topicId
                                    val c = row.classId
                                    if (s != null && t != null && c != null) viewModel.moveStudyClass(row.courseId, s, t, c, dir)
                                }
                            }
                        },
                        onDelete = {
                            when (row.type) {
                                NodeType.COURSE -> viewModel.deleteStudyCourse(row.courseId)
                                NodeType.SUBJECT -> row.subjectId?.let { viewModel.deleteStudySubject(row.courseId, it) }
                                NodeType.TOPIC -> {
                                    val s = row.subjectId
                                    val t = row.topicId
                                    if (s != null && t != null) viewModel.deleteStudyTopic(row.courseId, s, t)
                                }
                                NodeType.CLASS -> {
                                    val s = row.subjectId
                                    val t = row.topicId
                                    val c = row.classId
                                    if (s != null && t != null && c != null) viewModel.deleteStudyClass(row.courseId, s, t, c)
                                }
                            }
                        }
                    )
                }
                item { Spacer(modifier = Modifier.height(24.dp)) }
            }
        }
    }

    addTarget?.let { target ->
        AddNameDialog(
            title = "Add ${target.label}",
            onConfirm = { name ->
                when (target.type) {
                    NodeType.COURSE -> viewModel.addStudyCourse(name)
                    NodeType.SUBJECT -> target.courseId?.let { viewModel.addStudySubject(it, name) }
                    NodeType.TOPIC -> {
                        val c = target.courseId
                        val s = target.subjectId
                        if (c != null && s != null) viewModel.addStudyTopic(c, s, name)
                    }
                    NodeType.CLASS -> {
                        val c = target.courseId
                        val s = target.subjectId
                        val t = target.topicId
                        if (c != null && s != null && t != null) viewModel.addStudyClass(c, s, t, name)
                    }
                }
                addTarget = null
            },
            onDismiss = { addTarget = null }
        )
    }

    scheduleTarget?.let { target ->
        ScheduleDialog(
            target = target,
            onSave = { deadline, alert ->
                if (target.level == StudyReminderScheduler.LEVEL_TOPIC) {
                    viewModel.setStudyTopicSchedule(target.courseId, target.subjectId, target.topicId, deadline, alert)
                } else {
                    target.classId?.let {
                        viewModel.setStudyClassSchedule(target.courseId, target.subjectId, target.topicId, it, deadline, alert)
                    }
                }
                scheduleTarget = null
            },
            onDismiss = { scheduleTarget = null }
        )
    }

    renameRow?.let { row ->
        AddNameDialog(
            title = "Rename ${row.type.name.lowercase()}",
            initialValue = row.name,
            confirmLabel = "Save",
            onConfirm = { name ->
                val s = row.subjectId
                val t = row.topicId
                val c = row.classId
                when (row.type) {
                    NodeType.COURSE -> viewModel.renameStudyCourse(row.courseId, name)
                    NodeType.SUBJECT -> if (s != null) viewModel.renameStudySubject(row.courseId, s, name)
                    NodeType.TOPIC -> if (s != null && t != null) viewModel.renameStudyTopic(row.courseId, s, t, name)
                    NodeType.CLASS -> if (s != null && t != null && c != null) viewModel.renameStudyClass(row.courseId, s, t, c, name)
                }
                renameRow = null
            },
            onDismiss = { renameRow = null }
        )
    }

}

private fun formatTimeOfDay(hour: Int, minute: Int): String {
    val cal = java.util.Calendar.getInstance().apply {
        set(java.util.Calendar.HOUR_OF_DAY, hour)
        set(java.util.Calendar.MINUTE, minute)
    }
    return SimpleDateFormat("hh:mm a", Locale.getDefault()).format(cal.time)
}

@Composable
private fun StudyTreeRow(
    row: StudyRow,
    isDarkTheme: Boolean,
    onToggleExpand: () -> Unit,
    onToggleClass: () -> Unit,
    onAddChild: () -> Unit,
    onSetSchedule: () -> Unit,
    onEditName: () -> Unit,
    onDelete: () -> Unit,
    onMoveStep: (Int) -> Unit
) {
    val accent = statusColor(row.status)
    var menuOpen by remember { mutableStateOf(false) }
    val stepPx = with(LocalDensity.current) { 56.dp.toPx() }
    val drag = rememberDragStep(stepPx, onMoveStep)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer { translationY = drag.offset.value }
            .padding(start = (4 + row.level * 18).dp, end = 4.dp, top = 3.dp, bottom = 3.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable {
                if (row.type == NodeType.CLASS) onToggleClass() else onToggleExpand()
            }
            .padding(horizontal = 10.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (row.type == NodeType.CLASS) {
            Icon(
                imageVector = if (row.isCompleted) Icons.Default.CheckCircle else Icons.Outlined.Circle,
                contentDescription = null,
                tint = if (row.isCompleted) Color(0xFF4CAF50) else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                modifier = Modifier.size(22.dp)
            )
        } else {
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(accent.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (row.isExpanded) "-" else "+",
                    color = accent,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        }

        Spacer(modifier = Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = row.name,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontSize = 15.sp,
                    textDecoration = if (row.type == NodeType.CLASS && row.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                ),
                color = if (row.type == NodeType.CLASS && row.isCompleted)
                    MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                else MaterialTheme.colorScheme.onBackground,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            if (row.deadlineMillis != null) {
                Text(
                    text = "Deadline: ${formatDateTime(row.deadlineMillis)}",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = accent
                )
            }
            if (row.alertMillis != null) {
                Text(
                    text = "Reminder: ${formatDateTime(row.alertMillis)}",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = MaterialTheme.colorScheme.primary
                )
            }
            if (row.studiedMinutes > 0) {
                Text(
                    text = "${row.studiedMinutes} min studied",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f)
                )
            }
        }

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(accent.copy(alpha = 0.16f))
                .padding(horizontal = 8.dp, vertical = 3.dp)
        ) {
            Text(
                text = if (row.type == NodeType.CLASS) {
                    if (row.isCompleted) "done" else "todo"
                } else {
                    "${row.completed}/${row.total}"
                },
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = accent
            )
        }

        Icon(
            imageVector = Icons.Default.DragHandle,
            contentDescription = "Drag to reorder",
            tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.45f),
            modifier = Modifier
                .size(24.dp)
                .then(drag.modifier)
                .testTag("study_drag_${row.rowKey}")
        )

        Box {
            IconButton(onClick = { menuOpen = true }, modifier = Modifier.size(30.dp)) {
                Icon(Icons.Default.MoreVert, contentDescription = "Options", modifier = Modifier.size(18.dp))
            }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                DropdownMenuItem(
                    text = { Text("Edit name") },
                    onClick = { menuOpen = false; onEditName() }
                )
                // "Add <child>" is shown for every level, including Class, so a long
                // list of classes can be extended without scrolling back to the Topic.
                DropdownMenuItem(
                    text = { Text("Add ${childLabel(row.type)}") },
                    onClick = { menuOpen = false; onAddChild() }
                )
                if (row.type == NodeType.TOPIC || row.type == NodeType.CLASS) {
                    DropdownMenuItem(
                        text = { Text("Deadline & Reminder") },
                        onClick = { menuOpen = false; onSetSchedule() }
                    )
                }
                DropdownMenuItem(
                    text = { Text("Delete") },
                    onClick = { menuOpen = false; onDelete() }
                )
            }
        }
    }
}

@Composable
private fun StudyProgressGraph(
    courses: List<StudyCourse>,
    isDarkTheme: Boolean,
    modifier: Modifier = Modifier
) {
    val refs = remember(courses) { courses.flatMap { it.allClassRefs() } }
    val now = System.currentTimeMillis()
    val completed = refs.count { it.clazz.isCompleted }
    val missed = refs.count {
        !it.clazz.isCompleted && it.clazz.deadlineMillis != null && now > (it.clazz.deadlineMillis ?: 0L)
    }
    val pending = refs.size - completed - missed
    val total = refs.size

    Card(
        modifier = modifier.padding(vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = "PROGRESS", fontWeight = FontWeight.Bold, fontSize = 12.sp, letterSpacing = 1.sp)
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "$completed of $total tasks completed",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
            )
            Spacer(modifier = Modifier.height(8.dp))
            if (total == 0) {
                Text(
                    text = "Add classes to see your progress graph.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f)
                )
            } else {
                GraphBar("Completed", completed, total, Color(0xFF4CAF50))
                GraphBar("Pending", pending, total, Color(0xFF2196F3))
                GraphBar("Missed", missed, total, Color(0xFFE53935))
            }
        }
    }
}

@Composable
private fun GraphBar(label: String, count: Int, total: Int, color: Color) {
    val fraction = if (total == 0) 0f else count.toFloat() / total.toFloat()
    Column(modifier = Modifier.padding(vertical = 3.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(text = label, fontSize = 12.sp, color = color, fontWeight = FontWeight.SemiBold)
            Text(
                text = "$count",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
            )
        }
        Spacer(modifier = Modifier.height(3.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f))
        ) {
            if (fraction > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction)
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(color)
                )
            }
        }
    }
}

@Composable
private fun AddNameDialog(
    title: String,
    initialValue: String = "",
    confirmLabel: String = "Add",
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var text by remember { mutableStateOf(initialValue) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.Bold) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                singleLine = true,
                placeholder = { Text("Name") }
            )
        },
        confirmButton = {
            TextButton(onClick = { if (text.isNotBlank()) onConfirm(text) }) { Text(confirmLabel) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun ScheduleDialog(
    target: ScheduleTarget,
    onSave: (Long?, Long?) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var deadline by remember { mutableStateOf(target.deadlineMillis) }
    var alert by remember { mutableStateOf(target.alertMillis) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(target.name, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text("Reminder (notification)", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                Text(
                    text = alert?.let { formatDateTime(it) } ?: "Not set",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                )
                Row {
                    TextButton(onClick = { showDateTimePicker(context, alert) { alert = it } }) { Text("Set") }
                    if (alert != null) {
                        TextButton(onClick = { alert = null }) { Text("Clear") }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text("Deadline (notification)", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                Text(
                    text = deadline?.let { formatDateTime(it) } ?: "Not set",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                )
                Row {
                    TextButton(onClick = { showDateTimePicker(context, deadline) { deadline = it } }) { Text("Set") }
                    if (deadline != null) {
                        TextButton(onClick = { deadline = null }) { Text("Clear") }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { onSave(deadline, alert) }) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

private fun showDateTimePicker(context: Context, initial: Long?, onPicked: (Long) -> Unit) {
    val base = Calendar.getInstance().apply { if (initial != null) timeInMillis = initial }
    DatePickerDialog(
        context,
        { _, year, month, day ->
            val picked = Calendar.getInstance().apply {
                timeInMillis = base.timeInMillis
                set(Calendar.YEAR, year)
                set(Calendar.MONTH, month)
                set(Calendar.DAY_OF_MONTH, day)
            }
            TimePickerDialog(
                context,
                { _, hour, minute ->
                    picked.set(Calendar.HOUR_OF_DAY, hour)
                    picked.set(Calendar.MINUTE, minute)
                    picked.set(Calendar.SECOND, 0)
                    picked.set(Calendar.MILLISECOND, 0)
                    onPicked(picked.timeInMillis)
                },
                base.get(Calendar.HOUR_OF_DAY),
                base.get(Calendar.MINUTE),
                false
            ).show()
        },
        base.get(Calendar.YEAR),
        base.get(Calendar.MONTH),
        base.get(Calendar.DAY_OF_MONTH)
    ).show()
}

private fun buildStudyRows(courses: List<StudyCourse>, expanded: Set<String>): List<StudyRow> {
    val rows = mutableListOf<StudyRow>()
    for (course in courses) {
        val courseClasses = course.subjects.flatMap { it.topics.flatMap { topic -> topic.classes } }
        val courseExpanded = expanded.contains(course.id)
        rows.add(
            StudyRow(
                type = NodeType.COURSE,
                rowKey = course.id,
                courseId = course.id,
                name = course.name,
                level = 0,
                completed = course.completedCount(),
                total = course.totalCount(),
                status = folderStatus(courseClasses),
                hasChildren = course.subjects.isNotEmpty(),
                isExpanded = courseExpanded
            )
        )
        if (!courseExpanded) continue
        for (subject in course.subjects) {
            val subjectClasses = subject.topics.flatMap { it.classes }
            val subjectExpanded = expanded.contains(subject.id)
            rows.add(
                StudyRow(
                    type = NodeType.SUBJECT,
                    rowKey = subject.id,
                    courseId = course.id,
                    subjectId = subject.id,
                    name = subject.name,
                    level = 1,
                    completed = subject.completedCount(),
                    total = subject.totalCount(),
                    status = folderStatus(subjectClasses),
                    hasChildren = subject.topics.isNotEmpty(),
                    isExpanded = subjectExpanded
                )
            )
            if (!subjectExpanded) continue
            for (topic in subject.topics) {
                val topicExpanded = expanded.contains(topic.id)
                rows.add(
                    StudyRow(
                        type = NodeType.TOPIC,
                        rowKey = topic.id,
                        courseId = course.id,
                        subjectId = subject.id,
                        topicId = topic.id,
                        name = topic.name,
                        level = 2,
                        completed = topic.completedCount(),
                        total = topic.totalCount(),
                        status = folderStatus(topic.classes),
                        hasChildren = topic.classes.isNotEmpty(),
                        isExpanded = topicExpanded,
                        deadlineMillis = topic.deadlineMillis,
                        alertMillis = topic.alertMillis,
                        studiedMinutes = topic.studiedMinutes
                    )
                )
                if (!topicExpanded) continue
                for (clazz in topic.classes) {
                    rows.add(
                        StudyRow(
                            type = NodeType.CLASS,
                            rowKey = clazz.id,
                            courseId = course.id,
                            subjectId = subject.id,
                            topicId = topic.id,
                            classId = clazz.id,
                            name = clazz.name,
                            level = 3,
                            completed = if (clazz.isCompleted) 1 else 0,
                            total = 1,
                            status = clazz.status(),
                            isCompleted = clazz.isCompleted,
                            deadlineMillis = clazz.deadlineMillis,
                            alertMillis = clazz.alertMillis,
                            studiedMinutes = clazz.studiedMinutes
                        )
                    )
                }
            }
        }
    }
    return rows
}

private fun folderStatus(classes: List<StudyClass>): StudyStatus {
    if (classes.isEmpty()) return StudyStatus.PENDING
    val now = System.currentTimeMillis()
    if (classes.all { it.isCompleted }) return StudyStatus.COMPLETED
    if (classes.any { it.status(now) == StudyStatus.MISSED }) return StudyStatus.MISSED
    return StudyStatus.PENDING
}

private fun childLabel(type: NodeType): String = when (type) {
    NodeType.COURSE -> "Subject"
    NodeType.SUBJECT -> "Topic"
    NodeType.TOPIC -> "Class"
    // A Class can also spawn a sibling Class, so the same "Add Class" action is
    // offered from a class row too (no need to scroll back up to the Topic).
    NodeType.CLASS -> "Class"
}

private fun statusColor(status: StudyStatus): Color = when (status) {
    StudyStatus.COMPLETED -> Color(0xFF4CAF50)
    StudyStatus.MISSED -> Color(0xFFE53935)
    StudyStatus.PENDING -> Color(0xFF2196F3)
}

private fun formatDateTime(millis: Long): String =
    SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(millis))
