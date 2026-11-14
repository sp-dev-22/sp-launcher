package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AppInfo
import com.example.ui.components.AppDrawerItem
import com.example.ui.components.LoadingIndicator

@Composable
fun DrawerScreen(
    apps: List<AppInfo>,
    onOpenSearch: () -> Unit,
    isDarkTheme: Boolean,
    onAppClick: (AppInfo) -> Unit,
    onAppLongClick: (AppInfo) -> Unit,
    isLoadingApps: Boolean = false,
    lockedApps: Set<String> = emptySet(),
    badgeCounts: Map<String, Int> = emptyMap(),
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("drawer_screen")
    ) {
        // App Grid (4 columns)
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            if (isLoadingApps) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    LoadingIndicator()
                }
            } else if (apps.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No apps installed",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                    )
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(apps, key = { it.packageName }) { app ->
                        AppDrawerItem(
                            app = app,
                            isDarkTheme = isDarkTheme,
                            onClick = { onAppClick(app) },
                            onLongClick = { onAppLongClick(app) },
                            isLocked = app.packageName in lockedApps,
                            badgeCount = badgeCounts[app.packageName] ?: 0
                        )
                    }
                }
            }
        }

        // Search entry - opens the universal search screen
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            val searchBarColor = if (isDarkTheme) Color(0xFF26262A) else Color(0xFFF7EBE5)
            val placeholderColor = if (isDarkTheme) Color(0xFF9E9E9E) else Color(0xFF7D726D)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(searchBarColor)
                    .clickable { onOpenSearch() }
                    .padding(horizontal = 16.dp)
                    .testTag("drawer_search_button"),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = placeholderColor,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Search apps, tasks, notes…",
                    color = placeholderColor,
                    fontSize = 15.sp,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}
