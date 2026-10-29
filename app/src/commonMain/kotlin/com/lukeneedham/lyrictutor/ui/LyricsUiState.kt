package com.lukeneedham.lyrictutor.ui

import com.lukeneedham.lyrictutor.data.lyrics.Lyrics

/** The lyrics part of the song page: it loads after the song itself, so the title shows first. */
sealed interface LyricsUiState {
    data object Loading : LyricsUiState

    data class Found(val lyrics: Lyrics) : LyricsUiState

    data object NotFound : LyricsUiState

    /** The lookup could not be made (no connection, or LRCLIB answered badly). */
    data object Error : LyricsUiState
}
