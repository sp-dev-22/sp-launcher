package com.example.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AppInfo
import com.example.ui.theme.AppColors

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppDrawerItem(
    app: AppInfo,
    isDarkTheme: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = AppColors.getPaletteFor(app.packageName + app.label)
    val circleColor = if (isDarkTheme) palette.iconCircleDark else palette.iconCircleLight
    val initialChar = app.label.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "?"

    Column(
        modifier = modifier
            .width(76.dp)
            .clip(MaterialTheme.shapes.small)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(vertical = 10.dp, horizontal = 4.dp)
            .testTag("app_drawer_item_${app.packageName}"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Colorful solid circle icon with clean single letter inside
        Box(
            modifier = Modifier
                .size(54.dp)
                .background(color = circleColor, shape = CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = initialChar,
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Medium
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // App Name below icon (centered, 2 lines max)
        Text(
            text = app.label,
            style = MaterialTheme.typography.bodySmall.copy(
                fontSize = 11.sp,
                lineHeight = 14.sp
            ),
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}
