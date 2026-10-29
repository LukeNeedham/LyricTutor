package com.lukeneedham.lyrictutor.data.track

import kotlin.test.Test
import kotlin.test.assertEquals

class TitleCleanerTest {
    @Test
    fun stripsWithAndFeaturing() {
        assertEquals("Si Me Voy", TitleCleaner.clean("Si Me Voy (with The Marías)"))
        assertEquals("Song", TitleCleaner.clean("Song (feat. Someone)"))
        assertEquals("Song", TitleCleaner.clean("Song [ft. Someone]"))
    }

    @Test
    fun stripsRemasterSuffixes() {
        assertEquals("Song", TitleCleaner.clean("Song - Remastered 2011"))
        assertEquals("Song", TitleCleaner.clean("Song - 2009 Remaster"))
        assertEquals("Song", TitleCleaner.clean("Song (Remastered 2011)"))
    }

    @Test
    fun stripsSeveralSuffixes() {
        assertEquals("Song", TitleCleaner.clean("Song (feat. A) - Remastered 2011"))
    }

    @Test
    fun keepsPlainTitlesAndInnerParentheses() {
        assertEquals("Song", TitleCleaner.clean("Song"))
        assertEquals("(I Can't Get No) Satisfaction", TitleCleaner.clean("(I Can't Get No) Satisfaction"))
        assertEquals("Dance with Me", TitleCleaner.clean("Dance with Me"))
    }
}
