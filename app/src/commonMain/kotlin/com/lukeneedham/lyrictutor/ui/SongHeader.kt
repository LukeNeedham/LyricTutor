package com.lukeneedham.lyrictutor.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lukeneedham.lyrictutor.data.track.TrackIds
import com.lukeneedham.lyrictutor.data.track.TrackInfo
import com.lukeneedham.lyrictutor.generated.resources.Res
import com.lukeneedham.lyrictutor.generated.resources.song_cover_description
import com.lukeneedham.lyrictutor.generated.resources.song_play_on_spotify
import com.lukeneedham.lyrictutor.generated.resources.song_play_on_youtube
import com.lukeneedham.lyrictutor.generated.resources.song_translate_again
import com.lukeneedham.lyrictutor.ui.components.AppButton
import com.lukeneedham.lyrictutor.ui.components.AppButtonStyle
import com.lukeneedham.lyrictutor.ui.components.AppIcon
import com.lukeneedham.lyrictutor.ui.components.AppText
import com.lukeneedham.lyrictutor.ui.components.CoverArt
import com.lukeneedham.lyrictutor.ui.components.LanguageTag
import com.lukeneedham.lyrictutor.ui.theme.AppTheme
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

/** The cover, the title and artists of the song, and what can be done with it. [canTranslateAgain]: there are translations to throw away. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun SongHeader(
    track: TrackInfo,
    canTranslateAgain: Boolean,
    /** The languages of the lyrics, the most common first. */
    languages: List<String>,
    /** Whether the colours of the cover are read: the buttons wait for them, so they never show in the app's own colours first. */
    showActions: Boolean,
    onPlay: () -> Unit,
    onTranslateAgain: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val theme = AppTheme.current
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        val coverShape = RoundedCornerShape(5.dp)
        CoverArt(
            trackId = track.trackId,
            coverUrl = track.coverUrl,
            contentDescription = stringResource(Res.string.song_cover_description),
            size = 220.dp,
            modifier = Modifier.shadow(elevation = 16.dp, shape = coverShape, clip = false),
        )
        Column(
            modifier = Modifier.padding(top = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            AppText(
                text = track.title,
                size = 26.sp,
                lineHeight = 32.sp,
                weight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            AppText(
                text = track.artists.joinToString(", "),
                color = theme.onSurfaceVariant,
                size = 16.sp,
                textAlign = TextAlign.Center,
            )
        }
        if (showActions && languages.isNotEmpty()) {
            FlowRow(
                modifier = Modifier.padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                for (language in languages) LanguageTag(language = language)
            }
        }
        if (showActions) {
            FlowRow(
                modifier = Modifier.padding(top = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                when {
                    TrackIds.isByName(track.trackId) -> Unit // typed in by name: there is nowhere to play it
                    TrackIds.isYoutube(track.trackId) ->
                        AppButton(text = stringResource(Res.string.song_play_on_youtube), onClick = onPlay, icon = AppIcon.Play)
                    else -> AppButton(text = stringResource(Res.string.song_play_on_spotify), onClick = onPlay, icon = AppIcon.Play)
                }
                if (canTranslateAgain) {
                    AppButton(
                        text = stringResource(Res.string.song_translate_again),
                        onClick = onTranslateAgain,
                        style = AppButtonStyle.Secondary,
                        icon = AppIcon.Refresh,
                    )
                }
            }
        } else {
            // The room the buttons will take, so that the lyrics do not jump down when they come
            Spacer(modifier = Modifier.padding(top = 14.dp).height(48.dp))
        }
    }
}

@Preview
@Composable
private fun Preview() {
    AppTheme {
        SongHeader(
            track = TrackInfo(
                trackId = "6GiCszfL4D2GlCU8tFU3sR",
                title = "Si Me Voy (with The Marías)",
                searchTitle = "Si Me Voy",
                artists = listOf("Cuco", "The Marías"),
                coverUrl = null,
                durationSeconds = 203,
                releaseDate = "2023-03-10",
            ),
            canTranslateAgain = true,
            languages = listOf("Spanish", "English"),
            showActions = true,
            onPlay = {},
            onTranslateAgain = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}
