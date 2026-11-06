package com.lukeneedham.lyrictutor.data.catalog

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SongLinkPageParserTest {
    private val spotify = "https://open.spotify.com/track/6GiCszfL4D2GlCU8tFU3sR"
    private val youtube = "https://www.youtube.com/watch?v=dQw4w9WgXcQ"

    @Test
    fun readsTheFirstLinkOfEachKind() {
        val html = """<a href="$spotify">x</a> <a href="https://www.youtube.com/watch?v=aaaaaaaaaaa">a</a> <a href="$youtube">y</a>"""
        assertEquals(StreamingLinks(spotify, "https://www.youtube.com/watch?v=aaaaaaaaaaa"), SongLinkPageParser.parse(html))
    }

    @Test
    fun readsLinksEmbeddedAsEscapedJson() {
        val html = """{"url":"https:\/\/open.spotify.com\/track\/6GiCszfL4D2GlCU8tFU3sR"}"""
        assertEquals(StreamingLinks(spotify, null), SongLinkPageParser.parse(html))
    }

    @Test
    fun givesNothingWhenThereIsNoLink() {
        assertNull(SongLinkPageParser.parse("<html>nothing</html>"))
    }
}
