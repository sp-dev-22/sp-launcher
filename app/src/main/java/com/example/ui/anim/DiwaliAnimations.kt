package com.example.ui.anim

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp
import com.example.data.model.AnimationEffect
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private val DIWALI_COLORS = listOf(
    Color(0xFFFFC107), // marigold
    Color(0xFFFF5252), // red
    Color(0xFF00E5FF), // cyan
    Color(0xFF69F0AE), // green
    Color(0xFFFF4081), // pink
    Color(0xFFB388FF)  // violet
)

/** A soft rainbow wash used behind the app when the Diwali theme is on. */
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

/**
 * The in-app festival animation layer.
 *
 * Every effect has its own toggle in Settings > Auto & Animations; [effects] holds the keys that
 * are switched on. The canvas takes no pointer input, so it never blocks a tap.
 *
 * Everything is drawn inside the launcher only — there is deliberately no system overlay.
 */
@Composable
fun DiwaliAnimationLayer(
    effects: Set<String>,
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "diwali")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 9_000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "diwali_phase"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        if (effects.isEmpty()) return@Canvas
        if (AnimationEffect.LAMP.key in effects) drawLamps(phase)
        if (AnimationEffect.FIRECRACKERS.key in effects) drawFirecrackers(phase)
        if (AnimationEffect.RANGOLI.key in effects) drawRangoli(phase)
        if (AnimationEffect.CHAKRI.key in effects) drawChakri(phase)
        if (AnimationEffect.SKY_LANTERNS.key in effects) drawSkyLanterns(phase)
        if (AnimationEffect.PETALS.key in effects) drawPetals(phase)
    }
}

/** A row of diyas hanging from the top edge, each swaying on its own thread. */
private fun DrawScope.drawLamps(phase: Float) {
    val count = 7
    val threadTop = 0f
    val threadLength = size.height * 0.055f
    for (i in 0 until count) {
        val baseX = size.width * (i + 0.5f) / count
        val angle = sin((phase * 2 * PI + i * 0.7).toFloat()) * 6f
        val sway = sin((phase * 2 * PI + i * 0.7).toFloat()) * 10f
        val lampY = threadTop + threadLength

        rotate(degrees = angle, pivot = Offset(baseX, threadTop)) {
            drawLine(
                color = Color(0x66FFD54F),
                start = Offset(baseX, threadTop),
                end = Offset(baseX, lampY),
                strokeWidth = 1.5.dp.toPx()
            )
            drawCircle(
                color = Color(0x33FFB300),
                radius = 14.dp.toPx(),
                center = Offset(baseX, lampY + 8.dp.toPx())
            )
            drawArc(
                color = Color(0xFF8D4A38),
                startAngle = 0f,
                sweepAngle = 180f,
                useCenter = true,
                topLeft = Offset(baseX - 9.dp.toPx(), lampY),
                size = Size(18.dp.toPx(), 12.dp.toPx())
            )
            val flicker = 0.85f + 0.15f * sin((phase * 12 * PI).toFloat())
            drawCircle(
                color = Color(0xFFFFD54F),
                radius = 3.2.dp.toPx() * flicker,
                center = Offset(baseX + sway * 0.15f, lampY - 2.dp.toPx())
            )
        }
    }
}

/** Sparks flying out of a few crackers placed around the lower half of the screen. */
private fun DrawScope.drawFirecrackers(phase: Float) {
    val bursts = 4
    for (b in 0 until bursts) {
        val cx = size.width * (0.18f + 0.22f * b)
        val cy = size.height * (0.55f + 0.09f * (b % 2))
        val local = (phase + b * 0.25f) % 1f
        val sparkCount = 10
        for (s in 0 until sparkCount) {
            val angle = (s.toFloat() / sparkCount) * 2 * PI.toFloat() + b
            val distance = (6.dp.toPx() + local * 34.dp.toPx())
            val alpha = (1f - local).coerceIn(0f, 1f)
            val color = DIWALI_COLORS[(s + b) % DIWALI_COLORS.size].copy(alpha = alpha * 0.85f)
            drawCircle(
                color = color,
                radius = (1.6f - local).coerceAtLeast(0.6f).dp.toPx(),
                center = Offset(
                    cx + cos(angle) * distance,
                    cy + sin(angle) * distance
                )
            )
        }
    }
}

/** A slowly turning rangoli in each bottom corner. */
private fun DrawScope.drawRangoli(phase: Float) {
    val centres = listOf(
        Offset(size.width * 0.06f, size.height * 0.94f),
        Offset(size.width * 0.94f, size.height * 0.94f)
    )
    centres.forEachIndexed { index, centre ->
        val petals = 12
        val rotation = phase * 360f * if (index == 0) 1f else -1f
        val baseRadius = 22.dp.toPx()
        rotate(degrees = rotation, pivot = centre) {
            for (p in 0 until petals) {
                val angle = (p.toFloat() / petals) * 2 * PI.toFloat()
                val color = DIWALI_COLORS[p % DIWALI_COLORS.size].copy(alpha = 0.55f)
                drawCircle(
                    color = color,
                    radius = 4.dp.toPx(),
                    center = Offset(
                        centre.x + cos(angle) * baseRadius,
                        centre.y + sin(angle) * baseRadius
                    )
                )
            }
            drawCircle(
                color = Color(0x88FFC107),
                radius = baseRadius,
                center = centre,
                style = Stroke(width = 1.5.dp.toPx())
            )
        }
    }
}

/** A ground spinner (chakri): a ring of sparks turning fast around a fixed point. */
private fun DrawScope.drawChakri(phase: Float) {
    val spots = listOf(
        Offset(size.width * 0.28f, size.height * 0.72f),
        Offset(size.width * 0.74f, size.height * 0.63f)
    )
    spots.forEachIndexed { index, centre ->
        val rotation = phase * 360f * 6f * if (index == 0) 1f else -1f
        val radius = 16.dp.toPx()
        rotate(degrees = rotation, pivot = centre) {
            for (s in 0 until 14) {
                val angle = (s.toFloat() / 14) * 2 * PI.toFloat()
                val color = DIWALI_COLORS[s % DIWALI_COLORS.size].copy(alpha = 0.75f)
                drawCircle(
                    color = color,
                    radius = 2.6.dp.toPx(),
                    center = Offset(
                        centre.x + cos(angle) * radius,
                        centre.y + sin(angle) * radius
                    )
                )
            }
        }
        // the hot centre
        drawCircle(
            color = Color(0xFFFFF59D).copy(alpha = 0.5f),
            radius = 4.dp.toPx(),
            center = centre
        )
    }
}

/** Sky lanterns drifting upwards, each at its own speed and drift. */
private fun DrawScope.drawSkyLanterns(phase: Float) {
    val count = 5
    for (i in 0 until count) {
        val progress = (phase + i * 0.2f) % 1f
        val x = size.width * (0.12f + 0.18f * i) +
            sin((progress * 4 * PI + i).toFloat()) * 12.dp.toPx()
        // rise from below the screen to above it
        val y = size.height * (1.05f - progress * 1.25f)
        val fade = if (progress < 0.15f) progress / 0.15f else (1f - progress) / 0.35f
        val alpha = fade.coerceIn(0f, 1f) * 0.8f
        val w = 11.dp.toPx()
        val h = 15.dp.toPx()

        drawCircle(
            color = Color(0xFFFFB300).copy(alpha = alpha * 0.35f),
            radius = w,
            center = Offset(x, y + h * 0.4f)
        )
        drawRect(
            color = Color(0xFFFF7043).copy(alpha = alpha),
            topLeft = Offset(x - w * 0.55f, y - h * 0.5f),
            size = Size(w * 1.1f, h)
        )
        drawRect(
            color = Color(0xFFFFF59D).copy(alpha = alpha * 0.9f),
            topLeft = Offset(x - w * 0.3f, y + h * 0.35f),
            size = Size(w * 0.6f, h * 0.18f)
        )
    }
}

/** Marigold petals falling across the screen. */
private fun DrawScope.drawPetals(phase: Float) {
    val count = 14
    for (i in 0 until count) {
        val speed = 0.55f + (i % 4) * 0.15f
        val progress = (phase * speed + i * 0.083f) % 1f
        val sway = sin((progress * 6 * PI + i).toFloat()) * 16.dp.toPx()
        val x = size.width * ((i * 0.071f) % 1f) + sway
        val y = size.height * progress
        val color = DIWALI_COLORS[i % DIWALI_COLORS.size].copy(alpha = 0.55f)
        rotate(degrees = progress * 360f, pivot = Offset(x, y)) {
            drawCircle(
                color = color,
                radius = 3.4.dp.toPx(),
                center = Offset(x, y)
            )
        }
    }
}
