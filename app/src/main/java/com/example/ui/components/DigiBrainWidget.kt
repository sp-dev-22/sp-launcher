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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.example.data.model.DigiBrainItem
import com.example.data.model.DigiBrainKind
import com.example.util.DigiBrain

/**
 * The Digi Brain Home widget (v1.19.0): the mood face, the Trash signal and a
 * swipeable carousel of the public saves — pinned first, then newest first.
 * Tapping the card opens the Digi Brain screen; tapping a card opens that item.
 *
 * The Trash icon is the small-but-advanced part: grey while the Trash is calm,
 * neon red once something has sat in it 25+ days, and tapping it lands exactly
 * on the Trash tab with the expiring-soon filter. "Aaj ka ek" resurfaces one
 * public item unopened for 7+ days above the carousel.
 */
@Composable
fun DigiBrainWidget(
    items: List<DigiBrainItem>,
    lastUseMillis: Long,
    isDarkTheme: Boolean,
    onOpenDigiBrain: () -> Unit,
    onOpenTrash: (expiringOnly: Boolean) -> Unit,
    onOpenItem: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val now = remember { System.currentTimeMillis() }
    val mood = remember(lastUseMillis) { DigiBrain.moodFor(lastUseMillis, now) }
    val trashCount = remember(items) { items.count { it.deletedAtMillis != null } }
    val expiringCount = remember(items) {
        items.count { it.deletedAtMillis != null && DigiBrain.isExpiringSoon(it, now) }
    }
    val publicItems = remember(items) { DigiBrain.widgetOrder(items) }
    val resurface = remember(items) { DigiBrain.pickResurface(items, now) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable { onOpenDigiBrain() }
            .testTag("digi_brain_widget"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "DIGI BRAIN",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        ),
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    DigiBrainMoodFace(mood = mood, size = 22.dp)
                }

                // The Trash signal: neon red with a glow disc once something is
                // 25+ days in the Trash, grey otherwise. Tapping it opens the
                // Trash tab — filtered to expiring-soon when there is any.
                IconButton(
                    onClick = { onOpenTrash(expiringCount > 0) },
                    modifier = Modifier
                        .size(32.dp)
                        .testTag("digi_brain_trash_btn")
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(
                                if (expiringCount > 0) NEON_RED.copy(alpha = 0.18f)
                                else Color.Transparent
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = if (expiringCount > 0)
                                "Trash - $expiringCount expiring soon"
                            else
                                "Trash"
                            ,
                            tint = if (expiringCount > 0) NEON_RED
                            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            if (trashCount > 0) {
                Text(
                    text = if (expiringCount > 0)
                        "$expiringCount in Trash expiring soon - tap the trash icon to review"
                    else
                        "$trashCount in Trash - auto-deleted after 30 days",
                    fontSize = 11.sp,
                    color = if (expiringCount > 0) NEON_RED
                    else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                    modifier = Modifier.padding(top = 2.dp, bottom = 4.dp)
                )
            }

            // "Aaj ka ek": one old public save, resurfaced for today.
            if (resurface != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.10f))
                        .clickable { onOpenItem(resurface.id) }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                        .testTag("digi_brain_resurface"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "FROM YOUR SAVES",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = resurface.title.ifBlank { resurface.content },
                            fontSize = 13.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            if (publicItems.isEmpty()) {
                Text(
                    text = "Nothing on the widget yet. Save a note or a link from any app, " +
                        "tick 'Show on widget', and it appears here.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            } else {
                val pagerState = rememberPagerState(pageCount = { publicItems.size })

                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("digi_brain_pager")
                ) { page ->
                    val item = publicItems[page]
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .clickable { onOpenItem(item.id) }
                            .padding(horizontal = 14.dp, vertical = 12.dp)
                            .testTag("digi_brain_card_${item.id}"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (item.kind == DigiBrainKind.LINK)
                                Icons.Default.Link else Icons.Default.Description,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = item.title.ifBlank { item.content },
                                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                                    color = MaterialTheme.colorScheme.onBackground,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f, fill = false)
                                )
                                if (item.pinned) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Icon(
                                        imageVector = Icons.Default.PushPin,
                                        contentDescription = "Pinned",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (item.kind == DigiBrainKind.LINK)
                                    DigiBrain.domainOf(item.content)
                                else
                                    DigiBrain.previewLines(item.content, 2),
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                if (publicItems.size > 1) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        repeat(publicItems.size) { index ->
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
}

/** The neon red of the Trash warning (25+ days in the Trash). */
private val NEON_RED = Color(0xFFFF1744)
