package com.lukeneedham.lyrictutor.data.wiktionary

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class WiktionaryProtocolTest {

    // The shape of the answer of action=parse&prop=sections of the MediaWiki API
    private val answer = """
        {"parse":{"title":"voy","pageid":123,"sections":[
        {"toclevel":1,"level":"2","line":"Spanish","number":"1","index":"1","fromtitle":"voy","byteoffset":10,"anchor":"Spanish"},
        {"toclevel":2,"level":"3","line":"Etymology","number":"1.1","index":"2","fromtitle":"voy","byteoffset":20,"anchor":"Etymology"},
        {"toclevel":1,"level":"2","line":"Galician","number":"2","index":"3","fromtitle":"voy","byteoffset":30,"anchor":"Galician"}]}}
    """.trimIndent().replace("\n", "")

    @Test
    fun readsThePageAndItsSections() {
        val page = assertNotNull(WiktionaryProtocol.parsePage(answer))
        assertEquals("voy", page.title)
        assertEquals(listOf("Spanish", "Etymology", "Galician"), page.sections.map { it.line })
        assertEquals(listOf(2, 3, 2), page.sections.map { it.level })
    }

    @Test
    fun aPageThatDoesNotExistIsNull() {
        val missing = """{"error":{"code":"missingtitle","info":"The page you specified doesn't exist.","*":"..."}}"""
        assertNull(WiktionaryProtocol.parsePage(missing))
        assertNull(WiktionaryProtocol.parsePage("not json"))
    }

    @Test
    fun findsTheSectionOfALanguageOnlyAtLevelTwo() {
        val page = assertNotNull(WiktionaryProtocol.parsePage(answer))
        assertEquals("Spanish", WiktionaryProtocol.languageAnchor(page, "Spanish"))
        assertEquals("Galician", WiktionaryProtocol.languageAnchor(page, " galician "))
        assertNull(WiktionaryProtocol.languageAnchor(page, "Dutch"))
        assertNull(WiktionaryProtocol.languageAnchor(page, "Etymology"))
        assertNull(WiktionaryProtocol.languageAnchor(page, null))
    }

    @Test
    fun triesTheWordAsWrittenBeforeTheDictionaryForm() {
        assertEquals(listOf("Voy", "voy", "ir"), WiktionaryProtocol.candidates("Voy,", "ir"))
        assertEquals(listOf("voy", "ir"), WiktionaryProtocol.candidates("(voy)", "ir"))
    }

    @Test
    fun keepsAnApostropheInAnElidedWord() {
        assertEquals(listOf("pa'", "para"), WiktionaryProtocol.candidates("pa'", "para"))
        assertEquals(listOf("pa'"), WiktionaryProtocol.candidates("pa’", null))
    }

    @Test
    fun dropsEmptyAndRepeatedCandidates() {
        assertEquals(emptyList(), WiktionaryProtocol.candidates("...", null))
        assertEquals(listOf("ir"), WiktionaryProtocol.candidates("ir", "ir"))
    }

    @Test
    fun buildsLinks() {
        assertEquals("https://en.wiktionary.org/wiki/voy#Spanish", WiktionaryProtocol.pageUrl("voy", "Spanish"))
        assertEquals("https://en.wiktionary.org/wiki/voy", WiktionaryProtocol.pageUrl("voy"))
        assertEquals("https://en.wiktionary.org/wiki/Si_me_voy", WiktionaryProtocol.pageUrl("Si me voy"))
        assertEquals("https://en.wiktionary.org/w/index.php?search=voy", WiktionaryProtocol.searchUrl("voy"))
    }
}
