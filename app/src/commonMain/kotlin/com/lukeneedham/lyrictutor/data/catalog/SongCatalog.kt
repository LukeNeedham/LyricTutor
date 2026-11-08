package com.lukeneedham.lyrictutor.data.catalog

/** Finds the album art of a song by its name in a music catalog. An interface, so that a provider can be swapped without touching the rest. */
interface SongCatalog {
    /**
     * The link to a cover image of the song, or null when none is found or the catalog cannot be asked. Never throws (except to cancel).
     * [durationSeconds], when known, makes sure that a cover or a remix of the song is not taken for it.
     */
    suspend fun find(title: String, artist: String, durationSeconds: Int?): String?
}
