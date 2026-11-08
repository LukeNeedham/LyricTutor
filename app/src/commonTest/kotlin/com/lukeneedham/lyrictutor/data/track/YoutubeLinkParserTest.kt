package com.lukeneedham.lyrictutor.data.track

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class YoutubeLinkParserTest {
    private val id = "dQw4w9WgXcQ"

    @Test
    fun parsesTheCommonLinkShapes() {
        assertEquals(id, YoutubeLinkParser.parseVideoId("https://www.youtube.com/watch?v=$id"))
        assertEquals(id, YoutubeLinkParser.parseVideoId("https://music.youtube.com/watch?v=$id&list=RDAMVM$id"))
        assertEquals(id, YoutubeLinkParser.parseVideoId("https://www.youtube.com/watch?feature=share&v=$id"))
        assertEquals(id, YoutubeLinkParser.parseVideoId("https://youtu.be/$id?si=abc"))
        assertEquals(id, YoutubeLinkParser.parseVideoId("https://www.youtube.com/shorts/$id"))
        assertEquals(id, YoutubeLinkParser.parseVideoId("Look at this https://youtu.be/$id"))
    }

    @Test
    fun ignoresOtherText() {
        assertNull(YoutubeLinkParser.parseVideoId("https://www.youtube.com/@channel"))
        assertNull(YoutubeLinkParser.parseVideoId("hello"))
    }
}
