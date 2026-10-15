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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AlertItem
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.absoluteValue

/**
 * The Alerts widget: every alert shown as one swipeable card, with its custom tag
 * (only when set) and its reminder time (only when set).
 */
@Composable
fun AlertsWidget(
    items: List<AlertItem>,
    isDarkTheme: Boolean,
    onManage: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (items.isEmpty()) {
        Card(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp)
                .clickable { onManage() }
                .testTag("empty_alerts_card"),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add alert",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Add an alert",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        }
        return
    }

    val pagerState = rememberPagerState(pageCount = { items.size })

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("alerts_widget"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        HorizontalPager(state = pagerState, modifier = Modifier.fillMaxWidth()) { page ->
            val item = items[page]
            val chipBg = tagChipBackground(item.category, isDarkTheme)
            val chipFg = tagChipForeground(item.category, isDarkTheme)

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .clickable { onManage() }
                    .testTag("alert_card_$page"),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = item.title,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onBackground,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )

                        if (item.category.isNotBlank()) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(chipBg)
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = item.category,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = chipFg
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = item.content,
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp, lineHeight = 17.sp),
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    item.reminderMillis?.let { millis ->
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.NotificationsActive,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = formatAlertTime(millis),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }

        if (items.size > 1) {
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(items.size) { index ->
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

private fun formatAlertTime(millis: Long): String =
    SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date(millis))

private val CUSTOM_DARK_BG = listOf(0xFF3A2A4A, 0xFF243A3A, 0xFF3A3224, 0xFF2A2A3E, 0xFF3A2430)
private val CUSTOM_LIGHT_BG = listOf(0xFFE7DDF2, 0xFFD6EFEA, 0xFFF3E7CF, 0xFFDDE3F2, 0xFFF2DDE6)
private val CUSTOM_DARK_FG = listOf(0xFFD6B8F0, 0xFFA8E6DD, 0xFFEFD8A8, 0xFFB8C4F0, 0xFFF0B8D0)
private val CUSTOM_LIGHT_FG = listOf(0xFF6A3E96, 0xFF1B6B60, 0xFF8A6A16, 0xFF3A4A96, 0xFF963A66)

private fun tagChipBackground(tag: String, isDark: Boolean): Color {
    val idx = tag.hashCode().absoluteValue % CUSTOM_DARK_BG.size
    return Color(if (isDark) CUSTOM_DARK_BG[idx] else CUSTOM_LIGHT_BG[idx])
}

private fun tagChipForeground(tag: String, isDark: Boolean): Color {
    val idx = tag.hashCode().absoluteValue % CUSTOM_DARK_FG.size
    return Color(if (isDark) CUSTOM_DARK_FG[idx] else CUSTOM_LIGHT_FG[idx])
}
