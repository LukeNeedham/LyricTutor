package com.lukeneedham.lyrictutor.data.track

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TrackIdsTest {
    private val spotifyId = "6GiCszfL4D2GlCU8tFU3sR"

    @Test
    fun parsesASpotifyAndAYoutubeLink() {
        assertEquals(spotifyId, TrackIds.parseLink("https://open.spotify.com/track/$spotifyId"))
        assertEquals("yt:dQw4w9WgXcQ", TrackIds.parseLink("https://youtu.be/dQw4w9WgXcQ"))
        assertNull(TrackIds.parseLink("not a link"))
    }

    @Test
    fun theSameNameGivesTheSameId() {
        assertEquals(TrackIds.byName("Si Me Voy", "Cuco"), TrackIds.byName("  si  me voy ", "CUCO"))
    }

    @Test
    fun sourcesAreTold() {
        assertTrue(TrackIds.isSpotify(spotifyId))
        assertTrue(TrackIds.isYoutube("yt:dQw4w9WgXcQ"))
        assertTrue(TrackIds.isByName(TrackIds.byName("a", "b")))
        assertFalse(TrackIds.isSpotify("yt:dQw4w9WgXcQ"))
    }

    @Test
    fun playUrls() {
        assertEquals("https://open.spotify.com/track/$spotifyId", TrackIds.playUrl(spotifyId))
        assertEquals("https://www.youtube.com/watch?v=dQw4w9WgXcQ", TrackIds.playUrl("yt:dQw4w9WgXcQ"))
        assertNull(TrackIds.playUrl(TrackIds.byName("a", "b")))
    }
}
