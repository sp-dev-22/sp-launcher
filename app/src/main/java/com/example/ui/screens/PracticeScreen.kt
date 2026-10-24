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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.LauncherViewModel

/**
 * Manual practice: sit down and go through the cards on screen (front -> reveal -> Got it /
 * Again), instead of waiting for a popup. Grading is the same as the popups.
 */
@Composable
fun PracticeScreen(
    viewModel: LauncherViewModel,
    onBack: () -> Unit
) {
    val cards by viewModel.flashcardBank.collectAsState()

    val decks = remember(cards) {
        cards.map { it.deck.ifBlank { "Ungrouped" } }.distinct().sorted()
    }
    var deckFilter by remember { mutableStateOf<String?>(null) }
    var session by remember { mutableStateOf(0) }
    var index by remember { mutableStateOf(0) }
    var revealed by remember { mutableStateOf(false) }
    var gotIt by remember { mutableStateOf(0) }
    var again by remember { mutableStateOf(0) }

    // Snapshot the queue when the deck / session changes, so grading a card does not reshuffle it.
    val queue = remember(deckFilter, session) {
        cards.filter { !it.locked && (deckFilter == null || it.deck.ifBlank { "Ungrouped" } == deckFilter) }
            .sortedBy { it.nextDueMillis ?: 0L }
    }

    LaunchedEffect(deckFilter, session) {
        index = 0; revealed = false; gotIt = 0; again = 0
    }

    val current = queue.getOrNull(index)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 18.dp)
            .testTag("practice_screen")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack, modifier = Modifier.testTag("practice_back_btn")) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
            }
            Text(
                text = "Practice",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
            )
        }

        if (decks.isNotEmpty()) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DeckChip("All", deckFilter == null) { deckFilter = null }
                decks.forEach { deck -> DeckChip(deck, deckFilter == deck) { deckFilter = deck } }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        when {
            queue.isEmpty() -> {
                Spacer(modifier = Modifier.height(40.dp))
                Text(
                    text = "No cards to practise here.",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                )
            }
            current == null -> {
                Spacer(modifier = Modifier.height(40.dp))
                Text("Session complete", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))
                Text("Got it: $gotIt    Again: $again", fontSize = 14.sp)
                Spacer(modifier = Modifier.height(18.dp))
                Button(onClick = { session++ }, modifier = Modifier.testTag("practice_again_btn")) {
                    Text("Practise again")
                }
            }
            else -> {
                Text(
                    text = "Card ${index + 1} of ${queue.size}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    modifier = Modifier.fillMaxWidth().weight(1f, fill = false),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                ) {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = current.front,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.SemiBold,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            if (revealed) {
                                Spacer(modifier = Modifier.height(18.dp))
                                Text(
                                    text = current.back,
                                    fontSize = 16.sp,
                                    textAlign = TextAlign.Center,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (!revealed) {
                    Button(
                        onClick = { revealed = true },
                        modifier = Modifier.fillMaxWidth().height(50.dp).testTag("practice_show_btn")
                    ) {
                        Text("Show answer")
                    }
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(
                            onClick = {
                                viewModel.markCard(current.id, false)
                                again++; index++; revealed = false
                            },
                            modifier = Modifier.weight(1f).height(50.dp).testTag("practice_again_card_btn")
                        ) {
                            Text("Again")
                        }
                        Button(
                            onClick = {
                                viewModel.markCard(current.id, true)
                                gotIt++; index++; revealed = false
                            },
                            modifier = Modifier.weight(1f).height(50.dp).testTag("practice_gotit_btn")
                        ) {
                            Text("Got it")
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
    }
}

@Composable
private fun DeckChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(
                if (selected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.surfaceVariant
            )
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = if (selected) MaterialTheme.colorScheme.onPrimary
            else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
