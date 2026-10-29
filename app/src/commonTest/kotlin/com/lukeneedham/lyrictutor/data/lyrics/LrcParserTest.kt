package com.lukeneedham.lyrictutor.data.lyrics

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LrcParserTest {

    @Test
    fun parsesTimedLines() {
        val lines = LrcParser.parseTimed("[00:17.12] Si me voy\n[01:02.5] y si me quedo\n[10:00.000] fin")
        assertEquals(listOf("Si me voy", "y si me quedo", "fin"), lines.map { it.text })
        assertEquals(listOf(17_120L, 62_500L, 600_000L), lines.map { it.startMs })
    }

    @Test
    fun skipsTagsAndEmptyLines() {
        val lines = LrcParser.parseTimed("[ar:Cuco]\n[ti:Si Me Voy]\n[00:01.00] \n[00:02.00] hola")
        assertEquals(listOf("hola"), lines.map { it.text })
    }

    @Test
    fun repeatsALineForEachTimestampAndSorts() {
        val lines = LrcParser.parseTimed("[00:30.00][00:10.00] estribillo\n[00:20.00] verso")
        assertEquals(listOf("estribillo", "verso", "estribillo"), lines.map { it.text })
        assertEquals(listOf(10_000L, 20_000L, 30_000L), lines.map { it.startMs })
    }

    @Test
    fun parsesPlainTextWithoutTiming() {
        val lines = LrcParser.parsePlain("  uno \n\n dos\r\ntres")
        assertEquals(listOf("uno", "dos", "tres"), lines.map { it.text })
        assertTrue(lines.all { it.startMs == null })
    }
}
