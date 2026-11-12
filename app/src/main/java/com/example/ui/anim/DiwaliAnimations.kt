package com.example.ui.anim

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp
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

private val GOLD = Color(0xFFFFC107)
private val PALE_GOLD = Color(0xFFFFF59D)

/**
 * A soft, static rainbow wash used behind the app when the Rainbow theme is on.
 *
 * This draws once and never moves.
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

/**
 * The lighter Diwali animation layer (v1.14.0).
 *
 * The v1.11.3/v1.11.4 effects are back — Lamps, Firecrackers, Rangoli, Rockets and
 * Twinkling Stars — but cut down: roughly half the particles of the old engine, one
 * shared 30fps clock instead of a per-frame loop, and only five effects (no Chakri,
 * no Sky Lanterns, no Petals). The layer is mounted only on the Home screen, only
 * while the Rainbow theme is active, only when its toggle is on, and never while
 * Battery Saver is on — all of that is decided by the caller, not here. The canvas
 * takes no pointer input, so it never blocks a tap.
 */
@Composable
fun DiwaliAnimationLayer(modifier: Modifier = Modifier) {
    val t = rememberLightAnimationTime()
    Canvas(modifier = modifier.fillMaxSize()) {
        drawStarsLight(t)
        drawLampsLight(t)
        drawRangoliLight(t)
        drawFirecrackersLight(t)
        drawRocketsLight(t)
    }
}

/** Thirty-six twinkling stars, each at its own speed; every ninth is a sparkle cross. */
private fun DrawScope.drawStarsLight(t: Float) {
    val starColors = listOf(Color.White, PALE_GOLD, Color(0xFFB2EBF2))
    for (i in 0 until 36) {
        val x = hash01(i * 3 + 1) * size.width
        val y = hash01(i * 3 + 2) * size.height * 0.88f
        val speed = 0.6f + hash01(i * 3 + 3) * 2.4f
        val tw = 0.5f + 0.5f * sin(t * speed + i * 1.37f)
        val color = starColors[i % starColors.size].copy(alpha = 0.22f + 0.68f * tw)
        if (i % 9 == 0) {
            val arm = (4.5f + 4.5f * tw).dp.toPx()
            val w = 1.1.dp.toPx()
            drawLine(color, Offset(x - arm, y), Offset(x + arm, y), strokeWidth = w)
            drawLine(color, Offset(x, y - arm), Offset(x, y + arm), strokeWidth = w)
            drawCircle(color, radius = 1.5.dp.toPx(), center = Offset(x, y))
        } else {
            val r = (0.8f + hash01(i + 500) * 1.2f).dp.toPx() * (0.7f + 0.5f * tw)
            drawCircle(color, radius = r, center = Offset(x, y))
        }
    }
}

/** Six diyas hanging from the top edge; each sways and flickers on its own rhythm. */
private fun DrawScope.drawLampsLight(t: Float) {
    val count = 6
    for (i in 0 until count) {
        val baseX = size.width * (i + 0.5f) / count
        val threadLength = size.height * (0.04f + 0.025f * hash01(i * 5 + 1))
        val lampY = threadLength
        val angle = sin(t * 0.9f + i * 0.83f) * 5.5f
        val flicker = (0.84f + 0.14f * sin(t * 11f + i * 1.93f) + 0.06f * sin(t * 23f + i * 4.1f))
            .coerceIn(0.6f, 1f)

        rotate(degrees = angle, pivot = Offset(baseX, 0f)) {
            drawLine(
                color = Color(0x66FFD54F),
                start = Offset(baseX, 0f),
                end = Offset(baseX, lampY),
                strokeWidth = 1.5.dp.toPx()
            )
            // warm halo, breathing with the flame
            val haloR = (15f + 5f * flicker).dp.toPx()
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFFFFB300).copy(alpha = 0.4f * flicker), Color.Transparent),
                    center = Offset(baseX, lampY + 6.dp.toPx()),
                    radius = haloR
                ),
                radius = haloR,
                center = Offset(baseX, lampY + 6.dp.toPx())
            )
            // diya bowl
            drawArc(
                color = Color(0xFF8D4A38),
                startAngle = 0f,
                sweepAngle = 180f,
                useCenter = true,
                topLeft = Offset(baseX - 10.dp.toPx(), lampY),
                size = Size(20.dp.toPx(), 13.dp.toPx())
            )
            drawOval(
                color = Color(0xFFA85B45),
                topLeft = Offset(baseX - 10.dp.toPx(), lampY - 1.5.dp.toPx()),
                size = Size(20.dp.toPx(), 4.dp.toPx())
            )
            // teardrop flame with a bright heart
            val cx = baseX
            val cy = lampY - 1.dp.toPx()
            val fh = 9.5.dp.toPx() * (0.88f + 0.2f * flicker)
            val fw = 4.6.dp.toPx()
            val flame = Path().apply {
                moveTo(cx, cy - fh)
                quadraticTo(cx + fw, cy - fh * 0.35f, cx + fw * 0.55f, cy + fh * 0.3f)
                quadraticTo(cx + fw * 0.3f, cy + fh * 0.6f, cx, cy + fh * 0.6f)
                quadraticTo(cx - fw * 0.3f, cy + fh * 0.6f, cx - fw * 0.55f, cy + fh * 0.3f)
                quadraticTo(cx - fw, cy - fh * 0.35f, cx, cy - fh)
                close()
            }
            drawPath(flame, Color(0xFFFFA000).copy(alpha = 0.95f))
            val inner = Path().apply {
                val s = 0.52f
                moveTo(cx, cy - fh * s)
                quadraticTo(cx + fw * s, cy - fh * 0.2f, cx + fw * 0.3f, cy + fh * 0.3f)
                quadraticTo(cx, cy + fh * 0.45f, cx - fw * 0.3f, cy + fh * 0.3f)
                quadraticTo(cx - fw * s, cy - fh * 0.2f, cx, cy - fh * s)
                close()
            }
            drawPath(inner, PALE_GOLD.copy(alpha = 0.95f))
        }
    }
}

/**
 * One rangoli in each bottom corner: two petal rings turning against each other around a lit
 * centre, with a dotted ring between them. Fewer petals and dots than the v1.11.3 version.
 */
private fun DrawScope.drawRangoliLight(t: Float) {
    val centres = listOf(
        Offset(size.width * 0.07f, size.height * 0.93f),
        Offset(size.width * 0.93f, size.height * 0.93f)
    )
    centres.forEachIndexed { index, centre ->
        val dir = if (index == 0) 1f else -1f
        val pulse = 1f + 0.03f * sin(t * 0.8f + index * 2f)
        val r = 30.dp.toPx() * pulse
        val rot = t * 15f * dir // one slow turn every 24 seconds

        // soft glow underneath
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFFFFC107).copy(alpha = 0.16f), Color.Transparent),
                center = centre,
                radius = r * 1.3f
            ),
            radius = r * 1.3f,
            center = centre
        )

        // outer petal ring: 10 petals standing around the circle
        for (p in 0 until 10) {
            val a = (p.toFloat() / 10) * 2 * PI.toFloat() + rot * PI.toFloat() / 180f
            val pc = Offset(centre.x + cos(a) * r * 0.84f, centre.y + sin(a) * r * 0.84f)
            val petalColor = DIWALI_COLORS[p % DIWALI_COLORS.size].copy(alpha = 0.8f)
            rotate(degrees = a * 180f / PI.toFloat() + 90f, pivot = pc) {
                drawOval(
                    color = petalColor,
                    topLeft = Offset(pc.x - 3.2.dp.toPx(), pc.y - 6.5.dp.toPx()),
                    size = Size(6.4.dp.toPx(), 13.dp.toPx())
                )
            }
        }

        // dotted ring
        drawCircle(
            color = Color(0xFF00E5FF).copy(alpha = 0.5f),
            radius = r * 0.6f,
            center = centre,
            style = Stroke(width = 1.2.dp.toPx())
        )
        for (d in 0 until 12) {
            val a = (d.toFloat() / 12) * 2 * PI.toFloat() + rot * PI.toFloat() / 180f
            drawCircle(
                color = DIWALI_COLORS[(d + 2) % DIWALI_COLORS.size].copy(alpha = 0.75f),
                radius = 1.8.dp.toPx(),
                center = Offset(centre.x + cos(a) * r * 0.6f, centre.y + sin(a) * r * 0.6f)
            )
        }

        // inner petal ring, counter-turning
        for (p in 0 until 6) {
            val a = (p.toFloat() / 6) * 2 * PI.toFloat() - rot * 1.6f * PI.toFloat() / 180f
            val pc = Offset(centre.x + cos(a) * r * 0.36f, centre.y + sin(a) * r * 0.36f)
            rotate(degrees = a * 180f / PI.toFloat() + 90f, pivot = pc) {
                drawOval(
                    color = Color(0xFFFF4081).copy(alpha = 0.8f),
                    topLeft = Offset(pc.x - 2.3.dp.toPx(), pc.y - 4.8.dp.toPx()),
                    size = Size(4.6.dp.toPx(), 9.6.dp.toPx())
                )
            }
        }

        // lit centre
        drawCircle(color = GOLD, radius = 5.dp.toPx(), center = centre)
        drawCircle(color = Color(0xFFFF5722), radius = 2.4.dp.toPx(), center = centre)
    }
}

/**
 * Ground crackers: bursts of sparks with gravity and short fading trails, each at its own spot
 * and on its own cycle, with a white flash at the instant of the burst. Four sites, sixteen
 * sparks each — down from six sites of twenty-six.
 */
private fun DrawScope.drawFirecrackersLight(t: Float) {
    val gravity = 260.dp.toPx()
    val sites = 4
    for (b in 0 until sites) {
        val cx = size.width * (0.12f + 0.24f * b + 0.05f * hash01(b * 7 + 1))
        val cy = size.height * (0.52f + 0.12f * hash01(b * 13 + 2))
        val cycle = 2.6f + hash01(b * 3 + 5) * 1.3f
        val lt = (t + b * 0.97f) % cycle
        val life = 1.15f
        if (lt > life) continue

        if (lt < 0.09f) {
            drawCircle(
                color = Color.White.copy(alpha = (1f - lt / 0.09f) * 0.75f),
                radius = 10.dp.toPx(),
                center = Offset(cx, cy)
            )
        }
        val sparks = 16
        for (s in 0 until sparks) {
            val seed = b * 131 + s * 17
            val angle = (s.toFloat() / sparks) * 2 * PI.toFloat() + hash01(seed) * 0.6f
            val speed = (90f + 200f * hash01(seed + 9)).dp.toPx()
            val vx = cos(angle) * speed
            val vy = sin(angle) * speed
            fun pos(time: Float) = Offset(
                cx + vx * time,
                cy + vy * time + 0.5f * gravity * time * time
            )
            val alpha = (1f - lt / life).coerceIn(0f, 1f)
            val color = DIWALI_COLORS[(s + b) % DIWALI_COLORS.size].copy(alpha = alpha * 0.9f)
            val trailStart = pos((lt - 0.05f).coerceAtLeast(0f))
            val head = pos(lt)
            drawLine(color, trailStart, head, strokeWidth = 1.8.dp.toPx())
            drawCircle(
                color = color,
                radius = (2.1f - 1.4f * (lt / life)).dp.toPx(),
                center = head
            )
        }
    }
}

/**
 * Rockets: a sparkling climb, a flash, then a wide burst of falling sparks. Two rockets share
 * the sky on staggered cycles, each with its own colour and burst height.
 */
private fun DrawScope.drawRocketsLight(t: Float) {
    val gravity = 210.dp.toPx()
    for (r in 0 until 2) {
        val cycle = 3.4f + r * 0.7f
        val lt = (t + r * 1.43f) % cycle
        val riseTime = 0.85f
        val burstLife = 1.5f
        val startX = size.width * (0.25f + 0.5f * r + 0.06f * hash01(r * 31 + 4))
        val burstY = size.height * (0.15f + 0.13f * hash01(r * 17 + 6))
        val startY = size.height * 1.02f
        val baseColor = DIWALI_COLORS[(r * 2) % DIWALI_COLORS.size]

        if (lt < riseTime) {
            val p = lt / riseTime
            val eased = p * p * (3f - 2f * p)
            val x = startX + sin(p * 9f + r * 2f) * 2.dp.toPx()
            val y = startY + (burstY - startY) * eased
            // spark trail under the climbing rocket
            for (k in 1..5) {
                val ty = y + k * 7.dp.toPx()
                if (ty > size.height) continue
                val tx = x + (hash01(r * 91 + k * 7 + lt.toInt() * 13) - 0.5f) * 7.dp.toPx()
                val a = (1f - k / 6f) * 0.75f
                drawCircle(
                    color = (if (k % 2 == 0) GOLD else Color(0xFFFF7043)).copy(alpha = a),
                    radius = (2f - k * 0.28f).dp.toPx(),
                    center = Offset(tx, ty)
                )
            }
            // glowing head
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color.White.copy(alpha = 0.9f), Color.Transparent),
                    center = Offset(x, y),
                    radius = 8.dp.toPx()
                ),
                radius = 8.dp.toPx(),
                center = Offset(x, y)
            )
            drawCircle(color = Color.White, radius = 2.2.dp.toPx(), center = Offset(x, y))
        } else if (lt < riseTime + burstLife) {
            val bt = lt - riseTime
            if (bt < 0.07f) {
                drawCircle(
                    color = Color.White.copy(alpha = (1f - bt / 0.07f) * 0.85f),
                    radius = 14.dp.toPx(),
                    center = Offset(startX, burstY)
                )
            }
            val sparks = 22
            for (s in 0 until sparks) {
                val seed = r * 733 + s * 29
                val angle = s * 2.39996f + hash01(seed) * 0.35f // golden-angle spread
                val speed = (70f + 235f * hash01(seed + 11)).dp.toPx()
                val vx = cos(angle) * speed
                val vy = sin(angle) * speed
                fun pos(time: Float) = Offset(
                    startX + vx * time,
                    burstY + vy * time + 0.5f * gravity * time * time
                )
                val fade = (1f - bt / burstLife).coerceIn(0f, 1f)
                val twinkle = 0.75f + 0.25f * sin(bt * 26f + s * 2.3f)
                val color = when (s % 3) {
                    0 -> baseColor
                    1 -> GOLD
                    else -> Color.White
                }.copy(alpha = fade * twinkle)
                val head = pos(bt)
                val tail = pos((bt - 0.045f).coerceAtLeast(0f))
                drawLine(color, tail, head, strokeWidth = 1.9.dp.toPx())
                drawCircle(
                    color = color,
                    radius = (2.2f - 1.5f * (bt / burstLife)).dp.toPx(),
                    center = head
                )
            }
        }
    }
}
