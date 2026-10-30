package com.lukeneedham.lyrictutor.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lukeneedham.lyrictutor.data.track.TrackInfo
import com.lukeneedham.lyrictutor.generated.resources.Res
import com.lukeneedham.lyrictutor.generated.resources.song_cover_description
import com.lukeneedham.lyrictutor.ui.components.AppText
import com.lukeneedham.lyrictutor.ui.components.CoverArt
import com.lukeneedham.lyrictutor.ui.theme.AppTheme
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

/** The cover and the title of the song, small, for the middle of the toolbar once the page is scrolled past them. Tapping it is [onClick]. */
@Composable
internal fun SongToolbarTitle(track: TrackInfo, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(percent = 50))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        CoverArt(
            trackId = track.trackId,
            coverUrl = track.coverUrl,
            contentDescription = stringResource(Res.string.song_cover_description),
            size = 32.dp,
            cornerRadius = 8.dp,
        )
        AppText(
            text = track.title,
            size = 16.sp,
            weight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false),
        )
    }
}

@Preview
@Composable
private fun Preview() {
    AppTheme {
        SongToolbarTitle(
            track = TrackInfo(
                trackId = "6GiCszfL4D2GlCU8tFU3sR",
                title = "Si Me Voy (with The Marías)",
                searchTitle = "Si Me Voy",
                artists = listOf("Cuco", "The Marías"),
                coverUrl = null,
                durationSeconds = 203,
                releaseDate = "2023-03-10",
            ),
            onClick = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}
