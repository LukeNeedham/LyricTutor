package com.lukeneedham.lyrictutor.screenshot

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.lukeneedham.lyrictutor.data.track.TrackInfo
import com.lukeneedham.lyrictutor.ui.HomePageContent
import com.lukeneedham.lyrictutor.ui.HomeUiState
import com.lukeneedham.lyrictutor.ui.theme.AppTheme

/** Rendered by CI to docs/screenshots/<position in order.txt>-HomePage.png. */
@Preview
@Composable
private fun HomePageScreenshot() {
    val state = HomeUiState(
        recents = listOf(
            TrackInfo(
                trackId = "6GiCszfL4D2GlCU8tFU3sR",
                title = "Si Me Voy (with The Marías)",
                searchTitle = "Si Me Voy",
                artists = listOf("Cuco", "The Marías"),
                coverUrl = null,
                durationSeconds = 203,
                releaseDate = "2023-03-10",
            ),
            TrackInfo(
                trackId = "0000000000000000000000",
                title = "Het is een nacht",
                searchTitle = "Het is een nacht",
                artists = listOf("Guus Meeuwis"),
                coverUrl = null,
                durationSeconds = 240,
                releaseDate = "2004-01-01",
            ),
        ),
        songLanguages = mapOf(
            "6GiCszfL4D2GlCU8tFU3sR" to listOf("Spanish", "English"),
            "0000000000000000000000" to listOf("Dutch"),
        ),
        selectedLanguage = "Spanish",
    )
    AppTheme {
        HomePageContent(state = state, onInputChange = {}, onSubmit = {}, onLanguageClick = {}, onOpenSettings = {}, onOpenSong = {})
    }
}

/** Rendered by CI to docs/screenshots/<position in order.txt>-HomePageEmpty.png. */
@Preview
@Composable
private fun HomePageEmptyScreenshot() {
    AppTheme {
        HomePageContent(state = HomeUiState(), onInputChange = {}, onSubmit = {}, onLanguageClick = {}, onOpenSettings = {}, onOpenSong = {})
    }
}
