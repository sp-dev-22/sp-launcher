package com.example.ui.components

import android.content.pm.ShortcutInfo
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AppInfo
import com.example.util.AppShortcuts
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppActionBottomSheet(
    app: AppInfo,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onAppInfo: () -> Unit,
    onUninstall: () -> Unit,
    onDismiss: () -> Unit,
    isLocked: Boolean = false,
    onToggleLock: () -> Unit = {},
    onShare: () -> Unit = {}
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current

    // The app's own shortcuts (v1.18.0): loaded off the main thread, shown at
    // the top of the sheet. An app with no shortcuts shows no section at all.
    val shortcuts by produceState<List<ShortcutInfo>>(
        initialValue = emptyList(),
        app.packageName
    ) {
        value = withContext(Dispatchers.IO) {
            AppShortcuts.getShortcuts(context, app.packageName)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 8.dp)
                    .width(42.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 8.dp)
                .testTag("app_action_bottom_sheet")
        ) {
            // App Title centered with a underline
            Text(
                text = app.label,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 20.sp
                ),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(vertical = 8.dp)
            )

            Box(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .width(120.dp)
                    .height(2.dp)
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f))
            )

            Spacer(modifier = Modifier.height(20.dp))

            // The app's own shortcuts (v1.18.0), first — they are the app's
            // actions, so they sit above the launcher's actions.
            if (shortcuts.isNotEmpty()) {
                shortcuts.forEach { shortcut ->
                    ShortcutRow(
                        shortcut = shortcut,
                        onClick = {
                            if (!AppShortcuts.launchShortcut(context, shortcut)) {
                                Toast.makeText(
                                    context,
                                    "Could not open this shortcut",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                            onDismiss()
                        }
                    )
                }
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 8.dp),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                )
            }

            // Action: Add / Remove from Favorites
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        onToggleFavorite()
                        onDismiss()
                    }
                    .padding(vertical = 14.dp)
                    .testTag("action_toggle_favorite"),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (isFavorite) Icons.Default.Star else Icons.Outlined.StarBorder,
                    contentDescription = null,
                    tint = if (isFavorite) Color(0xFFF57C00) else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(18.dp))
                Text(
                    text = if (isFavorite) "Remove from Favorites" else "Add to Favorites",
                    style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            // Action: Lock / Unlock app (v1.14.0 App Lock). Locking asks for a PIN
            // the first time; unlocking always asks for the current PIN.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        onToggleLock()
                        onDismiss()
                    }
                    .padding(vertical = 14.dp)
                    .testTag("action_toggle_lock"),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (isLocked) Icons.Default.LockOpen else Icons.Default.Lock,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(18.dp))
                Text(
                    text = if (isLocked) "Unlock app" else "Lock app",
                    style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            // Action: App Info
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        onAppInfo()
                        onDismiss()
                    }
                    .padding(vertical = 14.dp)
                    .testTag("action_app_info"),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(18.dp))
                Text(
                    text = "App Info",
                    style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            // Action: Share app (v1.18.0): sends the APK via the share sheet.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        onShare()
                        onDismiss()
                    }
                    .padding(vertical = 14.dp)
                    .testTag("action_share"),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(18.dp))
                Text(
                    text = "Share app",
                    style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            // Action: Uninstall
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        onUninstall()
                        onDismiss()
                    }
                    .padding(vertical = 14.dp)
                    .testTag("action_uninstall"),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(18.dp))
                Text(
                    text = "Uninstall",
                    style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp),
                    color = MaterialTheme.colorScheme.error
                )
            }

            Spacer(modifier = Modifier.height(28.dp))
        }
    }
}

/** One shortcut row (v1.18.0): the shortcut's own icon, or a blank space when it has none. */
@Composable
private fun ShortcutRow(
    shortcut: ShortcutInfo,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val icon by produceState<ImageBitmap?>(initialValue = null, shortcut.id) {
        value = withContext(Dispatchers.IO) {
            AppShortcuts.shortcutIcon(context, shortcut, 96)
        }
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val bitmap = icon
        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = null,
                modifier = Modifier.size(24.dp)
            )
        } else {
            Spacer(modifier = Modifier.size(24.dp))
        }
        Spacer(modifier = Modifier.width(18.dp))
        Text(
            text = shortcut.shortLabel.toString(),
            style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
