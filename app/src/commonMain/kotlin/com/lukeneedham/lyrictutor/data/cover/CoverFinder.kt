package com.lukeneedham.lyrictutor.data.cover

/** Finds the album art of a song by its name. An interface, so that a provider can be swapped without touching the rest. */
interface CoverFinder {
    /**
     * The link to a cover image of the song, or null when none is found or the provider cannot be asked. Never throws (except to cancel).
     * [durationSeconds], when known, makes sure that a cover or a remix of the song does not supply its art.
     */
    suspend fun find(title: String, artist: String, durationSeconds: Int?): String?
}
