package com.lukeneedham.lyrictutor.screenshot

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.lukeneedham.lyrictutor.ui.HomePageContent
import com.lukeneedham.lyrictutor.ui.HomeUiState
import com.lukeneedham.lyrictutor.ui.theme.AppTheme

/** Rendered by CI to docs/screenshots/<position in order.txt>-HomePage.png. */
@Preview
@Composable
private fun HomePageScreenshot() {
    val state = HomeUiState(input = "https://open.spotify.com/track/6GiCszfL4D2GlCU8tFU3sR")
    AppTheme { HomePageContent(state = state, onInputChange = {}, onSubmit = {}, onOpenSettings = {}) }
}
