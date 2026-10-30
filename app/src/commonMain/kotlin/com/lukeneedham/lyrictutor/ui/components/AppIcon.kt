package com.lukeneedham.lyrictutor.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.lukeneedham.lyrictutor.ui.theme.AppTheme

/** The few icons the app needs, drawn by hand on a 24 by 24 grid: the app has no icon library. */
internal enum class AppIcon { Back, Chevron, Sliders, Arrow, Play, Close, Eye, EyeOff, Check, Refresh, Note, External }

@Composable
internal fun AppIconImage(
    icon: AppIcon,
    modifier: Modifier = Modifier,
    tint: Color = AppTheme.current.onBackground,
    iconSize: Dp = 24.dp,
) {
    Canvas(modifier = modifier.size(iconSize)) {
        val unit = this.size.minDimension / 24f
        val stroke = Stroke(width = 2f * unit, cap = StrokeCap.Round, join = StrokeJoin.Round)

        fun point(x: Float, y: Float) = Offset(x * unit, y * unit)

        fun polyline(vararg points: Pair<Float, Float>) {
            val path = Path()
            points.forEachIndexed { index, (x, y) ->
                if (index == 0) path.moveTo(x * unit, y * unit) else path.lineTo(x * unit, y * unit)
            }
            drawPath(path, tint, style = stroke)
        }

        fun ring(x: Float, y: Float, radius: Float) = drawCircle(tint, radius * unit, point(x, y), style = stroke)

        when (icon) {
            AppIcon.Back -> polyline(15f to 5f, 8f to 12f, 15f to 19f)
            AppIcon.Chevron -> polyline(9f to 5f, 16f to 12f, 9f to 19f)
            AppIcon.Arrow -> {
                polyline(5f to 12f, 19f to 12f)
                polyline(13f to 6f, 19f to 12f, 13f to 18f)
            }
            AppIcon.Sliders -> {
                polyline(4f to 8f, 8f to 8f)
                polyline(13f to 8f, 20f to 8f)
                ring(10.5f, 8f, 2.5f)
                polyline(4f to 16f, 11f to 16f)
                polyline(16f to 16f, 20f to 16f)
                ring(13.5f, 16f, 2.5f)
            }
            AppIcon.Play -> {
                val path = Path()
                path.moveTo(8f * unit, 5f * unit)
                path.lineTo(19f * unit, 12f * unit)
                path.lineTo(8f * unit, 19f * unit)
                path.close()
                drawPath(path, tint)
            }
            AppIcon.Close -> {
                polyline(6f to 6f, 18f to 18f)
                polyline(18f to 6f, 6f to 18f)
            }
            AppIcon.Eye, AppIcon.EyeOff -> {
                drawOval(tint, topLeft = point(2f, 6.5f), size = Size(20f * unit, 11f * unit), style = stroke)
                ring(12f, 12f, 2.8f)
                if (icon == AppIcon.EyeOff) polyline(4f to 4f, 20f to 20f)
            }
            AppIcon.Check -> polyline(5f to 12.5f, 10f to 17.5f, 19f to 7f)
            AppIcon.Refresh -> {
                drawArc(
                    color = tint,
                    startAngle = -50f,
                    sweepAngle = 290f,
                    useCenter = false,
                    topLeft = point(5f, 5f),
                    size = Size(14f * unit, 14f * unit),
                    style = stroke,
                )
                polyline(19f to 3f, 19f to 8f, 14f to 8f)
            }
            AppIcon.External -> {
                polyline(10f to 5f, 5f to 5f, 5f to 19f, 19f to 19f, 19f to 14f)
                polyline(13f to 4f, 20f to 4f, 20f to 11f)
                polyline(20f to 4f, 11f to 13f)
            }
            AppIcon.Note -> {
                polyline(14f to 4f, 14f to 16.5f)
                polyline(14f to 4f, 19f to 7.5f)
                drawCircle(tint, 3.5f * unit, point(10.5f, 17f))
            }
        }
    }
}
