package com.example.ui.anim

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
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
import com.example.data.model.AnimationEffect
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.floor
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

/** Deterministic 0..1 pseudo-random from an integer seed, so every particle keeps its own character. */
private fun hash(n: Int): Float {
    val x = sin(n * 127.1 + 311.7) * 43758.5453
    return (x - floor(x)).toFloat()
}

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
 * Every effect has its own toggle in Settings > Auto & Animations and its own sense of time:
 * particles have gravity and fading trails, each lamp flickers on its own rhythm, and nothing is
 * locked to one shared clock, so the screen never pulses in sync. The canvas takes no pointer
 * input, so it never blocks a tap.
 *
 * Everything is drawn inside the launcher only — there is deliberately no system overlay.
 */
@Composable
fun DiwaliAnimationLayer(
    effects: Set<String>,
    modifier: Modifier = Modifier
) {
    var timeSec by remember { mutableStateOf(0f) }
    LaunchedEffect(Unit) {
        var start = -1L
        while (true) {
            withFrameNanos { frame ->
                if (start < 0L) start = frame
                timeSec = (frame - start) / 1_000_000_000f
            }
        }
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        if (effects.isEmpty()) return@Canvas
        val t = timeSec
        if (AnimationEffect.STARS.key in effects) drawStars(t)
        if (AnimationEffect.LAMP.key in effects) drawLamps(t)
        if (AnimationEffect.RANGOLI.key in effects) drawRangoli(t)
        if (AnimationEffect.SKY_LANTERNS.key in effects) drawSkyLanterns(t)
        if (AnimationEffect.CHAKRI.key in effects) drawChakri(t)
        if (AnimationEffect.FIRECRACKERS.key in effects) drawFirecrackers(t)
        if (AnimationEffect.ROCKETS.key in effects) drawRockets(t)
        if (AnimationEffect.PETALS.key in effects) drawPetals(t)
    }
}

/** A starfield of seventy stars, each twinkling at its own speed; every ninth is a sparkle cross. */
private fun DrawScope.drawStars(t: Float) {
    val starColors = listOf(Color.White, PALE_GOLD, Color(0xFFB2EBF2))
    for (i in 0 until 70) {
        val x = hash(i * 3 + 1) * size.width
        val y = hash(i * 3 + 2) * size.height * 0.88f
        val speed = 0.6f + hash(i * 3 + 3) * 2.4f
        val tw = 0.5f + 0.5f * sin(t * speed + i * 1.37f)
        val color = starColors[i % starColors.size].copy(alpha = 0.22f + 0.68f * tw)
        if (i % 9 == 0) {
            val arm = (4.5f + 4.5f * tw).dp.toPx()
            val w = 1.1.dp.toPx()
            drawLine(color, Offset(x - arm, y), Offset(x + arm, y), strokeWidth = w)
            drawLine(color, Offset(x, y - arm), Offset(x, y + arm), strokeWidth = w)
            drawCircle(color, radius = 1.5.dp.toPx(), center = Offset(x, y))
        } else {
            val r = (0.8f + hash(i + 500) * 1.2f).dp.toPx() * (0.7f + 0.5f * tw)
            drawCircle(color, radius = r, center = Offset(x, y))
        }
    }
}

/** A row of diyas hanging from the top edge; each sways and flickers on its own rhythm. */
private fun DrawScope.drawLamps(t: Float) {
    val count = 9
    for (i in 0 until count) {
        val baseX = size.width * (i + 0.5f) / count
        val threadLength = size.height * (0.04f + 0.025f * hash(i * 5 + 1))
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
                quadraticBezierTo(cx + fw, cy - fh * 0.35f, cx + fw * 0.55f, cy + fh * 0.3f)
                quadraticBezierTo(cx + fw * 0.3f, cy + fh * 0.6f, cx, cy + fh * 0.6f)
                quadraticBezierTo(cx - fw * 0.3f, cy + fh * 0.6f, cx - fw * 0.55f, cy + fh * 0.3f)
                quadraticBezierTo(cx - fw, cy - fh * 0.35f, cx, cy - fh)
                close()
            }
            drawPath(flame, Color(0xFFFFA000).copy(alpha = 0.95f))
            val inner = Path().apply {
                val s = 0.52f
                moveTo(cx, cy - fh * s)
                quadraticBezierTo(cx + fw * s, cy - fh * 0.2f, cx + fw * 0.3f, cy + fh * 0.3f)
                quadraticBezierTo(cx, cy + fh * 0.45f, cx - fw * 0.3f, cy + fh * 0.3f)
                quadraticBezierTo(cx - fw * s, cy - fh * 0.2f, cx, cy - fh * s)
                close()
            }
            drawPath(inner, PALE_GOLD.copy(alpha = 0.95f))
        }
    }
}

/**
 * One rangoli in each bottom corner: two petal rings turning against each other around a lit
 * centre, with a dotted ring between them.
 */
private fun DrawScope.drawRangoli(t: Float) {
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

        // outer petal ring: 16 petals standing around the circle
        for (p in 0 until 16) {
            val a = (p.toFloat() / 16) * 2 * PI.toFloat() + rot * PI.toFloat() / 180f
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
        for (d in 0 until 20) {
            val a = (d.toFloat() / 20) * 2 * PI.toFloat() + rot * PI.toFloat() / 180f
            drawCircle(
                color = DIWALI_COLORS[(d + 2) % DIWALI_COLORS.size].copy(alpha = 0.75f),
                radius = 1.8.dp.toPx(),
                center = Offset(centre.x + cos(a) * r * 0.6f, centre.y + sin(a) * r * 0.6f)
            )
        }

        // inner petal ring, counter-turning
        for (p in 0 until 8) {
            val a = (p.toFloat() / 8) * 2 * PI.toFloat() - rot * 1.6f * PI.toFloat() / 180f
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

/** Paper sky lanterns with a tapered body and a flickering heart, drifting up and away. */
private fun DrawScope.drawSkyLanterns(t: Float) {
    val count = 7
    for (i in 0 until count) {
        val cycle = 26f + i * 3f
        val progress = ((t / cycle) + i * 0.17f) % 1f
        val scale = 0.8f + 0.5f * hash(i * 7 + 3)
        val w = 13.dp.toPx() * scale
        val h = 17.dp.toPx() * scale
        val drift = if (i % 2 == 0) 1f else -1f
        val x = size.width * (0.09f + 0.125f * i) +
            sin(t * 0.5f + i * 2f) * 10.dp.toPx() + progress * size.width * 0.05f * drift
        val y = size.height * (1.08f - progress * 1.32f)
        val fade = if (progress < 0.12f) progress / 0.12f else (1f - progress) / 0.3f
        val alpha = fade.coerceIn(0f, 1f) * 0.9f
        val flick = 0.85f + 0.15f * sin(t * 9f + i * 3.1f)

        // glow around the flame
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFFFFB300).copy(alpha = alpha * 0.4f * flick), Color.Transparent),
                center = Offset(x, y + h * 0.32f),
                radius = w * 0.95f
            ),
            radius = w * 0.95f,
            center = Offset(x, y + h * 0.32f)
        )
        // tapered paper body
        val topW = w * 0.74f
        val botW = w * 0.56f
        val body = Path().apply {
            moveTo(x - topW / 2, y - h / 2)
            lineTo(x + topW / 2, y - h / 2)
            quadraticBezierTo(x + w / 2, y, x + botW / 2, y + h / 2)
            lineTo(x - botW / 2, y + h / 2)
            quadraticBezierTo(x - w / 2, y, x - topW / 2, y - h / 2)
            close()
        }
        drawPath(
            body,
            Brush.verticalGradient(
                colors = listOf(
                    Color(0xFFFFB74D).copy(alpha = alpha),
                    Color(0xFFFF7043).copy(alpha = alpha)
                ),
                startY = y - h / 2,
                endY = y + h / 2
            )
        )
        // warm heart of the lantern
        drawOval(
            color = PALE_GOLD.copy(alpha = alpha * 0.85f * flick),
            topLeft = Offset(x - w * 0.2f, y + h * 0.08f),
            size = Size(w * 0.4f, h * 0.34f)
        )
        // rims
        drawLine(
            color = Color(0xFFBF360C).copy(alpha = alpha),
            start = Offset(x - topW / 2, y - h / 2),
            end = Offset(x + topW / 2, y - h / 2),
            strokeWidth = 1.6.dp.toPx()
        )
        drawLine(
            color = Color(0xFFBF360C).copy(alpha = alpha),
            start = Offset(x - botW / 2, y + h / 2),
            end = Offset(x + botW / 2, y + h / 2),
            strokeWidth = 1.6.dp.toPx()
        )
    }
}

/** A ground spinner (chakri): three rings of sparks turning at different speeds, with trails. */
private fun DrawScope.drawChakri(t: Float) {
    val spots = listOf(
        Offset(size.width * 0.28f, size.height * 0.72f),
        Offset(size.width * 0.74f, size.height * 0.63f)
    )
    val g = 1.dp.toPx()
    spots.forEachIndexed { index, centre ->
        val dir = if (index == 0) 1f else -1f
        // glow bed
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFFFFC107).copy(alpha = 0.22f), Color.Transparent),
                center = centre,
                radius = 24 * g
            ),
            radius = 24 * g,
            center = centre
        )
        val rings = listOf(
            Triple(18 * g, 16, 300f),
            Triple(12 * g, 12, -430f),
            Triple(6.5f * g, 8, 560f)
        )
        rings.forEachIndexed { ringIndex, (radius, count, speed) ->
            for (s in 0 until count) {
                val base = (s.toFloat() / count) * 2 * PI.toFloat()
                val aNow = base + t * speed * dir * PI.toFloat() / 180f
                val aPrev = base + (t - 0.045f) * speed * dir * PI.toFloat() / 180f
                val color = DIWALI_COLORS[(s + ringIndex * 2) % DIWALI_COLORS.size]
                drawLine(
                    color = color.copy(alpha = 0.45f),
                    start = Offset(centre.x + cos(aPrev) * radius, centre.y + sin(aPrev) * radius),
                    end = Offset(centre.x + cos(aNow) * radius, centre.y + sin(aNow) * radius),
                    strokeWidth = 2 * g
                )
                drawCircle(
                    color = color.copy(alpha = 0.9f),
                    radius = 2.3f * g,
                    center = Offset(centre.x + cos(aNow) * radius, centre.y + sin(aNow) * radius)
                )
            }
        }
        // the hot centre
        drawCircle(color = PALE_GOLD.copy(alpha = 0.95f), radius = 3.4f * g, center = centre)
        drawCircle(color = Color.White.copy(alpha = 0.8f), radius = 1.6f * g, center = centre)
    }
}

/**
 * Ground crackers: bursts of sparks with gravity and short fading trails, each at its own spot
 * and on its own cycle, with a white flash at the instant of the burst.
 */
private fun DrawScope.drawFirecrackers(t: Float) {
    val gravity = 260.dp.toPx()
    val sites = 6
    for (b in 0 until sites) {
        val cx = size.width * (0.1f + 0.16f * b + 0.05f * hash(b * 7 + 1))
        val cy = size.height * (0.52f + 0.12f * hash(b * 13 + 2))
        val cycle = 2.6f + hash(b * 3 + 5) * 1.3f
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
        val sparks = 26
        for (s in 0 until sparks) {
            val seed = b * 131 + s * 17
            val angle = (s.toFloat() / sparks) * 2 * PI.toFloat() + hash(seed) * 0.6f
            val speed = (90f + 200f * hash(seed + 9)).dp.toPx()
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
 * Rockets: a sparkling climb, a flash, then a wide burst of falling sparks. Three rockets share
 * the sky on staggered cycles, each with its own colour and burst height.
 */
private fun DrawScope.drawRockets(t: Float) {
    val gravity = 210.dp.toPx()
    for (r in 0 until 3) {
        val cycle = 3.4f + r * 0.7f
        val lt = (t + r * 1.43f) % cycle
        val riseTime = 0.85f
        val burstLife = 1.5f
        val startX = size.width * (0.2f + 0.28f * r + 0.06f * hash(r * 31 + 4))
        val burstY = size.height * (0.15f + 0.13f * hash(r * 17 + 6))
        val startY = size.height * 1.02f
        val baseColor = DIWALI_COLORS[(r * 2) % DIWALI_COLORS.size]

        if (lt < riseTime) {
            val p = lt / riseTime
            val eased = p * p * (3f - 2f * p)
            val x = startX + sin(p * 9f + r * 2f) * 2.dp.toPx()
            val y = startY + (burstY - startY) * eased
            // spark trail under the climbing rocket
            for (k in 1..9) {
                val ty = y + k * 7.dp.toPx()
                if (ty > size.height) continue
                val tx = x + (hash(r * 91 + k * 7 + lt.toInt() * 13) - 0.5f) * 7.dp.toPx()
                val a = (1f - k / 10f) * 0.75f
                drawCircle(
                    color = (if (k % 2 == 0) GOLD else Color(0xFFFF7043)).copy(alpha = a),
                    radius = (2f - k * 0.14f).dp.toPx(),
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
            val sparks = 36
            for (s in 0 until sparks) {
                val seed = r * 733 + s * 29
                val angle = s * 2.39996f + hash(seed) * 0.35f // golden-angle spread
                val speed = (70f + 235f * hash(seed + 11)).dp.toPx()
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

/** Marigold petals in real leaf shapes, fluttering as they fall — the turning is visible now. */
private fun DrawScope.drawPetals(t: Float) {
    val count = 22
    for (i in 0 until count) {
        val fallSeconds = 7f + hash(i * 3 + 8) * 5f
        val progress = ((t / fallSeconds) + i * 0.37f) % 1f
        val sway = sin(t * 0.9f + i * 1.7f) * 18.dp.toPx()
        val x = hash(i * 7 + 2) * size.width + sway
        val y = progress * size.height * 1.06f - 12.dp.toPx()
        val scaleF = 0.75f + 0.6f * hash(i * 11 + 5)
        val len = 9.5.dp.toPx() * scaleF
        val wid = 4.6.dp.toPx() * scaleF
        val angle = sin(t * (1.1f + hash(i + 40)) + i * 2f) * 55f + t * 18f * (if (i % 2 == 0) 1f else -1f)
        val color = DIWALI_COLORS[i % DIWALI_COLORS.size].copy(alpha = 0.78f)

        rotate(degrees = angle, pivot = Offset(x, y)) {
            val petal = Path().apply {
                moveTo(x, y - len / 2)
                quadraticBezierTo(x + wid, y - len * 0.12f, x, y + len / 2)
                quadraticBezierTo(x - wid, y - len * 0.12f, x, y - len / 2)
                close()
            }
            drawPath(petal, color)
            drawLine(
                color = Color.Black.copy(alpha = 0.18f),
                start = Offset(x, y - len * 0.34f),
                end = Offset(x, y + len * 0.34f),
                strokeWidth = 0.8.dp.toPx()
            )
        }
    }
}
