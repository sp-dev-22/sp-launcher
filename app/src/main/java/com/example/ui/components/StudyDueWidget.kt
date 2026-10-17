package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.StudyDueItem
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/** How many tasks to list on a day's page (keeps the card from looking cluttered). */
private const val MAX_ITEMS_PER_DAY = 3

/** The 7 day-pages shown by the widget: Today .. Today + 6. */
private const val DAY_COUNT = 7

private data class DayPage(val label: String, val items: List<StudyDueItem>)

/**
 * Home card showing Study Manager tasks due over the next 7 days.
 *
 * One page per day (Today, Tomorrow, then the following days) - swipe left/right to move
 * between days, exactly like the Notes widget. Overdue tasks are shown on the Today page.
 * Each page lists up to [MAX_ITEMS_PER_DAY] tasks, then a "+ N more" line.
 */
@Composable
fun StudyDueWidget(
    items: List<StudyDueItem>,
    isDarkTheme: Boolean,
    onOpenStudyManager: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (items.isEmpty()) return

    val pages = remember(items) { buildDayPages(items) }
    val pagerState = rememberPagerState(pageCount = { pages.size })
    val overdueCount = items.count { it.isOverdue }
    val dueToday = remember(items) {
        val today = startOfDay(System.currentTimeMillis())
        items.count { !it.isOverdue && startOfDay(it.deadlineMillis) == today }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable { onOpenStudyManager() }
            .testTag("study_due_widget"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "STUDY · NEXT 7 DAYS",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    ),
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                )
                if (overdueCount > 0) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFE53935).copy(alpha = 0.16f))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "$overdueCount overdue",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFE53935)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("study_due_pager")
            ) { page ->
                val day = pages[page]
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = day.label,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        ),
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    if (day.items.isEmpty()) {
                        Text(
                            text = "Nothing due",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.45f),
                            modifier = Modifier.padding(vertical = 6.dp)
                        )
                    } else {
                        day.items.take(MAX_ITEMS_PER_DAY).forEach { item ->
                            DueRow(item)
                        }
                        if (dueToday > 0) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                                horizontalArrangement = Arrangement.End
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFF2196F3).copy(alpha = 0.16f))
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                        .testTag("study_due_today")
                                ) {
                                    Text(
                                        text = "$dueToday due today",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF2196F3)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (pages.size > 1) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(pages.size) { index ->
                        val selected = pagerState.currentPage == index
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 3.dp)
                                .size(if (selected) 6.dp else 4.dp)
                                .clip(CircleShape)
                                .background(
                                    if (selected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.25f)
                                )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DueRow(item: StudyDueItem) {
    val accent = if (item.isOverdue) Color(0xFFE53935) else Color(0xFF2196F3)
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(accent)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.label,
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = if (item.isOverdue) "Overdue · ${formatDue(item.deadlineMillis)}"
                else "Due ${formatDue(item.deadlineMillis)}",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = accent
            )
        }
    }
}

/** Groups the items into 7 day-pages; overdue items land on the Today page. */
private fun buildDayPages(items: List<StudyDueItem>): List<DayPage> {
    val dayMillis = 24L * 60 * 60 * 1000
    val todayStart = startOfDay(System.currentTimeMillis())
    val buckets = List(DAY_COUNT) { mutableListOf<StudyDueItem>() }

    for (item in items) {
        val index = if (item.isOverdue) {
            0
        } else {
            val day = startOfDay(item.deadlineMillis)
            ((day - todayStart) / dayMillis).toInt().coerceIn(0, DAY_COUNT - 1)
        }
        buckets[index].add(item)
    }

    return (0 until DAY_COUNT).map { offset ->
        val dayStart = todayStart + offset * dayMillis
        DayPage(
            label = dayLabel(offset, dayStart),
            items = buckets[offset].sortedBy { it.deadlineMillis }
        )
    }
}

private fun dayLabel(offset: Int, dayStartMillis: Long): String = when (offset) {
    0 -> "TODAY"
    1 -> "TOMORROW"
    else -> SimpleDateFormat("EEE, dd MMM", Locale.getDefault())
        .format(Date(dayStartMillis))
        .uppercase(Locale.getDefault())
}

private fun startOfDay(millis: Long): Long = Calendar.getInstance().apply {
    timeInMillis = millis
    set(Calendar.HOUR_OF_DAY, 0)
    set(Calendar.MINUTE, 0)
    set(Calendar.SECOND, 0)
    set(Calendar.MILLISECOND, 0)
}.timeInMillis

private fun formatDue(millis: Long): String =
    SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date(millis))
