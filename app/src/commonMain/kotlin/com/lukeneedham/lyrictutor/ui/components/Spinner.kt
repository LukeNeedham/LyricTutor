package com.lukeneedham.lyrictutor.ui.components

import androidx.compose.animation.core.LinearEasing
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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.lukeneedham.lyrictutor.ui.theme.AppTheme

/** A turning arc, for something that is loading. */
@Composable
internal fun Spinner(
    modifier: Modifier = Modifier,
    diameter: Dp = 24.dp,
    color: Color = AppTheme.current.primary,
) {
    val transition = rememberInfiniteTransition(label = "spinner")
    val angle by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(durationMillis = 900, easing = LinearEasing), RepeatMode.Restart),
        label = "angle",
    )
    Canvas(modifier = modifier.size(diameter)) {
        val width = this.size.minDimension / 8f
        drawArc(
            color = color,
            startAngle = angle,
            sweepAngle = 270f,
            useCenter = false,
            topLeft = Offset(width / 2f, width / 2f),
            size = Size(this.size.width - width, this.size.height - width),
            style = Stroke(width = width, cap = StrokeCap.Round),
        )
    }
}
