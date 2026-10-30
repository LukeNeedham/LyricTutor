package com.lukeneedham.lyrictutor.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import org.jetbrains.compose.ui.tooling.preview.Preview
import com.lukeneedham.lyrictutor.ui.theme.AppTheme

/** The colour a song gets when it has no cover yet (and behind the cover while it loads): always the same for the same song. */
internal fun trackColor(trackId: String, saturation: Float = 0.55f, value: Float = 0.85f): Color {
    val hue = (trackId.hashCode().toLong() and 0x7fffffffL) % 360L
    return Color.hsv(hue.toFloat(), saturation, value)
}

/** Every album cover has the same slightly rounded corners. */
private val CoverCornerRadius = 5.dp

/** The album cover, over a gradient of the song's own colour with a note on it, which stays when there is no cover. */
@Composable
internal fun CoverArt(
    trackId: String,
    coverUrl: String?,
    contentDescription: String,
    size: Dp,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(CoverCornerRadius))
            .background(Brush.linearGradient(listOf(trackColor(trackId), trackColor(trackId.reversed(), value = 0.6f)))),
        contentAlignment = Alignment.Center,
    ) {
        AppIconImage(icon = AppIcon.Note, tint = Color.White.copy(alpha = 0.85f), iconSize = size / 2)
        if (coverUrl != null) {
            AsyncImage(
                model = coverUrl,
                contentDescription = contentDescription,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Preview
@Composable
private fun Preview() {
    AppTheme {
        CoverArt(trackId = "6GiCszfL4D2GlCU8tFU3sR", coverUrl = null, contentDescription = "", size = 96.dp)
    }
}
