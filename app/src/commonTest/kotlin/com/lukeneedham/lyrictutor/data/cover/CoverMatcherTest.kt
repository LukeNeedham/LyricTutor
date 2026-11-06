package com.lukeneedham.lyrictutor.data.cover

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CoverMatcherTest {
    @Test
    fun matchesTheSameSongSpelledLoosely() {
        assertTrue(CoverMatcher.isMatch("Si Me Voy", "Cuco", 203, "Si Me Voy (feat. The Marías)", "Cuco & The Marías", 204))
        assertTrue(CoverMatcher.isMatch("Si Me Voy", "Cuco, The Marías", null, "si me voy", "CUCO", null))
    }

    @Test
    fun rejectsAnotherSongOrArtist() {
        assertFalse(CoverMatcher.isMatch("Si Me Voy", "Cuco", null, "Otra Cosa", "Cuco", null))
        assertFalse(CoverMatcher.isMatch("Si Me Voy", "Cuco", null, "Si Me Voy", "Someone Else", null))
    }

    @Test
    fun rejectsAnotherRecordingByDuration() {
        assertFalse(CoverMatcher.isMatch("Song", "Artist", 200, "Song", "Artist", 260))
        assertTrue(CoverMatcher.isMatch("Song", "Artist", 200, "Song", "Artist", 205))
    }

    @Test
    fun nothingMatchesAMissingName() {
        assertFalse(CoverMatcher.isMatch("Song", "Artist", null, null, "Artist", null))
    }

    @Test
    fun itunesArtworkIsAskedAtALargerSize() {
        assertEquals(
            "https://is1-ssl.mzstatic.com/image/thumb/Music/abc/cover.jpg/600x600bb.jpg",
            ItunesCoverFinder.larger("https://is1-ssl.mzstatic.com/image/thumb/Music/abc/cover.jpg/100x100bb.jpg"),
        )
    }
}
