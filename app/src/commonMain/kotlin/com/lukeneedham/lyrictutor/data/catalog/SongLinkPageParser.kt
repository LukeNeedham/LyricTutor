package com.lukeneedham.lyrictutor.data.catalog

/** Reads the Spotify and YouTube links out of a song.link page. Pure, so it is tested without a network. */
object SongLinkPageParser {
    private val spotify = Regex("https://open\\.spotify\\.com/track/[A-Za-z0-9]{22}")
    private val youtube = Regex("https://(?:www\\.)?youtube\\.com/watch\\?v=[A-Za-z0-9_-]{11}")

    /** The first link of each kind in [html], or null when there is none of either. The page embeds them as JSON, where `/` may be written `\/`. */
    fun parse(html: String): StreamingLinks? {
        val text = html.replace("\\/", "/").replace("\\u002F", "/")
        val links = StreamingLinks(spotifyUrl = spotify.find(text)?.value, youtubeUrl = youtube.find(text)?.value)
        return links.takeIf { it.spotifyUrl != null || it.youtubeUrl != null }
    }
}
