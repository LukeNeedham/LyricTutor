package com.lukeneedham.lyrictutor.data.catalog

/** What a catalog knows about a song: its album art, and its page in that catalog (the starting point for finding it on other services). */
data class CatalogMatch(val coverUrl: String?, val pageUrl: String?)

/** Finds a song by its name in a music catalog. An interface, so that a provider can be swapped without touching the rest. */
interface SongCatalog {
    /**
     * The catalog's entry for the song, or null when none matches or the catalog cannot be asked. Never throws (except to cancel).
     * [durationSeconds], when known, makes sure that a cover or a remix of the song is not taken for it.
     */
    suspend fun find(title: String, artist: String, durationSeconds: Int?): CatalogMatch?
}
