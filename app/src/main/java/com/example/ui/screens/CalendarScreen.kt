package com.example.ui.screens

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.LauncherViewModel
import com.example.util.CalendarData
import com.example.util.CalendarEvent
import com.example.util.CalendarEventType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val DEADLINE_COLOR = Color(0xFFE53935)
private val REMINDER_COLOR = Color(0xFF1E88E5)
private val TASK_DUE_COLOR = Color(0xFFF9A825)
private val TASK_REMINDER_COLOR = Color(0xFF8E24AA)
private val CARD_COLOR = Color(0xFF43A047)

/**
 * A month view of everything already dated in the app: study deadlines, note reminders, task
 * due dates / reminders and card review days. Tap a day to see that day's list. Local only.
 */
@Composable
fun CalendarScreen(
    viewModel: LauncherViewModel,
    onBack: () -> Unit
) {
    val courses by viewModel.studyCourses.collectAsState()
    val tasks by viewModel.tasks.collectAsState()
    val alerts by viewModel.alerts.collectAsState()
    val cards by viewModel.flashcardBank.collectAsState()

    val events = remember(courses, tasks, alerts) { CalendarData.eventsFor(courses, tasks, alerts) }
    val cardCounts = remember(cards) { CalendarData.cardDueCounts(cards) }
    val eventsByDay = remember(events) { events.groupBy { it.dayStartMillis } }

    val today = remember { CalendarData.startOfDay(System.currentTimeMillis()) }
    var month by remember { mutableStateOf(CalendarData.startOfMonth(System.currentTimeMillis())) }
    var selected by remember { mutableStateOf(today) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .testTag("calendar_screen")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack, modifier = Modifier.testTag("calendar_back_btn")) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
            }
            Text(
                text = "Calendar",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { month = CalendarData.addMonths(month, -1) }) {
                Icon(Icons.Default.ChevronLeft, contentDescription = "Previous month")
            }
            Text(
                text = monthLabel(month),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
            )
            IconButton(onClick = { month = CalendarData.addMonths(month, 1) }) {
                Icon(Icons.Default.ChevronRight, contentDescription = "Next month")
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Row(modifier = Modifier.fillMaxWidth()) {
            listOf("S", "M", "T", "W", "T", "F", "S").forEach { d ->
                Text(
                    text = d,
                    modifier = Modifier.weight(1f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                )
            }
        }

        val firstDow = CalendarData.dayOfWeekIndex(month) // 1..7
        val daysInMonth = CalendarData.daysInMonth(month)
        val leading = firstDow - 1
        val rows = (leading + daysInMonth + 6) / 7

        for (r in 0 until rows) {
            Row(modifier = Modifier.fillMaxWidth()) {
                for (c in 0 until 7) {
                    val cellIndex = r * 7 + c
                    val dayNum = cellIndex - leading + 1
                    if (dayNum in 1..daysInMonth) {
                        val dayMillis = CalendarData.addDays(month, dayNum - 1)
                        DayCell(
                            dayNum = dayNum,
                            isToday = dayMillis == today,
                            isSelected = dayMillis == selected,
                            dots = eventsByDay[dayMillis].orEmpty().map { it.type }.distinct(),
                            cardCount = cardCounts[dayMillis] ?: 0,
                            onClick = { selected = dayMillis },
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        Spacer(modifier = Modifier.weight(1f).height(52.dp))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = selectedLabel(selected),
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(6.dp))

        val dayEvents = eventsByDay[selected].orEmpty().sortedBy { it.timeMillis ?: Long.MIN_VALUE }
        val dayCardCount = cardCounts[selected] ?: 0

        if (dayEvents.isEmpty() && dayCardCount == 0) {
            Text(
                text = "Nothing on this day.",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f)
            )
        } else {
            dayEvents.forEach { event -> EventRow(event) }
            if (dayCardCount > 0) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Dot(CARD_COLOR)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "$dayCardCount card(s) to review",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            LegendDot("Deadline", DEADLINE_COLOR)
            LegendDot("Reminder", REMINDER_COLOR)
            LegendDot("Task", TASK_DUE_COLOR)
            LegendDot("Card", CARD_COLOR)
        }

        Spacer(modifier = Modifier.height(30.dp))
    }
}

@Composable
private fun DayCell(
    dayNum: Int,
    isToday: Boolean,
    isSelected: Boolean,
    dots: List<CalendarEventType>,
    cardCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(52.dp)
            .padding(2.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(
                when {
                    isSelected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                    isToday -> MaterialTheme.colorScheme.surfaceVariant
                    else -> Color.Transparent
                }
            )
            .clickable { onClick() },
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(top = 5.dp)
        ) {
            Text(
                text = dayNum.toString(),
                fontSize = 13.sp,
                fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                color = if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                dots.take(4).forEach { type -> Dot(typeColor(type), size = 5.dp) }
                if (cardCount > 0) Dot(CARD_COLOR, size = 5.dp)
            }
        }
    }
}

@Composable
private fun EventRow(event: CalendarEvent) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Dot(typeColor(event.type))
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = event.label,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = typeLabel(event.type),
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
            )
        }
        event.timeMillis?.let {
            Text(
                text = formatTime(it),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun Dot(color: Color, size: androidx.compose.ui.unit.Dp = 8.dp) {
    Box(modifier = Modifier.size(size).clip(CircleShape).background(color))
}

@Composable
private fun LegendDot(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Dot(color, size = 7.dp)
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.65f)
        )
    }
}

private fun typeColor(type: CalendarEventType): Color = when (type) {
    CalendarEventType.DEADLINE -> DEADLINE_COLOR
    CalendarEventType.REMINDER -> REMINDER_COLOR
    CalendarEventType.TASK_DUE -> TASK_DUE_COLOR
    CalendarEventType.TASK_REMINDER -> TASK_REMINDER_COLOR
    CalendarEventType.CARD -> CARD_COLOR
}

private fun typeLabel(type: CalendarEventType): String = when (type) {
    CalendarEventType.DEADLINE -> "Deadline"
    CalendarEventType.REMINDER -> "Reminder"
    CalendarEventType.TASK_DUE -> "Task due"
    CalendarEventType.TASK_REMINDER -> "Task reminder"
    CalendarEventType.CARD -> "Card"
}

private fun monthLabel(millis: Long): String =
    SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(Date(millis))

private fun selectedLabel(millis: Long): String =
    SimpleDateFormat("EEEE, d MMM yyyy", Locale.getDefault()).format(Date(millis))

private fun formatTime(millis: Long): String =
    SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(millis))
