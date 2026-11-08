package com.example.ui.anim

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * A soft, static rainbow wash used behind the app when the Rainbow theme is on.
 *
 * This is all that is left of the old festival animation file: the animated effects
 * (lamps, rockets, stars and the rest) were removed in v1.11.5 because the frame loop
 * kept the GPU busy and drank the battery. This background draws once and never moves.
 */
@Composable
fun RainbowBackground(modifier: Modifier = Modifier, dark: Boolean = false) {
    val top = if (dark) Color(0xFF1A0F14) else Color(0xFFFFF8E1)
    val bottom = if (dark) Color(0xFF12061A) else Color(0xFFFFEFF6)
    val brush = Brush.verticalGradient(
        listOf(
            top,
            Color(0x33FF5252),
            Color(0x33FFC107),
            Color(0x3300E5FF),
            Color(0x3369F0AE),
            bottom
        )
    )
    Canvas(modifier = modifier.fillMaxSize()) {
        drawRect(brush = brush)
    }
}
