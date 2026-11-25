package com.example.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp

/**
 * The AI avatar's face (v1.20.0), drawn with Canvas — no emoji, no image asset.
 *
 * A round head in [bodyColor], two eyes and a smile in [faceColor]; the [mood]
 * changes the drawing (happy eyes, listening mouth, sleeping "Z", celebrate
 * blush + sparkles, ...). The same component is used in the chat header, on
 * Home, and in the floating overlay bubble, so keep the signature stable.
 *
 * The only animation is a slow breathing scale (2 s, infinite). It is cheap —
 * one float, no timers — and it runs only while the face is composed.
 */
enum class AvatarMood {
    IDLE,
    HAPPY,
    THINKING,
    LISTENING,
    SLEEPING,
    CELEBRATE
}

@Composable
fun AvatarFace(
    mood: AvatarMood,
    modifier: Modifier = Modifier,
    bodyColor: Color = Color(0xFFFFC46B),
    faceColor: Color = Color(0xFF3A2E2A)
) {
    // Slow breathing: 1.00 -> 1.04 -> 1.00 over 2 s, forever.
    val transition = rememberInfiniteTransition(label = "avatar_breathing")
    val scale by transition.animateFloat(
        initialValue = 1f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "avatar_breathing_scale"
    )

    Canvas(
        modifier = modifier
            .size(64.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
    ) {
        val r = size.minDimension / 2f
        val cx = size.width / 2f
        val cy = size.height / 2f
        val eyeY = cy - r * 0.22f
        val eyeDx = r * 0.34f
        val eyeR = r * 0.11f
        val stroke = Stroke(width = r * 0.08f, cap = StrokeCap.Round)

        // Head — slightly dimmed while sleeping.
        val headColor = if (mood == AvatarMood.SLEEPING) bodyColor.copy(alpha = 0.75f) else bodyColor
        drawCircle(color = headColor, radius = r, center = Offset(cx, cy))

        val leftEye = Offset(cx - eyeDx, eyeY)
        val rightEye = Offset(cx + eyeDx, eyeY)

        when (mood) {
            AvatarMood.IDLE -> {
                drawCircle(color = faceColor, radius = eyeR, center = leftEye)
                drawCircle(color = faceColor, radius = eyeR, center = rightEye)
                // Gentle smile: an arc through the bottom (90 degrees).
                drawArc(
                    color = faceColor,
                    startAngle = 20f,
                    sweepAngle = 140f,
                    useCenter = false,
                    topLeft = Offset(cx - r * 0.42f, cy + r * 0.05f),
                    size = androidx.compose.ui.geometry.Size(r * 0.84f, r * 0.55f),
                    style = stroke
                )
            }

            AvatarMood.HAPPY, AvatarMood.CELEBRATE -> {
                // Closed happy eyes: arcs through the top (270 degrees).
                for (eye in listOf(leftEye, rightEye)) {
                    drawArc(
                        color = faceColor,
                        startAngle = 200f,
                        sweepAngle = 140f,
                        useCenter = false,
                        topLeft = Offset(eye.x - eyeR, eye.y - eyeR),
                        size = androidx.compose.ui.geometry.Size(eyeR * 2f, eyeR * 2f),
                        style = stroke
                    )
                }
                // Open smile: a filled oval mouth.
                drawOval(
                    color = faceColor,
                    topLeft = Offset(cx - r * 0.2f, cy + r * 0.18f),
                    size = androidx.compose.ui.geometry.Size(r * 0.4f, r * 0.26f)
                )
                if (mood == AvatarMood.CELEBRATE) {
                    // Blush cheeks + two small gold sparkle dots.
                    val blush = Color(0xFFFF9AA2)
                    drawCircle(color = blush, radius = r * 0.1f, center = Offset(cx - r * 0.58f, cy + r * 0.18f))
                    drawCircle(color = blush, radius = r * 0.1f, center = Offset(cx + r * 0.58f, cy + r * 0.18f))
                    val sparkle = Color(0xFFFFD54F)
                    drawCircle(color = sparkle, radius = r * 0.06f, center = Offset(cx - r * 0.62f, cy - r * 0.62f))
                    drawCircle(color = sparkle, radius = r * 0.06f, center = Offset(cx + r * 0.62f, cy - r * 0.62f))
                }
            }

            AvatarMood.THINKING -> {
                // Left eye normal; right eye looking up.
                drawCircle(color = faceColor, radius = eyeR, center = leftEye)
                drawCircle(
                    color = faceColor,
                    radius = eyeR,
                    center = Offset(rightEye.x, rightEye.y - r * 0.07f)
                )
                // Tiny thought dot floating above the head.
                drawCircle(color = faceColor, radius = r * 0.07f, center = Offset(cx + r * 0.55f, cy - r * 0.78f))
                drawCircle(color = faceColor, radius = r * 0.045f, center = Offset(cx + r * 0.68f, cy - r * 0.92f))
                // Small flat smile.
                drawArc(
                    color = faceColor,
                    startAngle = 30f,
                    sweepAngle = 120f,
                    useCenter = false,
                    topLeft = Offset(cx - r * 0.3f, cy + r * 0.12f),
                    size = androidx.compose.ui.geometry.Size(r * 0.6f, r * 0.4f),
                    style = stroke
                )
            }

            AvatarMood.LISTENING -> {
                // Wide eyes + open round mouth.
                drawCircle(color = faceColor, radius = eyeR * 1.45f, center = leftEye)
                drawCircle(color = faceColor, radius = eyeR * 1.45f, center = rightEye)
                drawCircle(color = faceColor, radius = r * 0.14f, center = Offset(cx, cy + r * 0.38f))
            }

            AvatarMood.SLEEPING -> {
                // Closed eyes: short horizontal lines.
                for (eye in listOf(leftEye, rightEye)) {
                    drawLine(
                        color = faceColor,
                        start = Offset(eye.x - eyeR, eye.y),
                        end = Offset(eye.x + eyeR, eye.y),
                        strokeWidth = r * 0.08f,
                        cap = StrokeCap.Round
                    )
                }
                // "Z" drawn as a path, top-right of the head.
                val z = Path().apply {
                    moveTo(cx + r * 0.45f, cy - r * 0.85f)
                    lineTo(cx + r * 0.8f, cy - r * 0.85f)
                    lineTo(cx + r * 0.45f, cy - r * 0.5f)
                    lineTo(cx + r * 0.8f, cy - r * 0.5f)
                }
                drawPath(path = z, color = faceColor, style = Stroke(width = r * 0.07f, cap = StrokeCap.Round))
            }
        }
    }
}
