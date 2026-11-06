package com.lukeneedham.lyrictutor.data.track

import kotlin.test.Test
import kotlin.test.assertEquals

class TrackLinksTest {
    private val spotifyId = "6GiCszfL4D2GlCU8tFU3sR"

    private fun track(id: String, spotifyUrl: String? = null, youtubeUrl: String? = null) = TrackInfo(
        trackId = id,
        title = "Si Me Voy",
        searchTitle = "Si Me Voy",
        artists = listOf("Cuco", "The Marías"),
        coverUrl = null,
        durationSeconds = null,
        releaseDate = null,
        spotifyUrl = spotifyUrl,
        youtubeUrl = youtubeUrl,
    )

    @Test
    fun aSpotifySongPlaysOnSpotifyFromItsId() {
        assertEquals("https://open.spotify.com/track/$spotifyId", TrackLinks.playUrl(track(spotifyId), PlayProvider.Spotify))
    }

    @Test
    fun aYoutubeSongPlaysOnYoutubeFromItsId() {
        assertEquals("https://www.youtube.com/watch?v=dQw4w9WgXcQ", TrackLinks.playUrl(track("yt:dQw4w9WgXcQ"), PlayProvider.YouTube))
    }

    @Test
    fun linksFoundForASongTypedInAreUsed() {
        val found = track(TrackIds.byName("a", "b"), spotifyUrl = "https://open.spotify.com/track/x", youtubeUrl = "https://www.youtube.com/watch?v=y")
        assertEquals("https://open.spotify.com/track/x", TrackLinks.playUrl(found, PlayProvider.Spotify))
        assertEquals("https://www.youtube.com/watch?v=y", TrackLinks.playUrl(found, PlayProvider.YouTube))
    }

    @Test
    fun withoutALinkItIsASearchOnTheService() {
        val typed = track(TrackIds.byName("a", "b"))
        assertEquals("https://open.spotify.com/search/Si%20Me%20Voy%20Cuco", TrackLinks.playUrl(typed, PlayProvider.Spotify))
        assertEquals("https://www.youtube.com/results?search_query=Si%20Me%20Voy%20Cuco", TrackLinks.playUrl(typed, PlayProvider.YouTube))
        // A Spotify song has no YouTube link of its own
        assertEquals("https://www.youtube.com/results?search_query=Si%20Me%20Voy%20Cuco", TrackLinks.playUrl(track(spotifyId), PlayProvider.YouTube))
    }
}
