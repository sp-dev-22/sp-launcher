package com.example.ui.components

import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput

/**
 * Live state for a long-press drag handle: [offset] is the current vertical drag distance
 * (read it inside a graphicsLayer lambda so the row follows the finger), and [modifier] is
 * the pointer modifier to attach to the drag handle.
 */
class DragStep internal constructor(
    val offset: State<Float>,
    val modifier: Modifier
)

/**
 * A long-press drag handle that reports discrete moves through [onStep]: -1 when the finger
 * has been dragged up by [stepPx], +1 when dragged down by [stepPx]. Each crossed threshold
 * fires one step and the offset is reduced accordingly, so dragging across several rows
 * fires several steps.
 */
@Composable
fun rememberDragStep(stepPx: Float, onStep: (Int) -> Unit): DragStep {
    val offset = remember { mutableStateOf(0f) }
    val currentOnStep by rememberUpdatedState(onStep)

    val modifier = Modifier.pointerInput(stepPx) {
        detectDragGesturesAfterLongPress(
            onDragStart = { offset.value = 0f },
            onDragEnd = { offset.value = 0f },
            onDragCancel = { offset.value = 0f },
            onDrag = { change, dragAmount ->
                change.consume()
                offset.value += dragAmount.y
                while (offset.value <= -stepPx) {
                    currentOnStep(-1)
                    offset.value += stepPx
                }
                while (offset.value >= stepPx) {
                    currentOnStep(1)
                    offset.value -= stepPx
                }
            }
        )
    }

    return DragStep(offset, modifier)
}
