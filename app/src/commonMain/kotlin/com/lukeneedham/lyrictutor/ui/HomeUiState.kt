package com.lukeneedham.lyrictutor.ui

data class HomeUiState(
    val input: String = "",
    /** Set when the last attempt found no Spotify track in [input]. */
    val showInvalidLink: Boolean = false,
)
