package com.lukeneedham.lyrictutor.ui

import com.lukeneedham.lyrictutor.data.wiktionary.WiktionaryResult

/** What the sheet of a word shows: what Wiktionary says of the word, looked up when the sheet opens. */
sealed interface WiktionaryUiState {
    data object Loading : WiktionaryUiState

    data class Found(val result: WiktionaryResult) : WiktionaryUiState

    /** No page was found: [searchUrl] searches Wiktionary for the word. */
    data class NotFound(val searchUrl: String) : WiktionaryUiState

    data object Error : WiktionaryUiState
}
