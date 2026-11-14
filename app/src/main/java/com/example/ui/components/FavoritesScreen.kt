package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AppGroup
import com.example.data.model.AppInfo
import com.example.data.model.FavoriteSlots

/**
 * The single "Favorites" screen, and the only place app groups are made.
 *
 * The top section is the Home taskbar itself: at most five slots, each either an app or a group,
 * drag to reorder. Below it are the groups, then every installed app.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesScreen(
    allApps: List<AppInfo>,
    initialFavorites: List<String>,
    groups: List<AppGroup>,
    onSave: (List<String>) -> Unit,
    onAddGroup: (List<String>) -> AppGroup?,
    onUpdateGroup: (AppGroup) -> Unit,
    onDeleteGroup: (String) -> Unit,
    onBack: () -> Unit
) {
    val appMap = remember(allApps) { allApps.associateBy { it.packageName } }
    val groupMap = remember(groups) { groups.associateBy { it.id } }

    val favoriteOrder = remember {
        mutableStateListOf<String>().apply { addAll(initialFavorites.take(FavoriteSlots.MAX_SLOTS)) }
    }

    var showGroupEditor by remember { mutableStateOf<AppGroup?>(null) }
    var creatingGroup by remember { mutableStateOf(false) }
    var fullMessage by remember { mutableStateOf(false) }

    fun labelFor(slot: String): String = if (FavoriteSlots.isGroup(slot)) {
        groupMap[FavoriteSlots.groupId(slot)]?.let { "${it.packages.size} apps" } ?: "Group"
    } else {
        appMap[slot]?.label ?: slot
    }

    fun addSlot(slot: String) {
        if (favoriteOrder.contains(slot)) return
        if (favoriteOrder.size >= FavoriteSlots.MAX_SLOTS) {
            fullMessage = true
            return
        }
        favoriteOrder.add(slot)
    }

    val stepPx = with(LocalDensity.current) { 56.dp.toPx() }
    val notFavorites = allApps.filter { it.packageName !in favoriteOrder }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Favorites",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 22.sp
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("favorites_back_btn")) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    onSave(favoriteOrder.toList())
                    onBack()
                },
                shape = RoundedCornerShape(16.dp),
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.testTag("save_favorites_fab")
            ) {
                Icon(imageVector = Icons.Default.Check, contentDescription = "Save Favorites")
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .testTag("favorites_screen_list"),
            contentPadding = PaddingValues(bottom = 96.dp)
        ) {
            item {
                SectionHeader("YOUR BAR — ${favoriteOrder.size} / ${FavoriteSlots.MAX_SLOTS} SLOTS")
            }

            if (fullMessage) {
                item {
                    Text(
                        text = "The bar is full: ${FavoriteSlots.MAX_SLOTS} slots, apps and groups together. " +
                            "Remove one first.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
                    )
                }
            }

            if (favoriteOrder.isEmpty()) {
                item {
                    Text(
                        text = "No favorites yet. Tick apps below, or make a group.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                    )
                }
            } else {
                itemsIndexed(favoriteOrder, key = { _, slot -> slot }) { index, slot ->
                    val drag = rememberDragStep(stepPx) { dir ->
                        val target = index + dir
                        if (target in favoriteOrder.indices) {
                            val moved = favoriteOrder.removeAt(index)
                            favoriteOrder.add(target, moved)
                        }
                    }
                    val isGroup = FavoriteSlots.isGroup(slot)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .graphicsLayer { translationY = drag.offset.value }
                            .padding(horizontal = 20.dp, vertical = 10.dp)
                            .testTag("favorite_row_$slot"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.DragHandle,
                            contentDescription = "Drag to reorder",
                            tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f),
                            modifier = Modifier
                                .size(26.dp)
                                .then(drag.modifier)
                                .testTag("favorite_drag_$slot")
                        )
                        Spacer(modifier = Modifier.width(10.dp))

                        val app = if (isGroup) null else appMap[slot]
                        if (app != null) {
                            RealAppIcon(app = app, size = 26.dp, isDarkTheme = false)
                            Spacer(modifier = Modifier.width(8.dp))
                        } else if (isGroup) {
                            Icon(
                                imageVector = Icons.Default.Apps,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }

                        Text(
                            text = labelFor(slot),
                            style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp),
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.weight(1f)
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFC62828))
                                .clickable { favoriteOrder.remove(slot) }
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                                .testTag("remove_favorite_$slot")
                        ) {
                            Text("Remove", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }

            item { SectionHeader("GROUPS — ONE SLOT EACH") }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { creatingGroup = true }
                        .padding(horizontal = 20.dp, vertical = 10.dp)
                        .testTag("new_group_row"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Apps,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "New group",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            items(groups, key = { it.id }) { group ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp)
                        .testTag("group_row_${group.id}"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Group · ${group.packages.size} apps",
                        style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp),
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    // A group that is not in the bar (removed earlier, or restored from a
                    // backup) needs its way back in: without this, Edit/Delete were its only
                    // actions and it could never take a slot again.
                    val groupSlot = FavoriteSlots.forGroup(group.id)
                    if (groupSlot in favoriteOrder) {
                        Text(
                            text = "In bar",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.45f),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                        )
                    } else {
                        Text(
                            text = "Add",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .clickable { addSlot(groupSlot) }
                                .padding(horizontal = 6.dp, vertical = 4.dp)
                                .testTag("add_group_${group.id}")
                        )
                    }
                    Text(
                        text = "Edit",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .clickable { showGroupEditor = group }
                            .padding(horizontal = 6.dp, vertical = 4.dp)
                    )
                    Text(
                        text = "Delete",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier
                            .clickable {
                                onDeleteGroup(group.id)
                                favoriteOrder.remove(FavoriteSlots.forGroup(group.id))
                            }
                            .padding(horizontal = 6.dp, vertical = 4.dp)
                    )
                }
            }

            item { SectionHeader("ALL APPS") }

            items(notFavorites, key = { it.packageName }) { app ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { addSlot(app.packageName) }
                        .padding(horizontal = 20.dp, vertical = 10.dp)
                        .testTag("app_checkbox_row_${app.packageName}"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = false,
                        onCheckedChange = { addSlot(app.packageName) },
                        colors = CheckboxDefaults.colors(
                            checkedColor = Color(0xFF8D4A38),
                            checkmarkColor = Color.White
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    RealAppIcon(app = app, size = 26.dp, isDarkTheme = false)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = app.label,
                        style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp),
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }

    if (creatingGroup) {
        GroupEditorDialog(
            allApps = allApps,
            existing = null,
            onSave = { packages ->
                val created = onAddGroup(packages)
                creatingGroup = false
                if (created != null) {
                    // The new group takes its bar slot through the same path as every
                    // other slot, so the 5-slot limit is judged by the bar on this
                    // screen. If the bar is full the group still exists below, with
                    // its own Add button, ready for when a slot frees up.
                    addSlot(FavoriteSlots.forGroup(created.id))
                }
            },
            onDismiss = { creatingGroup = false }
        )
    }

    showGroupEditor?.let { group ->
        GroupEditorDialog(
            allApps = allApps,
            existing = group,
            onSave = { packages ->
                onUpdateGroup(group.copy(packages = packages))
                showGroupEditor = null
            },
            onDismiss = { showGroupEditor = null }
        )
    }
}

/**
 * Pick the apps that share one slot.
 *
 * A group has no name: it is just up to four apps packed into a single favourites slot, which is
 * how five slots can hold twenty apps.
 */
@Composable
private fun GroupEditorDialog(
    allApps: List<AppInfo>,
    existing: AppGroup?,
    onSave: (List<String>) -> Unit,
    onDismiss: () -> Unit
) {
    // Count installed apps only (v1.18.0): a stored package whose app was
    // uninstalled is a ghost — it never appears in the picker, so it must not
    // occupy one of the four slots or inflate the "N / 4 selected" count.
    // (Pruning normally removes ghosts at uninstall time; this is the safety
    // net for any that slip through, e.g. from a backup restore.)
    val selected = remember {
        mutableStateListOf<String>().apply {
            val installed = allApps.map { it.packageName }.toSet()
            addAll((existing?.packages ?: emptyList()).filter { it in installed })
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "New group" else "Edit group", fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    "Pick up to ${FavoriteSlots.MAX_APPS_PER_GROUP} apps. They share one slot in the " +
                        "favorites bar.",
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    "${selected.size} / ${FavoriteSlots.MAX_APPS_PER_GROUP} selected",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(6.dp))
                LazyColumn(modifier = Modifier.height(260.dp)) {
                    items(allApps, key = { it.packageName }) { app ->
                        val isSelected = selected.contains(app.packageName)
                        val full = selected.size >= FavoriteSlots.MAX_APPS_PER_GROUP && !isSelected
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(enabled = !full) {
                                    if (isSelected) selected.remove(app.packageName)
                                    else if (!full) selected.add(app.packageName)
                                }
                                .padding(vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Start
                        ) {
                            Checkbox(
                                checked = isSelected,
                                enabled = !full,
                                onCheckedChange = { checked ->
                                    if (checked) {
                                        if (selected.size < FavoriteSlots.MAX_APPS_PER_GROUP) {
                                            selected.add(app.packageName)
                                        }
                                    } else {
                                        selected.remove(app.packageName)
                                    }
                                }
                            )
                            Text(
                                text = app.label,
                                fontSize = 14.sp,
                                maxLines = 1,
                                color = MaterialTheme.colorScheme.onSurface.copy(
                                    alpha = if (full) 0.4f else 1f
                                )
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { if (selected.isNotEmpty()) onSave(selected.toList()) },
                modifier = Modifier.testTag("group_save_btn")
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun SectionHeader(text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Star,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
        )
    }
    Spacer(modifier = Modifier.height(4.dp))
}
