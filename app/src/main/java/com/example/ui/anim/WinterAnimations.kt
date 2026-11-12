package com.example.ui.anim

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.dp
import kotlin.math.sin

/**
 * A soft, static frost wash used behind the app when the Winter theme is on.
 *
 * Draws once and never moves — the Winter theme itself costs no battery at all,
 * exactly like the Rainbow wash.
 */
@Composable
fun WinterBackground(modifier: Modifier = Modifier, dark: Boolean = false) {
    val top = if (dark) Color(0xFF0D161D) else Color(0xFFF0F7FA)
    val bottom = if (dark) Color(0xFF0A1218) else Color(0xFFE3EFF6)
    val brush = Brush.verticalGradient(
        listOf(
            top,
            Color(0x26B3D9F2),
            Color(0x267FC4EC),
            Color(0x2654687A),
            bottom
        )
    )
    Canvas(modifier = modifier.fillMaxSize()) {
        drawRect(brush = brush)
    }
}

/**
 * The lighter Winter animation layer (v1.14.0): forty snowflakes drifting down with a
 * gentle sway. No trails, no rotation, no glow — just circles on the shared 30fps
 * clock, so it is the cheapest layer in the app.
 *
 * Mounted only on the Home screen, only while the Winter theme is active, only when
 * its toggle is on, and never while Battery Saver is on — all of that is decided by
 * the caller, not here. The canvas takes no pointer input, so it never blocks a tap.
 */
@Composable
fun WinterAnimationLayer(modifier: Modifier = Modifier) {
    val t = rememberLightAnimationTime()
    Canvas(modifier = modifier.fillMaxSize()) {
        drawSnowfallLight(t)
    }
}

private fun DrawScope.drawSnowfallLight(t: Float) {
    val flakeColors = listOf(Color.White, Color(0xFFD6EAF7), Color(0xFFB3D9F2))
    for (i in 0 until 40) {
        val fallSeconds = 9f + hash01(i * 3 + 8) * 7f
        val progress = ((t / fallSeconds) + i * 0.37f) % 1f
        val sway = sin(t * (0.5f + hash01(i * 5 + 1)) + i * 1.7f) * 14.dp.toPx()
        val x = hash01(i * 7 + 2) * size.width + sway
        val y = progress * size.height * 1.04f - 8.dp.toPx()
        val r = (1.4f + hash01(i * 11 + 5) * 2.1f).dp.toPx()
        val alpha = 0.45f + 0.45f * hash01(i * 13 + 3)
        drawCircle(
            color = flakeColors[i % flakeColors.size].copy(alpha = alpha),
            radius = r,
            center = Offset(x, y)
        )
    }
}
