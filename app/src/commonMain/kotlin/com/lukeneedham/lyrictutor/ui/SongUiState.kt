package com.lukeneedham.lyrictutor.ui

import com.lukeneedham.lyrictutor.data.track.TrackInfo

sealed interface SongUiState {
    data object Loading : SongUiState

    data class Loaded(val track: TrackInfo) : SongUiState

    /** [isNetwork]: no connection, as opposed to a page the app could not read. */
    data class Error(val isNetwork: Boolean) : SongUiState
}
