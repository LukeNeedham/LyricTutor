package com.lukeneedham.lyrictutor.data.annotation

import com.lukeneedham.lyrictutor.data.lyrics.LyricLine
import kotlin.test.Test
import kotlin.test.assertEquals

class SongLanguagesTest {

    private fun line(text: String) = LyricLine(text)

    @Test
    fun theLanguageWithTheMostWordsComesFirst() {
        val lines = listOf(line("uno dos tres"), line("one two"), line("cuatro cinco"))
        val annotations = mapOf(
            0 to LineAnnotation(index = 0, language = "Spanish"),
            1 to LineAnnotation(index = 1, language = "English"),
            2 to LineAnnotation(index = 2, language = "Spanish"),
        )
        assertEquals(listOf("Spanish", "English"), SongLanguages.ranked(lines, annotations))
    }

    @Test
    fun aWordOfAnotherLanguageCountsForThatLanguage() {
        val lines = listOf(line("quiero baby baby baby"))
        val annotations = mapOf(
            0 to LineAnnotation(index = 0, language = "Spanish", otherLanguages = listOf(WordLanguage("baby", "English"))),
        )
        assertEquals(listOf("English", "Spanish"), SongLanguages.ranked(lines, annotations))
    }

    @Test
    fun linesWithoutALanguageDoNotCount() {
        val lines = listOf(line("uno dos"), line("tres"))
        val annotations = mapOf(0 to LineAnnotation(index = 0, language = "Spanish"))
        assertEquals(listOf("Spanish"), SongLanguages.ranked(lines, annotations))
        assertEquals(emptyList(), SongLanguages.ranked(lines, emptyMap()))
    }
}
