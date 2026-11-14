@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AppGroup
import com.example.data.model.AppInfo
import com.example.data.model.FavoriteSlots

/** One filled slot of the bar, already resolved to the app or the group it points at. */
data class BarSlot(
    val key: String,
    val app: AppInfo?,
    val group: AppGroup?,
    val groupApps: List<AppInfo>
)

/**
 * The Home-screen taskbar.
 *
 * It sits at the very bottom of Home, on top of the scrolling widget column, so it never moves
 * when the widgets scroll. Five slots at most — apps and groups together — plus a "+" that
 * opens the Favorites screen when there is still room.
 */
@Composable
fun FavoritesBar(
    favorites: List<String>,
    apps: List<AppInfo>,
    groups: List<AppGroup>,
    isDarkTheme: Boolean,
    onLaunchApp: (AppInfo) -> Unit,
    onEdit: () -> Unit,
    badgeCounts: Map<String, Int> = emptyMap(),
    modifier: Modifier = Modifier
) {
    val appMap = remember(apps) { apps.associateBy { it.packageName } }
    val groupMap = remember(groups) { groups.associateBy { it.id } }

    val slots = remember(favorites, apps, groups) {
        favorites.mapNotNull { slot ->
            if (FavoriteSlots.isGroup(slot)) {
                val group = groupMap[FavoriteSlots.groupId(slot)] ?: return@mapNotNull null
                BarSlot(
                    key = slot,
                    app = null,
                    group = group,
                    groupApps = group.packages.mapNotNull { appMap[it] }
                )
            } else {
                val app = appMap[slot] ?: return@mapNotNull null
                BarSlot(key = slot, app = app, group = null, groupApps = emptyList())
            }
        }
    }

    var openGroup by remember { mutableStateOf<BarSlot?>(null) }

    val barColor = if (isDarkTheme) Color(0xFF1B1B1F) else Color(0xFFFFFFFF)
    val outline = MaterialTheme.colorScheme.onBackground.copy(alpha = if (isDarkTheme) 0.16f else 0.10f)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(barColor)
            .border(width = 0.6.dp, color = outline)
            .padding(horizontal = 8.dp, vertical = 6.dp)
            .testTag("favorites_bar")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            slots.forEach { slot ->
                if (slot.group != null) {
                    GroupSlot(
                        slot = slot,
                        isDarkTheme = isDarkTheme,
                        badgeCount = slot.groupApps.sumOf { badgeCounts[it.packageName] ?: 0 },
                        onClick = { openGroup = slot }
                    )
                } else {
                    AppSlot(
                        app = slot.app!!,
                        isDarkTheme = isDarkTheme,
                        badgeCount = badgeCounts[slot.app.packageName] ?: 0,
                        onClick = { onLaunchApp(slot.app) },
                        onLongClick = onEdit
                    )
                }
            }

            if (favorites.size < FavoriteSlots.MAX_SLOTS) {
                AddSlot(isDarkTheme = isDarkTheme, onClick = onEdit)
            }
        }
    }

    val sheetGroup = openGroup
    if (sheetGroup != null) {
        GroupSheet(
            slot = sheetGroup,
            isDarkTheme = isDarkTheme,
            badgeCounts = badgeCounts,
            onLaunchApp = { app ->
                openGroup = null
                onLaunchApp(app)
            },
            onEdit = {
                openGroup = null
                onEdit()
            },
            onDismiss = { openGroup = null }
        )
    }
}

@Composable
private fun AppSlot(
    app: AppInfo,
    isDarkTheme: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    badgeCount: Int = 0
) {
    Column(
        modifier = Modifier
            .width(62.dp)
            .clip(RoundedCornerShape(12.dp))
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(vertical = 4.dp)
            .testTag("favorite_slot_${app.packageName}"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(contentAlignment = Alignment.Center) {
            RealAppIcon(app = app, size = 34.dp, isDarkTheme = isDarkTheme)
            NotificationBadge(
                count = badgeCount,
                modifier = Modifier.align(Alignment.TopEnd)
            )
        }
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = app.label,
            fontSize = 9.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
        )
    }
}

@Composable
private fun GroupSlot(
    slot: BarSlot,
    isDarkTheme: Boolean,
    onClick: () -> Unit,
    badgeCount: Int = 0
) {
    Column(
        modifier = Modifier
            .width(62.dp)
            .clip(RoundedCornerShape(12.dp))
            .combinedClickable(onClick = onClick, onLongClick = onClick)
            .padding(vertical = 4.dp)
            .testTag("favorite_group_slot_${slot.group?.id}"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // A 2x2 mini-grid of the first four icons reads as "a bundle of apps".
        // The outer Box holds the badge: the inner box is clipped to a rounded
        // rect, which would cut the badge's corner off.
        Box(contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                val preview = slot.groupApps.take(4)
                if (preview.isEmpty()) {
                    Icon(
                        imageVector = Icons.Default.Apps,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                } else {
                    Column {
                        preview.chunked(2).forEach { rowApps ->
                            Row {
                                rowApps.forEach { app ->
                                    RealAppIcon(
                                        app = app,
                                        size = 15.dp,
                                        isDarkTheme = isDarkTheme,
                                        modifier = Modifier.padding(0.5.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
            // Notification badge (v1.18.0): the unread total across the group.
            NotificationBadge(
                count = badgeCount,
                modifier = Modifier.align(Alignment.TopEnd)
            )
        }
        Spacer(modifier = Modifier.height(3.dp))
        // Groups are unnamed: the icons say it all.
        Text(
            text = "${slot.groupApps.size} apps",
            fontSize = 8.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
        )
    }
}

@Composable
private fun AddSlot(isDarkTheme: Boolean, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(52.dp)
            .clip(RoundedCornerShape(12.dp))
            .combinedClickable(onClick = onClick, onLongClick = onClick)
            .padding(vertical = 4.dp)
            .testTag("favorites_bar_add"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(9.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Add a favorite",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = "Add",
            fontSize = 9.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
        )
    }
}

/** The apps inside a group, opened by tapping the group's slot. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GroupSheet(
    slot: BarSlot,
    isDarkTheme: Boolean,
    onLaunchApp: (AppInfo) -> Unit,
    onEdit: () -> Unit,
    onDismiss: () -> Unit,
    badgeCounts: Map<String, Int> = emptyMap()
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp)
        ) {
            Text(
                text = "${slot.groupApps.size} apps",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "One slot in the favorites bar",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            Spacer(modifier = Modifier.height(12.dp))

            if (slot.groupApps.isEmpty()) {
                Text(
                    "No installed apps in this group.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            } else {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(slot.groupApps, key = { it.packageName }) { app ->
                        Column(
                            modifier = Modifier
                                .width(72.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .combinedClickable(
                                    onClick = { onLaunchApp(app) },
                                    onLongClick = { onLaunchApp(app) }
                                )
                                .padding(vertical = 8.dp)
                                .testTag("group_app_${app.packageName}"),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                RealAppIcon(app = app, size = 42.dp, isDarkTheme = isDarkTheme)
                                NotificationBadge(
                                    count = badgeCounts[app.packageName] ?: 0,
                                    modifier = Modifier.align(Alignment.TopEnd)
                                )
                            }
                            Spacer(modifier = Modifier.height(5.dp))
                            Text(
                                text = app.label,
                                fontSize = 10.sp,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Edit groups",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .combinedClickable(onClick = onEdit, onLongClick = onEdit)
                    .padding(vertical = 8.dp, horizontal = 4.dp)
            )
        }
    }
}
