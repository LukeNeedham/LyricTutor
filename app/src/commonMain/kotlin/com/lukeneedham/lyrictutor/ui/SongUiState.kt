package com.lukeneedham.lyrictutor.ui

import com.lukeneedham.lyrictutor.data.track.TrackInfo

sealed interface SongUiState {
    data object Loading : SongUiState

    data class Loaded(
        val track: TrackInfo,
        val lyrics: LyricsUiState,
        /** The text in the paste box, or null when the box is closed. */
        val pasteText: String? = null,
    ) : SongUiState

    /** [isNetwork]: no connection, as opposed to a page the app could not read. */
    data class Error(val isNetwork: Boolean) : SongUiState
}
