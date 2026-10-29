package com.lukeneedham.lyrictutor.data.track

/** What the app knows about a song after reading its Spotify page. */
data class TrackInfo(
    val trackId: String,
    /** The title as Spotify shows it, for display. */
    val title: String,
    /** [title] without `(with …)`, `(feat. …)` and remaster suffixes, for the lyrics lookup. */
    val searchTitle: String,
    /** The first is the main artist. */
    val artists: List<String>,
    val coverUrl: String?,
    val durationSeconds: Int?,
    val releaseDate: String?,
)
