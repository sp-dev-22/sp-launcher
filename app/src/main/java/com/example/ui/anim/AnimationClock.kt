package com.example.ui.anim

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import kotlin.math.floor
import kotlin.math.sin

/**
 * A deliberately slow animation clock for the theme animations (v1.14.0).
 *
 * The v1.11.3 engine re-rendered on every display frame, which kept the GPU busy and
 * drank the battery — the reason all animations were removed in v1.11.5. The lighter
 * layers update at roughly 30 frames per second instead, which is plenty for
 * lamps, sparks and falling snow, and they only run while their layer is mounted
 * (Home screen, own theme active, Battery Saver off).
 */
@Composable
fun rememberLightAnimationTime(): Float {
    var timeSec by remember { mutableStateOf(0f) }
    LaunchedEffect(Unit) {
        val start = System.nanoTime()
        while (true) {
            delay(33L)
            timeSec = (System.nanoTime() - start) / 1_000_000_000f
        }
    }
    return timeSec
}

/** Deterministic 0..1 pseudo-random from an integer seed, so every particle keeps its own character. */
internal fun hash01(n: Int): Float {
    val x = sin(n * 127.1 + 311.7) * 43758.5453
    return (x - floor(x)).toFloat()
}
