package com.example.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.TimeTableEntry
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * The Now/Next class strip (v1.17.0): today's current and next Time Table class
 * on Home, below the clock. Tapping it opens the Time Table.
 *
 * Free slots are shown honestly ("Free period till ...") — never as a fake
 * class. When there are no more classes today, the strip hides itself entirely.
 * It recomputes on a 1-minute tick; a per-second ticker would be wasteful for
 * something that only changes on class boundaries.
 */
@Composable
fun NowNextStrip(
    entries: List<TimeTableEntry>,
    onOpenTimeTable: () -> Unit,
    modifier: Modifier = Modifier
) {
    var nowMillis by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(60_000L)
            nowMillis = System.currentTimeMillis()
        }
    }

    val today = remember(entries, nowMillis) { todayEntries(entries, nowMillis) }
    if (today.isEmpty()) return
    val now = today.firstOrNull { it.startMillis <= nowMillis && nowMillis < it.endMillis }
    val next = today.firstOrNull { it.startMillis > nowMillis }
    if (now == null && next == null) return

    val timeFormat = remember { SimpleDateFormat("h:mm a", Locale.getDefault()) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable { onOpenTimeTable() }
            .testTag("now_next_strip"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Text(
                text = "TIME TABLE",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                ),
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
            )
            Spacer(modifier = Modifier.height(4.dp))
            if (now != null) {
                Text(
                    text = if (now.isFree) {
                        "Free period till ${timeFormat.format(Date(now.endMillis))}"
                    } else {
                        "Now: ${now.subjectName} - ${now.className}, " +
                            "till ${timeFormat.format(Date(now.endMillis))}"
                    },
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
            if (next != null) {
                if (now != null) Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = if (next.isFree) {
                        "Next: Free period at ${timeFormat.format(Date(next.startMillis))}"
                    } else {
                        "Next: ${next.subjectName} - ${next.className} " +
                            "at ${timeFormat.format(Date(next.startMillis))}"
                    },
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                )
            }
        }
    }
}

/** Today's entries, earliest first. Matched by day range, not exact midnight. */
private fun todayEntries(entries: List<TimeTableEntry>, nowMillis: Long): List<TimeTableEntry> {
    val start = Calendar.getInstance().apply {
        timeInMillis = nowMillis
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
    val end = start + 24 * 60 * 60 * 1000L
    return entries
        .filter { it.dateMillis in start until end }
        .sortedBy { it.startMillis }
}
