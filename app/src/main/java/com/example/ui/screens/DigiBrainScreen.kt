package com.example.ui.screens

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.RestoreFromTrash
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DigiBrainItem
import com.example.data.model.DigiBrainKind
import com.example.ui.DigiBrainSaveResult
import com.example.ui.LauncherViewModel
import com.example.ui.components.ChoiceChip
import com.example.ui.components.DigiBrainMoodFace
import com.example.util.DigiBrain
import com.example.util.DigiBrainMood

private enum class SavedFilter { ALL, NOTES, LINKS, ON_WIDGET }

/**
 * Digi Brain, as a proper screen (v1.19.0): the launcher's small second brain.
 * Notes and links, saved from anywhere — the text-selection menu, the share
 * sheet, or the add row here. Two tabs: Saved and Trash. Public items (ticked
 * "Show on widget") appear on the Home widget carousel and in SP Assistant
 * search; private items never leave this screen.
 *
 * Trash: a deleted item warns at 25 days (the widget's trash icon turns neon
 * red) and is auto-deleted at 30. "Aaj ka ek" resurfaces one public item
 * unopened for 7+ days at the top.
 */
@Composable
fun DigiBrainScreen(
    viewModel: LauncherViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val items by viewModel.digiBrainItems.collectAsState()
    val lastUseMillis by viewModel.digiBrainLastUse.collectAsState()
    val args by viewModel.digiBrainArgs.collectAsState()
    val now = remember { System.currentTimeMillis() }

    // Where to land: the widget's red trash icon opens the Trash tab with the
    // expiring-soon filter; a search hit opens one item's detail.
    var selectedTab by remember { mutableIntStateOf(if (args.startInTrash) 1 else 0) }
    var trashExpiringOnly by remember { mutableStateOf(args.expiringOnly) }
    var detailItemId by remember { mutableStateOf(args.focusItemId) }
    var editingItem by remember { mutableStateOf<DigiBrainItem?>(null) }
    var duplicateItem by remember { mutableStateOf<DigiBrainItem?>(null) }
    var confirmDeleteId by remember { mutableStateOf<String?>(null) }
    var confirmEmptyTrash by remember { mutableStateOf(false) }
    var savedFilter by remember { mutableStateOf(SavedFilter.ALL) }
    var newText by remember { mutableStateOf("") }
    var newPublic by remember { mutableStateOf(false) }

    // A focused item (from search or a duplicate) counts as opened.
    LaunchedEffect(args.focusItemId) {
        args.focusItemId?.let { viewModel.openDigiBrainItem(it) }
    }

    val liveItems = remember(items) { items.filter { it.deletedAtMillis == null } }
    val trashItems = remember(items) {
        items.filter { it.deletedAtMillis != null }
            .sortedBy { it.deletedAtMillis }
    }
    val resurface = remember(items) { DigiBrain.pickResurface(items, now) }
    val mood = remember(lastUseMillis) { DigiBrain.moodFor(lastUseMillis, now) }

    val shownSaved = remember(liveItems, savedFilter) {
        when (savedFilter) {
            SavedFilter.ALL -> liveItems
            SavedFilter.NOTES -> liveItems.filter { it.kind == DigiBrainKind.NOTE }
            SavedFilter.LINKS -> liveItems.filter { it.kind == DigiBrainKind.LINK }
            SavedFilter.ON_WIDGET -> liveItems.filter { it.isPublic }
        }
    }
    val shownTrash = remember(trashItems, trashExpiringOnly) {
        if (trashExpiringOnly) trashItems.filter { DigiBrain.isExpiringSoon(it, now) }
        else trashItems
    }

    val detailItem = detailItemId?.let { id -> items.firstOrNull { it.id == id } }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 18.dp)
            .testTag("digi_brain_screen")
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack, modifier = Modifier.testTag("digi_brain_back_btn")) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
                Text(
                    text = "Digi Brain",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.width(10.dp))
                DigiBrainMoodFace(mood = mood, size = 34.dp)
            }

            Text(
                text = moodCaption(mood, lastUseMillis, now),
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
            )

            // "Aaj ka ek": one old public save, resurfaced for today.
            if (resurface != null && selectedTab == 0) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.10f))
                        .clickable {
                            viewModel.openDigiBrainItem(resurface.id)
                            detailItemId = resurface.id
                        }
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                        .testTag("digi_brain_resurface_card"),
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
                            fontSize = 14.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = if (resurface.kind == DigiBrainKind.LINK)
                                DigiBrain.domainOf(resurface.content)
                            else
                                DigiBrain.previewLines(resurface.content, 1),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            TabRow(
                selectedTabIndex = selectedTab,
                modifier = Modifier.testTag("digi_brain_tabs")
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Saved (${liveItems.size})") },
                    modifier = Modifier.testTag("digi_brain_tab_saved")
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Trash (${trashItems.size})") },
                    modifier = Modifier.testTag("digi_brain_tab_trash")
                )
            }

            Spacer(modifier = Modifier.height(14.dp))
        }

        if (selectedTab == 0) {
            item {
                // Add row: a bare URL becomes a link, anything else a note.
                OutlinedTextField(
                    value = newText,
                    onValueChange = { newText = it },
                    placeholder = { Text("Save a note or paste a link...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("digi_brain_add_input"),
                    shape = RoundedCornerShape(12.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clickable { newPublic = !newPublic }
                            .testTag("digi_brain_add_public")
                    ) {
                        Checkbox(checked = newPublic, onCheckedChange = { newPublic = it })
                        Text("Show on widget", fontSize = 13.sp)
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    Button(
                        onClick = {
                            if (newText.isBlank()) return@Button
                            when (val result = viewModel.saveDigiBrainText(newText, newPublic)) {
                                is DigiBrainSaveResult.Saved -> {
                                    newText = ""
                                    newPublic = false
                                }
                                is DigiBrainSaveResult.Duplicate -> {
                                    duplicateItem = result.existing
                                }
                            }
                        },
                        modifier = Modifier.testTag("digi_brain_add_btn")
                    ) {
                        Text("Save")
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    ChoiceChip("All", savedFilter == SavedFilter.ALL) { savedFilter = SavedFilter.ALL }
                    ChoiceChip("Notes", savedFilter == SavedFilter.NOTES) { savedFilter = SavedFilter.NOTES }
                    ChoiceChip("Links", savedFilter == SavedFilter.LINKS) { savedFilter = SavedFilter.LINKS }
                    ChoiceChip("On widget", savedFilter == SavedFilter.ON_WIDGET) { savedFilter = SavedFilter.ON_WIDGET }
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            if (shownSaved.isEmpty()) {
                item {
                    Text(
                        text = if (liveItems.isEmpty())
                            "Nothing saved yet. Save text from any app — select it and tap " +
                                "'Save to Digi Brain', or share it here — or type above."
                        else
                            "Nothing matches this filter.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f)
                    )
                }
            }

            items(shownSaved, key = { it.id }) { item ->
                DigiBrainRow(
                    item = item,
                    onOpen = {
                        viewModel.openDigiBrainItem(item.id)
                        detailItemId = item.id
                    },
                    onEdit = { editingItem = item },
                    onDelete = { viewModel.moveDigiBrainToTrash(item.id) },
                    onTogglePin = { viewModel.setDigiBrainPinned(item.id, !item.pinned) },
                    onTogglePublic = { viewModel.setDigiBrainPublic(item.id, !item.isPublic) }
                )
            }
        } else {
            item {
                Text(
                    text = "Deleted items stay here 30 days, then disappear for good. " +
                        "Items 25+ days old turn the widget's trash icon red.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        ChoiceChip("All", !trashExpiringOnly) { trashExpiringOnly = false }
                        ChoiceChip("Expiring soon", trashExpiringOnly) { trashExpiringOnly = true }
                    }
                    if (trashItems.isNotEmpty()) {
                        TextButton(
                            onClick = { confirmEmptyTrash = true },
                            modifier = Modifier.testTag("digi_brain_empty_trash_btn")
                        ) {
                            Text("Empty trash", color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            if (shownTrash.isEmpty()) {
                item {
                    Text(
                        text = if (trashItems.isEmpty())
                            "Trash is empty."
                        else
                            "Nothing expiring soon.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f)
                    )
                }
            }

            items(shownTrash, key = { it.id }) { item ->
                val daysLeft = DigiBrain.daysLeftInTrash(item, now)
                val expiring = DigiBrain.isExpiringSoon(item, now)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                        .testTag("digi_brain_trash_row_${item.id}"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (item.kind == DigiBrainKind.LINK)
                            Icons.Default.Link else Icons.Default.Description,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = item.title.ifBlank { item.content },
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = if (daysLeft <= 0) "Deleting soon"
                            else if (daysLeft == 1) "1 day left"
                            else "$daysLeft days left",
                            fontSize = 11.sp,
                            fontWeight = if (expiring) FontWeight.Bold else FontWeight.Normal,
                            color = if (expiring) Color(0xFFFF1744)
                            else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f)
                        )
                    }
                    IconButton(
                        onClick = { viewModel.restoreDigiBrainFromTrash(item.id) },
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.RestoreFromTrash,
                            contentDescription = "Restore",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    IconButton(
                        onClick = { confirmDeleteId = item.id },
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteForever,
                            contentDescription = "Delete forever",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(30.dp))
        }
    }

    // One item, full view: the whole note, or the link with an Open button.
    detailItem?.let { item ->
        DigiBrainDetailDialog(
            item = item,
            onDismiss = { detailItemId = null },
            onEdit = { editingItem = item; detailItemId = null },
            onTogglePublic = { viewModel.setDigiBrainPublic(item.id, !item.isPublic) },
            onTogglePin = { viewModel.setDigiBrainPinned(item.id, !item.pinned) },
            onDelete = { viewModel.moveDigiBrainToTrash(item.id); detailItemId = null },
            onRestore = { viewModel.restoreDigiBrainFromTrash(item.id); detailItemId = null },
            onDeleteForever = { confirmDeleteId = item.id; detailItemId = null }
        )
    }

    // Edit title / text / URL, public tick and pin.
    editingItem?.let { original ->
        DigiBrainEditDialog(
            original = original,
            allItems = items,
            onSave = { updated ->
                viewModel.updateDigiBrainItem(updated)
                editingItem = null
            },
            onDismiss = { editingItem = null }
        )
    }

    // This link was saved before: stop, and offer the one that exists.
    duplicateItem?.let { existing ->
        AlertDialog(
            onDismissRequest = { duplicateItem = null },
            title = { Text("Already saved", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "This link is already in Digi Brain as \"${existing.title.ifBlank { existing.content }}\".",
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    duplicateItem = null
                    newText = ""
                    viewModel.openDigiBrainItem(existing.id)
                    detailItemId = existing.id
                }) { Text("Open existing") }
            },
            dismissButton = {
                TextButton(onClick = { duplicateItem = null }) { Text("Keep typing") }
            }
        )
    }

    // Delete forever needs a confirm — there is no coming back from it.
    confirmDeleteId?.let { id ->
        AlertDialog(
            onDismissRequest = { confirmDeleteId = null },
            title = { Text("Delete forever?", fontWeight = FontWeight.Bold) },
            text = { Text("This cannot be undone.", fontSize = 13.sp) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteDigiBrainPermanently(id)
                    confirmDeleteId = null
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDeleteId = null }) { Text("Cancel") }
            }
        )
    }

    if (confirmEmptyTrash) {
        AlertDialog(
            onDismissRequest = { confirmEmptyTrash = false },
            title = { Text("Empty the Trash?", fontWeight = FontWeight.Bold) },
            text = { Text("Every item in the Trash will be deleted forever.", fontSize = 13.sp) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.emptyDigiBrainTrash()
                    confirmEmptyTrash = false
                }) { Text("Empty", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { confirmEmptyTrash = false }) { Text("Cancel") }
            }
        )
    }
}

private fun moodCaption(mood: DigiBrainMood, lastUseMillis: Long, now: Long): String {
    if (lastUseMillis <= 0L) return "Your second brain is ready. Save notes and links from any app."
    val days = DigiBrain.daysBetween(lastUseMillis, now)
    return when (mood) {
        DigiBrainMood.HAPPY -> "Happy to see you. Notes and links, saved from anywhere."
        DigiBrainMood.OK -> "All good. Notes and links, saved from anywhere."
        DigiBrainMood.SAD, DigiBrainMood.VERY_SAD ->
            "Missed you for $days days. Your saves are still here."
    }
}

@Composable
private fun DigiBrainRow(
    item: DigiBrainItem,
    onOpen: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onTogglePin: () -> Unit,
    onTogglePublic: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .testTag("digi_brain_row_${item.id}"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (item.kind == DigiBrainKind.LINK)
                Icons.Default.Link else Icons.Default.Description,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column(
            modifier = Modifier
                .weight(1f)
                .clickable { onOpen() }
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = item.title.ifBlank { item.content },
                    style = MaterialTheme.typography.bodyMedium,
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
            Text(
                text = if (item.kind == DigiBrainKind.LINK)
                    DigiBrain.domainOf(item.content)
                else
                    DigiBrain.previewLines(item.content, 1),
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        IconButton(
            onClick = onTogglePublic,
            modifier = Modifier.size(34.dp)
        ) {
            Text(
                text = if (item.isPublic) "WIDGET" else "PRIVATE",
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = if (item.isPublic) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.45f)
            )
        }
        IconButton(
            onClick = onTogglePin,
            modifier = Modifier.size(34.dp)
        ) {
            Icon(
                imageVector = Icons.Default.PushPin,
                contentDescription = if (item.pinned) "Unpin" else "Pin to top of widget",
                tint = if (item.pinned) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(18.dp)
            )
        }
        IconButton(
            onClick = onEdit,
            modifier = Modifier.size(34.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Edit,
                contentDescription = "Edit",
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                modifier = Modifier.size(18.dp)
            )
        }
        IconButton(
            onClick = onDelete,
            modifier = Modifier.size(34.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "Move to Trash",
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun DigiBrainDetailDialog(
    item: DigiBrainItem,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onTogglePublic: () -> Unit,
    onTogglePin: () -> Unit,
    onDelete: () -> Unit,
    onRestore: () -> Unit,
    onDeleteForever: () -> Unit
) {
    val context = LocalContext.current
    val inTrash = item.deletedAtMillis != null
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                item.title.ifBlank { item.content },
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                if (item.kind == DigiBrainKind.LINK) {
                    Text(
                        text = DigiBrain.domainOf(item.content),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(text = item.content, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(onClick = { openLink(context, item.content) }) {
                        Icon(
                            imageVector = Icons.Default.OpenInBrowser,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Open in browser")
                    }
                } else {
                    Text(text = item.content, fontSize = 14.sp)
                }
                if (!inTrash) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { onTogglePublic() }
                    ) {
                        Checkbox(checked = item.isPublic, onCheckedChange = { onTogglePublic() })
                        Text("Show on widget", fontSize = 13.sp)
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { onTogglePin() }
                    ) {
                        Checkbox(checked = item.pinned, onCheckedChange = { onTogglePin() })
                        Text("Pin to top of widget", fontSize = 13.sp)
                    }
                }
            }
        },
        confirmButton = {
            if (inTrash) {
                TextButton(onClick = onRestore) { Text("Restore") }
            } else {
                TextButton(onClick = onEdit) { Text("Edit") }
            }
        },
        dismissButton = {
            if (inTrash) {
                TextButton(onClick = onDeleteForever) {
                    Text("Delete forever", color = MaterialTheme.colorScheme.error)
                }
            } else {
                TextButton(onClick = onDelete) {
                    Text("Trash", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    )
}

@Composable
private fun DigiBrainEditDialog(
    original: DigiBrainItem,
    allItems: List<DigiBrainItem>,
    onSave: (DigiBrainItem) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var title by remember { mutableStateOf(original.title) }
    var content by remember { mutableStateOf(original.content) }
    var isPublic by remember { mutableStateOf(original.isPublic) }
    var pinned by remember { mutableStateOf(original.pinned) }
    val isLink = original.kind == DigiBrainKind.LINK

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isLink) "Edit Link" else "Edit Note", fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text(if (isLink) "Link" else "Note") },
                    singleLine = isLink,
                    minLines = if (isLink) 1 else 4,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { isPublic = !isPublic }
                ) {
                    Checkbox(checked = isPublic, onCheckedChange = { isPublic = it })
                    Text("Show on widget", fontSize = 13.sp)
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { pinned = !pinned }
                ) {
                    Checkbox(checked = pinned, onCheckedChange = { pinned = it })
                    Text("Pin to top of widget", fontSize = 13.sp)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val cleanContent = content.trim()
                if (cleanContent.isBlank()) return@TextButton
                if (isLink) {
                    if (!DigiBrain.looksLikeUrl(cleanContent) &&
                        DigiBrain.extractUrl(cleanContent) == null
                    ) {
                        Toast.makeText(context, "Enter a valid link", Toast.LENGTH_SHORT).show()
                        return@TextButton
                    }
                    val normalised = DigiBrain.normalizeUrl(cleanContent)
                    val clash = allItems.firstOrNull {
                        it.id != original.id && it.deletedAtMillis == null &&
                            it.kind == DigiBrainKind.LINK &&
                            DigiBrain.normalizeUrl(it.content) == normalised
                    }
                    if (clash != null) {
                        Toast.makeText(
                            context,
                            "That link is already saved as \"${clash.title.ifBlank { clash.content }}\"",
                            Toast.LENGTH_SHORT
                        ).show()
                        return@TextButton
                    }
                    onSave(
                        original.copy(
                            title = title.trim().ifBlank { DigiBrain.domainOf(normalised) },
                            content = normalised,
                            isPublic = isPublic,
                            pinned = pinned
                        )
                    )
                } else {
                    onSave(
                        original.copy(
                            title = title.trim().ifBlank { DigiBrain.titleFromText(cleanContent) },
                            content = cleanContent,
                            isPublic = isPublic,
                            pinned = pinned
                        )
                    )
                }
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

/** Opens a saved link in the browser. No browser -> a toast, never a crash. */
private fun openLink(context: Context, url: String) {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(DigiBrain.normalizeUrl(url)))
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    try {
        context.startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        Toast.makeText(context, "No browser found", Toast.LENGTH_SHORT).show()
    }
}
