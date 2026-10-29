package com.lukeneedham.lyrictutor.data.lyrics

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class LrclibTrackTest {

    // The shape LRCLIB documents; unknown fields (id, name) must be ignored
    private val one = """
        {"id":1,"name":"Song","trackName":"Song","artistName":"Artist","albumName":"Album","duration":203.0,
         "instrumental":false,"plainLyrics":"uno\ndos","syncedLyrics":"[00:01.00] uno\n[00:02.00] dos"}
    """.trimIndent()

    @Test
    fun prefersSyncedLyrics() {
        val lyrics = LrclibTrack.parseOne(one).toLyrics()!!
        assertEquals(listOf(1_000L, 2_000L), lyrics.lines.map { it.startMs })
    }

    @Test
    fun fallsBackToPlainLyrics() {
        val lyrics = LrclibTrack.parseOne("""{"plainLyrics":"uno\ndos","syncedLyrics":null}""").toLyrics()!!
        assertEquals(listOf("uno", "dos"), lyrics.lines.map { it.text })
        assertNull(lyrics.lines.first().startMs)
    }

    @Test
    fun instrumentalAndEmptyHaveNoLyrics() {
        assertNull(LrclibTrack.parseOne("""{"instrumental":true,"plainLyrics":"la la"}""").toLyrics())
        assertNull(LrclibTrack.parseOne("""{"plainLyrics":"","syncedLyrics":""}""").toLyrics())
    }

    @Test
    fun picksTheClosestDuration() {
        val results = LrclibTrack.parseList(
            """[{"duration":190.0,"plainLyrics":"a"},{"duration":204.0,"plainLyrics":"b"},{"duration":240.0,"plainLyrics":"c"}]""",
        )
        assertEquals("b", LrclibTrack.pickBest(results, 203)!!.plainLyrics)
    }

    @Test
    fun rejectsResultsFarFromTheDuration() {
        val results = LrclibTrack.parseList("""[{"duration":300.0,"plainLyrics":"a"}]""")
        assertNull(LrclibTrack.pickBest(results, 203))
    }

    @Test
    fun skipsResultsWithoutLyrics() {
        val results = LrclibTrack.parseList(
            """[{"duration":203.0,"instrumental":true},{"duration":206.0,"plainLyrics":"b"}]""",
        )
        assertEquals("b", LrclibTrack.pickBest(results, 203)!!.plainLyrics)
    }

    @Test
    fun takesTheFirstWhenTheDurationIsUnknown() {
        val results = LrclibTrack.parseList("""[{"plainLyrics":"a"},{"plainLyrics":"b"}]""")
        assertEquals("a", LrclibTrack.pickBest(results, null)!!.plainLyrics)
    }
}
