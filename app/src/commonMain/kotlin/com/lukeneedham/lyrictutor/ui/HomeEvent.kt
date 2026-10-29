package com.lukeneedham.lyrictutor.ui

/** One-off events of the home page, never part of [HomeUiState]. */
sealed interface HomeEvent {
    data class OpenSong(val trackId: String) : HomeEvent
}
