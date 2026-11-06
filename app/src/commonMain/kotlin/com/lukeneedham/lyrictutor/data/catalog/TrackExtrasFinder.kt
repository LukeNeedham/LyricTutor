package com.lukeneedham.lyrictutor.data.catalog

/** What is looked up about a song by its name: its album art, and where to play it. Each part can be missing. */
data class TrackExtras(val coverUrl: String?, val spotifyUrl: String?, val youtubeUrl: String?)

/** Finds the song in a catalog, then its links on Spotify and YouTube from the catalog's page for it. */
class TrackExtrasFinder(private val catalog: SongCatalog, private val linkFinder: StreamingLinkFinder) {
    suspend fun find(title: String, artist: String, durationSeconds: Int?): TrackExtras {
        val match = catalog.find(title, artist, durationSeconds) ?: return TrackExtras(null, null, null)
        val links = match.pageUrl?.let { linkFinder.find(it) }
        return TrackExtras(coverUrl = match.coverUrl, spotifyUrl = links?.spotifyUrl, youtubeUrl = links?.youtubeUrl)
    }
}
