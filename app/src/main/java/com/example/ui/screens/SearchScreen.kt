package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.LauncherViewModel
import com.example.util.GlobalSearch
import com.example.util.SearchHit
import com.example.util.SearchKind

/**
 * Universal search. The box sits at the top (so the keyboard never covers it) and results are
 * one flat list across apps, tasks, notes, study classes and cards. Tapping a result opens it.
 */
@Composable
fun SearchScreen(
    viewModel: LauncherViewModel,
    onBack: () -> Unit
) {
    val apps by viewModel.installedApps.collectAsState()
    val tasks by viewModel.tasks.collectAsState()
    val alerts by viewModel.alerts.collectAsState()
    val courses by viewModel.studyCourses.collectAsState()
    val cards by viewModel.flashcardBank.collectAsState()

    var query by remember { mutableStateOf("") }
    val hits = remember(query, apps, tasks, alerts, courses, cards) {
        GlobalSearch.search(query, apps, tasks, alerts, courses, cards)
    }

    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("search_screen")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 4.dp, end = 12.dp, top = 8.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack, modifier = Modifier.testTag("search_back_btn")) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
            }
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("Search apps, tasks, notes, cards…") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(focusRequester)
                    .testTag("search_input")
            )
        }

        when {
            query.isBlank() -> Hint("Type to search apps, tasks, notes, study classes and cards.")
            hits.isEmpty() -> Hint("No results for \"$query\"")
            else -> LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp)
            ) {
                items(hits, key = { it.kind.name + ":" + it.refId }) { hit ->
                    SearchRow(hit) {
                        when (hit.kind) {
                            SearchKind.APP -> {
                                val app = apps.firstOrNull { it.packageName == hit.refId }
                                viewModel.setShowSearch(false)
                                if (app != null) viewModel.launchApp(app)
                            }
                            SearchKind.TASK, SearchKind.NOTE -> {
                                viewModel.setShowSearch(false)
                                viewModel.setShowWidgetsDialog(true)
                            }
                            SearchKind.CLASS -> {
                                viewModel.setShowSearch(false)
                                viewModel.setShowStudyManager(true)
                            }
                            SearchKind.CARD -> {
                                viewModel.setShowSearch(false)
                                viewModel.setShowCards(true)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Hint(text: String) {
    Box(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        Text(text, fontSize = 13.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f))
    }
}

@Composable
private fun SearchRow(hit: SearchHit, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 12.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = hit.title,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = hit.subtitle,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
