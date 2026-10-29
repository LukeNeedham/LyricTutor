package com.lukeneedham.lyrictutor.data.track

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SpotifyPageParserTest {

    // The tags of the real page of "Si Me Voy", without the dozens of og:restrictions:country:allowed
    private val page = """
        <!DOCTYPE html><html><head>
        <meta charset="utf-8">
        <meta property="og:title" content="Si Me Voy (with The Marías)"/>
        <meta property="og:description" content="Cuco, The Marías · Si Me Voy (with The Marías) · Song · 2023"/>
        <meta property="og:image" content="https://i.scdn.co/image/ab67616d0000b27314a012da5638f895eaefffa5"/>
        <meta property="og:restrictions:country:allowed" content="AD"/>
        <meta property="og:restrictions:country:allowed" content="AE"/>
        <meta content="Cuco, The Marías" name="music:musician_description"/>
        <meta name="music:duration" content="203"/>
        <meta name="music:release_date" content="2023-03-10"/>
        <meta name="music:album" content="https://open.spotify.com/album/5TZm9qi0223t20ypmJevEq"/>
        </head><body></body></html>
    """.trimIndent()

    @Test
    fun readsTheTagsOfARealPage() {
        val track = SpotifyPageParser.parse("6GiCszfL4D2GlCU8tFU3sR", page).getOrThrow()
        assertEquals("Si Me Voy (with The Marías)", track.title)
        assertEquals("Si Me Voy", track.searchTitle)
        assertEquals(listOf("Cuco", "The Marías"), track.artists)
        assertEquals("https://i.scdn.co/image/ab67616d0000b27314a012da5638f895eaefffa5", track.coverUrl)
        assertEquals(203, track.durationSeconds)
        assertEquals("2023-03-10", track.releaseDate)
    }

    @Test
    fun fallsBackToTheDescription() {
        val html = """<meta property="og:description" content="Cuco, The Marías · Si Me Voy · Song · 2023"/>"""
        val track = SpotifyPageParser.parse("id", html).getOrThrow()
        assertEquals("Si Me Voy", track.title)
        assertEquals(listOf("Cuco", "The Marías"), track.artists)
        assertNull(track.coverUrl)
        assertNull(track.durationSeconds)
    }

    @Test
    fun decodesEntities() {
        val html = """
            <meta property="og:title" content="Rock &amp; Roll &#x27;n&#39; &quot;Blues&quot;"/>
            <meta name="music:musician_description" content="A"/>
        """.trimIndent()
        assertEquals("Rock & Roll 'n' \"Blues\"", SpotifyPageParser.parse("id", html).getOrThrow().title)
    }

    @Test
    fun failsWithoutAnArtist() {
        val result = SpotifyPageParser.parse("id", """<meta property="og:title" content="Song"/>""")
        assertTrue(result.isFailure)
        assertIs<TrackResolveException.Unreadable>(result.exceptionOrNull())
    }

    @Test
    fun failsOnAnEmptyPage() {
        assertTrue(SpotifyPageParser.parse("id", "<html></html>").isFailure)
    }
}
