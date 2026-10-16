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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.FlashcardItem
import com.example.ui.LauncherViewModel

@Composable
fun CardsScreen(
    viewModel: LauncherViewModel,
    onBack: () -> Unit
) {
    val cards by viewModel.flashcardBank.collectAsState()
    val settings by viewModel.popupSettings.collectAsState()
    val stats by viewModel.cardStats.collectAsState()

    var query by remember { mutableStateOf("") }
    var editing by remember { mutableStateOf<FlashcardItem?>(null) }
    var deleting by remember { mutableStateOf<FlashcardItem?>(null) }

    val filtered = remember(cards, query) {
        if (query.isBlank()) cards
        else cards.filter {
            it.front.contains(query, ignoreCase = true) || it.back.contains(query, ignoreCase = true)
        }
    }
    val byDeck = remember(filtered) {
        filtered.groupBy { it.deck.ifBlank { "Ungrouped" } }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("cards_screen")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack, modifier = Modifier.testTag("cards_back_btn")) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
            }
            Text(
                text = "Cards",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.weight(1f)
            )
        }

        // progress summary
        Card(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Stat("Total", stats.total, MaterialTheme.colorScheme.primary)
                Stat("Mastered", stats.mastered, Color(0xFF4CAF50))
                Stat("Due", stats.due, Color(0xFF2196F3))
                Stat("Today", stats.masteredToday, Color(0xFFE65100))
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text("Search cards…") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).testTag("cards_search")
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (cards.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    "No cards yet.\nAdd card files from Study Popups.",
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                    fontSize = 13.sp
                )
            }
            return@Column
        }

        LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp)) {
            byDeck.forEach { (deck, list) ->
                item(key = "deck-$deck") {
                    val enabled = !settings.disabledDecks.contains(deck)
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(deck, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            Text(
                                "${list.size} cards · ${list.count { it.locked }} mastered",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                            )
                        }
                        Text(
                            if (enabled) "Active" else "Off",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Switch(
                            checked = enabled,
                            onCheckedChange = { viewModel.setDeckEnabled(deck, it) },
                            modifier = Modifier.testTag("deck_switch_$deck")
                        )
                    }
                }
                items(list, key = { it.id }) { card ->
                    CardRow(
                        card = card,
                        onEdit = { editing = card },
                        onDelete = { deleting = card }
                    )
                }
                item(key = "div-$deck") { HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp)) }
            }
            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }

    editing?.let { card ->
        CardEditDialog(
            card = card,
            onSave = { viewModel.updateCard(it); editing = null },
            onDismiss = { editing = null }
        )
    }

    deleting?.let { card ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text("Delete this card?", fontWeight = FontWeight.Bold) },
            text = { Text(card.front, maxLines = 3, overflow = TextOverflow.Ellipsis) },
            confirmButton = { TextButton(onClick = { viewModel.deleteCard(card.id); deleting = null }) { Text("Delete") } },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun Stat(label: String, value: Int, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("$value", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = color)
        Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f))
    }
}

@Composable
private fun CardRow(card: FlashcardItem, onEdit: () -> Unit, onDelete: () -> Unit) {
    val status = when {
        card.locked -> "mastered"
        card.nextDueMillis == null || card.nextDueMillis <= System.currentTimeMillis() -> "due"
        else -> "later"
    }
    val statusColor = when (status) {
        "mastered" -> Color(0xFF4CAF50)
        "due" -> Color(0xFF2196F3)
        else -> MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f)
    }
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(card.front, fontSize = 13.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(
                    card.back, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                )
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(statusColor.copy(alpha = 0.15f))
                    .padding(horizontal = 7.dp, vertical = 2.dp)
            ) {
                Text(status, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = statusColor)
            }
            IconButton(onClick = onEdit, modifier = Modifier.size(34.dp)) {
                Icon(Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(17.dp))
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(34.dp)) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(17.dp))
            }
        }
    }
}

@Composable
private fun CardEditDialog(
    card: FlashcardItem,
    onSave: (FlashcardItem) -> Unit,
    onDismiss: () -> Unit
) {
    var front by remember { mutableStateOf(card.front) }
    var back by remember { mutableStateOf(card.back) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit card", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                OutlinedTextField(
                    value = front,
                    onValueChange = { front = it },
                    label = { Text("Front (question)") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = back,
                    onValueChange = { back = it },
                    label = { Text("Back (answer)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (front.isNotBlank()) onSave(card.copy(front = front.trim(), back = back.trim()))
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
