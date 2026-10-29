package com.lukeneedham.lyrictutor.data.track

/** Finds a Spotify track id in what the user pasted or shared. Pure, so it is tested without a network. */
object SpotifyLinkParser {
    private const val ID = "[A-Za-z0-9]{22}"
    private val trackUrl = Regex("open\\.spotify\\.com/(?:intl-[A-Za-z-]+/)?track/($ID)")
    private val trackUri = Regex("spotify:track:($ID)")
    private val bareId = Regex("^$ID$")

    /** The track id in [text] (a link, a share text that contains a link, a `spotify:track:` URI or a bare id), or null. */
    fun parseTrackId(text: String): String? {
        val trimmed = text.trim()
        trackUrl.find(trimmed)?.let { return it.groupValues[1] }
        trackUri.find(trimmed)?.let { return it.groupValues[1] }
        return if (bareId.matches(trimmed)) trimmed else null
    }
}
