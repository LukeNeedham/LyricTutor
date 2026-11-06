package com.lukeneedham.lyrictutor.data.track

import kotlin.test.Test
import kotlin.test.assertEquals

class YoutubeTitleParserTest {
    @Test
    fun splitsArtistAndSongAndDropsNoise() {
        assertEquals("Song" to "Artist", YoutubeTitleParser.parse("Artist - Song (Official Video)", "ArtistVEVO"))
        assertEquals("Song" to "Artist", YoutubeTitleParser.parse("Artist - Song [Lyrics]", "Some channel"))
        assertEquals("Song" to "Artist", YoutubeTitleParser.parse("Artist - Song (Video Oficial)", null))
    }

    @Test
    fun usesTheChannelWhenTheTitleHasNoArtist() {
        assertEquals("Song" to "Artist", YoutubeTitleParser.parse("Song", "Artist - Topic"))
    }

    @Test
    fun keepsBracketsThatAreNotNoise() {
        assertEquals("Song (Remix)" to "Artist", YoutubeTitleParser.parse("Artist - Song (Remix)", null))
    }

    @Test
    fun anEmptyTitleIsNothing() {
        assertEquals(null, YoutubeTitleParser.parse("  ", "Artist"))
    }
}
