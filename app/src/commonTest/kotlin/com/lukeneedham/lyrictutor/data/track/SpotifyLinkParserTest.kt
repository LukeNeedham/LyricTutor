package com.lukeneedham.lyrictutor.data.track

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SpotifyLinkParserTest {
    private val id = "6GiCszfL4D2GlCU8tFU3sR"

    @Test
    fun parsesAPlainLink() {
        assertEquals(id, SpotifyLinkParser.parseTrackId("https://open.spotify.com/track/$id"))
    }

    @Test
    fun parsesALinkWithTrackingAndLocale() {
        assertEquals(id, SpotifyLinkParser.parseTrackId("https://open.spotify.com/intl-es/track/$id?si=abc123"))
    }

    @Test
    fun parsesShareTextWithALinkInside() {
        assertEquals(id, SpotifyLinkParser.parseTrackId("Si Me Voy\nhttps://open.spotify.com/track/$id?si=x\n"))
    }

    @Test
    fun parsesAUriAndABareId() {
        assertEquals(id, SpotifyLinkParser.parseTrackId("spotify:track:$id"))
        assertEquals(id, SpotifyLinkParser.parseTrackId("  $id "))
    }

    @Test
    fun buildsTheLinkThatOpensATrack() {
        assertEquals("https://open.spotify.com/track/$id", SpotifyLinkParser.trackUrl(id))
        // The link it builds is one it reads back
        assertEquals(id, SpotifyLinkParser.parseTrackId(SpotifyLinkParser.trackUrl(id)))
    }

    @Test
    fun rejectsOtherText() {
        assertNull(SpotifyLinkParser.parseTrackId(""))
        assertNull(SpotifyLinkParser.parseTrackId("hello"))
        assertNull(SpotifyLinkParser.parseTrackId("https://open.spotify.com/album/5TZm9qi0223t20ypmJevEq"))
    }
}
