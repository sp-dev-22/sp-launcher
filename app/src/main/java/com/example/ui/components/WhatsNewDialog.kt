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
                WHAT_S_NEW_1_19_1.forEach { line ->
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

private val WHAT_S_NEW_1_19_1 = listOf(
    "In-app Browser: web kholo, bina launcher chhode — Home ke Browser chip se ya Settings > Apps se.",
    "AI Avatar: ek chhota chehra jo kisi bhi app ke upar tairta hai. Settings > AI Avatar se on karo (overlay permission chahiye).",
    "AI chat jald aa raha hai — avatar dabane par abhi ek message dikhta hai, chat baad me aayegi. Browser ka \"AI ko bhejo\" bhi tabhi kaam karega.",
    "Browser ko internet chahiye, isliye INTERNET permission add hui — internet sirf browser use karta hai, aur kuch nahi."
)
