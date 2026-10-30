package com.lukeneedham.lyrictutor.screenshot

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import com.lukeneedham.lyrictutor.data.art.ArtColors
import com.lukeneedham.lyrictutor.data.annotation.LineAnnotation
import com.lukeneedham.lyrictutor.data.lyrics.LyricLine
import com.lukeneedham.lyrictutor.data.lyrics.Lyrics
import com.lukeneedham.lyrictutor.data.lyrics.LyricsSourceKind
import com.lukeneedham.lyrictutor.data.track.TrackInfo
import com.lukeneedham.lyrictutor.data.wiktionary.WiktionaryResult
import com.lukeneedham.lyrictutor.data.wiktionary.WiktionarySense
import com.lukeneedham.lyrictutor.ui.AnnotationStatus
import com.lukeneedham.lyrictutor.ui.LyricsUiState
import com.lukeneedham.lyrictutor.ui.SelectedWord
import com.lukeneedham.lyrictutor.ui.SongPageContent
import com.lukeneedham.lyrictutor.ui.SongUiState
import com.lukeneedham.lyrictutor.ui.WiktionaryUiState
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
            LyricLine("Uno dos tres cuatro"),
            LyricLine("Cinco seis siete ocho"),
            LyricLine("Nueve diez once doce"),
        ),
        source = LyricsSourceKind.Lrclib,
    )
    val annotations = mapOf(
        0 to LineAnnotation(index = 0, translation = "One two three four", language = "Spanish"),
        1 to LineAnnotation(index = 1, translation = "Five six seven eight", language = "Spanish"),
        2 to LineAnnotation(index = 2, translation = "Nine ten eleven twelve", language = "Spanish"),
    )
    val result = WiktionaryResult(
        title = "dos",
        language = "Spanish",
        url = "https://en.wiktionary.org/wiki/dos#Spanish",
        senses = listOf(WiktionarySense(partOfSpeech = "Numeral", glosses = listOf("two"))),
    )
    val state = SongUiState.Loaded(
        track = track,
        artColors = ArtColors(primary = Color(0xFF1F8A8A), secondary = Color(0xFFE8833A)),
        lyrics = LyricsUiState.Found(lyrics),
        annotations = annotations,
        selectedWord = SelectedWord(surface = "dos", language = "Spanish", lineIndex = 0, tokenIndex = 1),
        wiktionary = WiktionaryUiState.Found(result),
    )
    AppTheme {
        SongPageContent(
            state = state,
            onBack = {},
            onRetry = {},
            onRetryLyrics = {},
            onOpenPaste = {},
            onPasteChange = {},
            onSavePaste = {},
            onClosePaste = {},
            onRetryAnnotations = {},
            onTranslateAgain = {},
            onOpenSettings = {},
            onWordClick = {},
            onWordDismiss = {},
        )
    }
}

/** Rendered by CI to docs/screenshots/<position in order.txt>-SongPageTranslating.png. */
@Preview
@Composable
private fun SongPageTranslatingScreenshot() {
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
            LyricLine("Uno dos tres cuatro"),
            LyricLine("Cinco seis siete ocho"),
            LyricLine("Nueve diez once doce"),
        ),
        source = LyricsSourceKind.Lrclib,
    )
    val state = SongUiState.Loaded(
        track = track,
        lyrics = LyricsUiState.Found(lyrics),
        annotations = mapOf(0 to LineAnnotation(index = 0, translation = "One two three four", language = "Spanish")),
        annotationStatus = AnnotationStatus.Loading,
    )
    AppTheme {
        SongPageContent(
            state = state,
            onBack = {},
            onRetry = {},
            onRetryLyrics = {},
            onOpenPaste = {},
            onPasteChange = {},
            onSavePaste = {},
            onClosePaste = {},
            onRetryAnnotations = {},
            onTranslateAgain = {},
            onOpenSettings = {},
            onWordClick = {},
            onWordDismiss = {},
        )
    }
}
