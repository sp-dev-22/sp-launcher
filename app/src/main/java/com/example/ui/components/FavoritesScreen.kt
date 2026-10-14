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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
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
import com.example.data.model.AppInfo

/**
 * The single "Favorites" screen: tick the apps you want on the Home screen, and drag the
 * chosen ones to reorder them. Combines the old separate "Edit Favorites" and "Arrange
 * Favorites" screens into one place.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesScreen(
    allApps: List<AppInfo>,
    initialFavorites: List<String>,
    onSave: (List<String>) -> Unit,
    onBack: () -> Unit
) {
    val appMap = remember(allApps) { allApps.associateBy { it.packageName } }
    val favoriteOrder = remember {
        mutableStateListOf<String>().apply { addAll(initialFavorites) }
    }

    fun labelFor(pkg: String): String = appMap[pkg]?.label ?: pkg

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
                SectionHeader("YOUR FAVORITES — DRAG TO REORDER")
            }

            if (favoriteOrder.isEmpty()) {
                item {
                    Text(
                        text = "No favorites yet. Tick apps below to add them.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                    )
                }
            } else {
                itemsIndexed(favoriteOrder, key = { _, pkg -> pkg }) { index, pkg ->
                    val drag = rememberDragStep(stepPx) { dir ->
                        val target = index + dir
                        if (target in favoriteOrder.indices) {
                            val moved = favoriteOrder.removeAt(index)
                            favoriteOrder.add(target, moved)
                        }
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .graphicsLayer { translationY = drag.offset.value }
                            .padding(horizontal = 20.dp, vertical = 10.dp)
                            .testTag("favorite_row_$pkg"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.DragHandle,
                            contentDescription = "Drag to reorder",
                            tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f),
                            modifier = Modifier
                                .size(26.dp)
                                .then(drag.modifier)
                                .testTag("favorite_drag_$pkg")
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = labelFor(pkg),
                            style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp),
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.weight(1f)
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFC62828))
                                .clickable { favoriteOrder.remove(pkg) }
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                                .testTag("remove_favorite_$pkg")
                        ) {
                            Text("Remove", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }

            item { SectionHeader("ALL APPS") }

            items(notFavorites, key = { it.packageName }) { app ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { favoriteOrder.add(app.packageName) }
                        .padding(horizontal = 20.dp, vertical = 10.dp)
                        .testTag("app_checkbox_row_${app.packageName}"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = false,
                        onCheckedChange = { favoriteOrder.add(app.packageName) },
                        colors = CheckboxDefaults.colors(
                            checkedColor = Color(0xFF8D4A38),
                            checkmarkColor = Color.White
                        )
                    )
                    Spacer(modifier = Modifier.width(16.dp))
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
