package com.lukeneedham.lyrictutor.ui

import com.lukeneedham.lyrictutor.data.track.TrackInfo

data class HomeUiState(
    val input: String = "",
    /** Set when the last attempt found no Spotify track in [input]. */
    val showInvalidLink: Boolean = false,
    /** The songs opened before, most recent first. */
    val recents: List<TrackInfo> = emptyList(),
)
