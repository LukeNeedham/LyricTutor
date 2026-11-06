package com.lukeneedham.lyrictutor.data.track

/** What the app knows about a song: from its Spotify page, a YouTube video, or the name and artist the user typed (see [TrackIds]). */
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
    /** The song on Spotify, when it was found by looking the song up by name; else null (a Spotify song has it in its id). */
    val spotifyUrl: String? = null,
    /** The song on YouTube, when it was found by looking the song up by name; else null (a YouTube song has it in its id). */
    val youtubeUrl: String? = null,
)
