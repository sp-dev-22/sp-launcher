package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.util.DigiBrainMood

/**
 * Digi Brain's own face (v1.19.0) — drawn on a canvas, never a real emoji, so
 * it looks the same on every phone and font. Four moods, from the days since
 * Digi Brain was last used: happy (used today), ok (1-2 days), sad (3-6 days),
 * very sad (7+ days). Static — no animation, per the battery rule.
 */
@Composable
fun DigiBrainMoodFace(
    mood: DigiBrainMood,
    size: Dp = 28.dp,
    modifier: Modifier = Modifier
) {
    val faceColor = when (mood) {
        DigiBrainMood.HAPPY -> Color(0xFFFFC107)
        DigiBrainMood.OK -> Color(0xFFFFD54F)
        DigiBrainMood.SAD -> Color(0xFF90A4AE)
        DigiBrainMood.VERY_SAD -> Color(0xFF78909C)
    }
    val featureColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)

    Canvas(
        modifier = modifier.size(size)
    ) {
        val radius = this.size.minDimension / 2f
        val center = Offset(this.size.width / 2f, this.size.height / 2f)

        // Face
        drawCircle(color = faceColor, radius = radius, center = center)

        // Eyes
        val eyeDx = radius * 0.38f
        val eyeDy = radius * 0.30f
        val eyeRadius = radius * 0.11f
        val leftEye = Offset(center.x - eyeDx, center.y - eyeDy)
        val rightEye = Offset(center.x + eyeDx, center.y - eyeDy)
        when (mood) {
            // Happy: closed, smiling eyes (upward arcs).
            DigiBrainMood.HAPPY -> {
                val arcSize = Size(eyeRadius * 3f, eyeRadius * 2f)
                drawArc(
                    color = featureColor,
                    startAngle = 180f,
                    sweepAngle = 180f,
                    useCenter = false,
                    topLeft = Offset(leftEye.x - eyeRadius * 1.5f, leftEye.y - eyeRadius),
                    size = arcSize,
                    style = Stroke(width = radius * 0.09f)
                )
                drawArc(
                    color = featureColor,
                    startAngle = 180f,
                    sweepAngle = 180f,
                    useCenter = false,
                    topLeft = Offset(rightEye.x - eyeRadius * 1.5f, rightEye.y - eyeRadius),
                    size = arcSize,
                    style = Stroke(width = radius * 0.09f)
                )
            }
            // Sadder: drooping eyes (small downward arcs) on the saddest face, dots otherwise.
            DigiBrainMood.VERY_SAD -> {
                val arcSize = Size(eyeRadius * 3f, eyeRadius * 2f)
                drawArc(
                    color = featureColor,
                    startAngle = 0f,
                    sweepAngle = 180f,
                    useCenter = false,
                    topLeft = Offset(leftEye.x - eyeRadius * 1.5f, leftEye.y - eyeRadius),
                    size = arcSize,
                    style = Stroke(width = radius * 0.09f)
                )
                drawArc(
                    color = featureColor,
                    startAngle = 0f,
                    sweepAngle = 180f,
                    useCenter = false,
                    topLeft = Offset(rightEye.x - eyeRadius * 1.5f, rightEye.y - eyeRadius),
                    size = arcSize,
                    style = Stroke(width = radius * 0.09f)
                )
            }
            else -> {
                drawCircle(color = featureColor, radius = eyeRadius, center = leftEye)
                drawCircle(color = featureColor, radius = eyeRadius, center = rightEye)
            }
        }

        // Mouth: smile, small smile, flat line, frown.
        val mouthWidth = radius * 0.9f
        val mouthHeight = radius * 0.55f
        val mouthLeft = center.x - mouthWidth / 2f
        when (mood) {
            DigiBrainMood.HAPPY -> {
                drawArc(
                    color = featureColor,
                    startAngle = 15f,
                    sweepAngle = 150f,
                    useCenter = false,
                    topLeft = Offset(mouthLeft, center.y + radius * 0.02f),
                    size = Size(mouthWidth, mouthHeight),
                    style = Stroke(width = radius * 0.10f)
                )
            }
            DigiBrainMood.OK -> {
                drawArc(
                    color = featureColor,
                    startAngle = 25f,
                    sweepAngle = 130f,
                    useCenter = false,
                    topLeft = Offset(mouthLeft, center.y + radius * 0.10f),
                    size = Size(mouthWidth, mouthHeight * 0.8f),
                    style = Stroke(width = radius * 0.10f)
                )
            }
            DigiBrainMood.SAD -> {
                drawLine(
                    color = featureColor,
                    start = Offset(mouthLeft + radius * 0.12f, center.y + radius * 0.45f),
                    end = Offset(mouthLeft + mouthWidth - radius * 0.12f, center.y + radius * 0.45f),
                    strokeWidth = radius * 0.10f
                )
            }
            DigiBrainMood.VERY_SAD -> {
                drawArc(
                    color = featureColor,
                    startAngle = 195f,
                    sweepAngle = 150f,
                    useCenter = false,
                    topLeft = Offset(mouthLeft, center.y + radius * 0.30f),
                    size = Size(mouthWidth, mouthHeight),
                    style = Stroke(width = radius * 0.10f)
                )
            }
        }
    }
}
