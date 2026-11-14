package com.example.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * The What's New popup (v1.17.0): shown once after an update, never again until
 * the next one. The list is hardcoded per version — update it when the version
 * changes. Dismissing by any means (Got it, Back, tap outside) records the
 * version, so it can never re-show for the same version.
 */
@Composable
fun WhatsNewDialog(
    versionName: String,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "What's new in $versionName",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                Text(
                    "This update brings:",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                Spacer(modifier = Modifier.height(8.dp))
                WHAT_S_NEW_1_19_0.forEach { line ->
                    Text("- $line", fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("whats_new_dismiss")
            ) { Text("Got it") }
        }
    )
}

private val WHAT_S_NEW_1_19_0 = listOf(
    "New: Digi Brain - save notes and links from any app (select text and tap 'Save to Digi Brain', or share it here). Find it in Settings > Apps.",
    "Tick 'Show on widget' and a save appears on the Home widget, swipeable, with pinned items first.",
    "Trash with a conscience: deleted saves warn at 25 days (the widget's trash icon turns red) and auto-delete at 30.",
    "The Digi Brain face turns sad if you stay away - 3 days, then sadder at 7.",
    "'From your saves' resurfaces one old public save a day, so saved things get read again.",
    "SP Assistant search finds your public saves too. Private ones stay inside Digi Brain.",
    "Saving a link twice now warns you and opens the one you already saved."
)
