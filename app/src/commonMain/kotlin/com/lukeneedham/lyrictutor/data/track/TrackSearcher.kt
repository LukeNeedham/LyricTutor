package com.lukeneedham.lyrictutor.data.track

/** Finds a song from the name and artist the user typed, which may be spelled loosely: the song found has the canonical name and artist. */
interface TrackSearcher {
    /** Success with null when no song matches; failure when the source could not be asked. */
    suspend fun findTrack(title: String, artist: String): Result<TrackInfo?>
}
