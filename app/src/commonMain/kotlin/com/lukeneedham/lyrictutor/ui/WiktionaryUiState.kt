package com.lukeneedham.lyrictutor.ui

/** The "where it comes from" part of the word sheet: a link to Wiktionary, found when the sheet opens. */
sealed interface WiktionaryUiState {
    data object Loading : WiktionaryUiState

    data class Found(val title: String, val url: String) : WiktionaryUiState

    /** No page was found: [searchUrl] searches Wiktionary for the word. */
    data class NotFound(val searchUrl: String) : WiktionaryUiState

    data object Error : WiktionaryUiState
}
