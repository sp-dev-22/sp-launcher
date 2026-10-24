package com.example.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Clock + date. When both are on they sit side by side, each taking half the width
 * (neither holds long text, so two full-width rows wasted space). A lone one stays centred.
 */
@Composable
fun ClockWidget(
    showTime: Boolean,
    showDate: Boolean,
    onDateClick: () -> Unit,
    onTimeClick: () -> Unit,
    overrideMillis: Long?,
    modifier: Modifier = Modifier
) {
    if (!showTime && !showDate) return

    var currentTime by remember { mutableStateOf(Date()) }

    LaunchedEffect(Unit) {
        while (true) {
            currentTime = Date()
            delay(1000L)
        }
    }

    // 12-hour clock, e.g. "8:19 PM" (was 24-hour "20:18").
    val timeFormat = remember { SimpleDateFormat("h:mm a", Locale.getDefault()) }
    val dateFormat = remember { SimpleDateFormat("EEEE, d MMMM", Locale.getDefault()) }

    // A "set time" decoy may pin the clock to a different time for a few seconds.
    val formattedTime = timeFormat.format(Date(overrideMillis ?: currentTime.time))
    val formattedDate = dateFormat.format(currentTime)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .testTag("clock_widget"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (showTime && showDate) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = formattedTime,
                    fontSize = 38.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = (-1).sp,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onTimeClick() }
                        .testTag("clock_time_text")
                )
                Text(
                    text = formattedDate,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onDateClick() }
                        .testTag("clock_date_text")
                )
            }
        } else if (showTime) {
            Text(
                text = formattedTime,
                fontSize = 52.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
                letterSpacing = (-1).sp,
                modifier = Modifier
                    .clickable { onTimeClick() }
                    .testTag("clock_time_text")
            )
        } else {
            Text(
                text = formattedDate,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                ),
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                modifier = Modifier
                    .clickable { onDateClick() }
                    .testTag("clock_date_text")
            )
        }
    }
}


