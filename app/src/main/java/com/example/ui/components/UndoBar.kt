package com.example.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * The "deleted - undo" bar.
 *
 * It is drawn twice: once on the main screen and once inside the widget-manager sheet. The
 * sheet lives in its own window, so a bar drawn only on the main screen would end up hidden
 * behind it. Whichever is visible works; the auto-hide timer lives in the ViewModel.
 */
@Composable
fun UndoBar(
    message: String?,
    onUndo: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (message == null) return
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(12.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.inverseSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = message,
                modifier = Modifier.weight(1f),
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.inverseOnSurface
            )
            TextButton(onClick = onUndo) {
                Text("Undo", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.inversePrimary)
            }
        }
    }
}
