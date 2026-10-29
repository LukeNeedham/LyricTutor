package com.lukeneedham.lyrictutor.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.lukeneedham.lyrictutor.data.track.TrackInfo
import com.lukeneedham.lyrictutor.generated.resources.Res
import com.lukeneedham.lyrictutor.generated.resources.song_cover_description
import com.lukeneedham.lyrictutor.generated.resources.song_error_network
import com.lukeneedham.lyrictutor.generated.resources.song_error_unreadable
import com.lukeneedham.lyrictutor.generated.resources.song_loading
import com.lukeneedham.lyrictutor.generated.resources.song_retry
import com.lukeneedham.lyrictutor.ui.theme.AppTheme
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

/** All of the page's UI. `internal`: the screenshots of the app use it too. */
@Composable
internal fun SongPageContent(
    state: SongUiState,
    onRetry: () -> Unit,
) {
    val theme = AppTheme.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(theme.background)
            .safeDrawingPadding()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        when (state) {
            SongUiState.Loading -> BasicText(
                text = stringResource(Res.string.song_loading),
                style = TextStyle(color = theme.onBackground, fontFamily = theme.fontFamily, fontSize = 16.sp, lineHeight = 24.sp),
            )

            is SongUiState.Loaded -> Header(state.track)

            is SongUiState.Error -> {
                BasicText(
                    text = stringResource(
                        if (state.isNetwork) Res.string.song_error_network else Res.string.song_error_unreadable,
                    ),
                    style = TextStyle(color = theme.error, fontFamily = theme.fontFamily, fontSize = 16.sp, lineHeight = 24.sp),
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .background(theme.primary, RoundedCornerShape(24.dp))
                        .clickable(onClick = onRetry),
                    contentAlignment = Alignment.Center,
                ) {
                    BasicText(
                        text = stringResource(Res.string.song_retry),
                        style = TextStyle(
                            color = theme.onPrimary,
                            fontFamily = theme.fontFamily,
                            fontSize = 16.sp,
                            lineHeight = 24.sp,
                            fontWeight = FontWeight.Medium,
                        ),
                    )
                }
            }
        }
    }
}

@Composable
private fun Header(track: TrackInfo) {
    val theme = AppTheme.current
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        AsyncImage(
            model = track.coverUrl,
            contentDescription = stringResource(Res.string.song_cover_description),
            modifier = Modifier
                .size(160.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(theme.surface),
        )
        BasicText(
            text = track.title,
            style = TextStyle(
                color = theme.onBackground,
                fontFamily = theme.fontFamily,
                fontSize = 24.sp,
                lineHeight = 32.sp,
                fontWeight = FontWeight.SemiBold,
            ),
        )
        BasicText(
            text = track.artists.joinToString(", "),
            style = TextStyle(color = theme.onBackground, fontFamily = theme.fontFamily, fontSize = 16.sp, lineHeight = 24.sp),
        )
    }
}

/** For Android Studio while building the page. */
@Preview
@Composable
private fun Preview() {
    AppTheme {
        SongPageContent(
            state = SongUiState.Loaded(
                TrackInfo(
                    trackId = "6GiCszfL4D2GlCU8tFU3sR",
                    title = "Si Me Voy (with The Marías)",
                    searchTitle = "Si Me Voy",
                    artists = listOf("Cuco", "The Marías"),
                    coverUrl = null,
                    durationSeconds = 203,
                    releaseDate = "2023-03-10",
                ),
            ),
            onRetry = {},
        )
    }
}

@Preview
@Composable
private fun PreviewError() {
    AppTheme { SongPageContent(state = SongUiState.Error(isNetwork = true), onRetry = {}) }
}
