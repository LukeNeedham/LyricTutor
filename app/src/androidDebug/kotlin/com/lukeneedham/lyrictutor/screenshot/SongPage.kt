package com.lukeneedham.lyrictutor.screenshot

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.lukeneedham.lyrictutor.data.annotation.LineAnnotation
import com.lukeneedham.lyrictutor.data.annotation.WordAnnotation
import com.lukeneedham.lyrictutor.data.lyrics.LyricLine
import com.lukeneedham.lyrictutor.data.lyrics.Lyrics
import com.lukeneedham.lyrictutor.data.lyrics.LyricsSourceKind
import com.lukeneedham.lyrictutor.data.track.TrackInfo
import com.lukeneedham.lyrictutor.ui.LyricsUiState
import com.lukeneedham.lyrictutor.ui.SongPageContent
import com.lukeneedham.lyrictutor.ui.SongUiState
import com.lukeneedham.lyrictutor.ui.theme.AppTheme

/** Rendered by CI to docs/screenshots/<position in order.txt>-SongPage.png. */
@Preview
@Composable
private fun SongPageScreenshot() {
    val track = TrackInfo(
        trackId = "6GiCszfL4D2GlCU8tFU3sR",
        title = "Si Me Voy (with The Marías)",
        searchTitle = "Si Me Voy",
        artists = listOf("Cuco", "The Marías"),
        coverUrl = null,
        durationSeconds = 203,
        releaseDate = "2023-03-10",
    )
    val lyrics = Lyrics(
        lines = listOf(
            LyricLine("Si me voy, me voy contigo"),
            LyricLine("Y si me quedo, me quedo contigo"),
            LyricLine("Voy a quedarme contigo"),
        ),
        source = LyricsSourceKind.Lrclib,
    )
    val annotations = mapOf(
        0 to LineAnnotation(
            index = 0,
            translation = "If I go, I go with you",
            words = listOf(
                WordAnnotation(surface = "voy", lemma = "ir", meaning = "I go"),
                WordAnnotation(surface = "contigo", lemma = "contigo", meaning = "with you"),
            ),
        ),
        1 to LineAnnotation(index = 1, translation = "And if I stay, I stay with you"),
        2 to LineAnnotation(index = 2, translation = "I am going to stay with you"),
    )
    val state = SongUiState.Loaded(track = track, lyrics = LyricsUiState.Found(lyrics), annotations = annotations)
    AppTheme {
        SongPageContent(
            state = state,
            onRetry = {},
            onRetryLyrics = {},
            onOpenPaste = {},
            onPasteChange = {},
            onSavePaste = {},
            onClosePaste = {},
            onRetryAnnotations = {},
            onOpenSettings = {},
            onWordClick = {},
            onWordDismiss = {},
        )
    }
}
