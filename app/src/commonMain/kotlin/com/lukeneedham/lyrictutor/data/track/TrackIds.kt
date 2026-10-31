package com.lukeneedham.lyrictutor.data.track

/**
 * A song is identified by one string, which says where it came from: a bare Spotify id, `yt:<video id>` for YouTube, or
 * `name:<title>|<artist>` for a song the user typed in. Pure, so it is tested without a network.
 */
object TrackIds {
    private const val YOUTUBE_PREFIX = "yt:"
    private const val NAME_PREFIX = "name:"

    fun youtube(videoId: String) = YOUTUBE_PREFIX + videoId

    /** The same title and artist (any case, any spacing) always give the same id, so the song is found again. */
    fun byName(title: String, artist: String) = NAME_PREFIX + normalize(title) + "|" + normalize(artist)

    fun isYoutube(trackId: String) = trackId.startsWith(YOUTUBE_PREFIX)

    fun isByName(trackId: String) = trackId.startsWith(NAME_PREFIX)

    fun isSpotify(trackId: String) = !isYoutube(trackId) && !isByName(trackId)

    /** The song in what the user pasted or shared (a Spotify or a YouTube link), as a track id, or null. */
    fun parseLink(text: String): String? =
        SpotifyLinkParser.parseTrackId(text) ?: YoutubeLinkParser.parseVideoId(text)?.let(::youtube)

    /** The link that plays the song in its own app, or null for a song that has no place to play (one typed in by name). */
    fun playUrl(trackId: String): String? = when {
        isYoutube(trackId) -> YoutubeLinkParser.videoUrl(trackId.removePrefix(YOUTUBE_PREFIX))
        isByName(trackId) -> null
        else -> SpotifyLinkParser.trackUrl(trackId)
    }

    private fun normalize(text: String) = text.trim().lowercase().replace(Regex("\\s+"), " ")
}
