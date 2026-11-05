package com.lukeneedham.lyrictutor.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import kotlin.math.abs
import kotlin.math.sign
import kotlinx.coroutines.launch

/** Lets [content] be swiped away to either side: past [DISMISS_FRACTION] of its width it slides out and [onDismiss] is called; short of that it springs back. */
@Composable
internal fun SwipeToDismiss(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val offset = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    var widthPx by remember { mutableFloatStateOf(1f) }
    val currentOnDismiss by rememberUpdatedState(onDismiss)
    Box(
        modifier = modifier
            .onSizeChanged { widthPx = it.width.toFloat().coerceAtLeast(1f) }
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragEnd = {
                        scope.launch {
                            if (abs(offset.value) > widthPx * DISMISS_FRACTION) {
                                offset.animateTo(sign(offset.value) * widthPx)
                                currentOnDismiss()
                            } else {
                                offset.animateTo(0f)
                            }
                        }
                    },
                    onDragCancel = { scope.launch { offset.animateTo(0f) } },
                    onHorizontalDrag = { change, amount ->
                        change.consume()
                        scope.launch { offset.snapTo(offset.value + amount) }
                    },
                )
            },
    ) {
        Box(
            modifier = Modifier.graphicsLayer {
                translationX = offset.value
                alpha = 1f - (abs(offset.value) / widthPx).coerceIn(0f, 1f) * 0.7f
            },
        ) {
            content()
        }
    }
}

private const val DISMISS_FRACTION = 0.4f
