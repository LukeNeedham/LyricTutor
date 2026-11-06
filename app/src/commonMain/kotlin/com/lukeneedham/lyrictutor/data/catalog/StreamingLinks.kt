package com.lukeneedham.lyrictutor.data.catalog

/** The links of one song on the services the app can play it on. Either can be missing. */
data class StreamingLinks(val spotifyUrl: String?, val youtubeUrl: String?)

/** Finds the links of a song on other services, from a link to it on one service. */
interface StreamingLinkFinder {
    /** The links, or null when the service does not know the song or cannot be asked. Never throws (except to cancel). */
    suspend fun find(pageUrl: String): StreamingLinks?
}
