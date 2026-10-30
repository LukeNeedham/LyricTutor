package com.lukeneedham.lyrictutor.data.wiktionary

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class WiktionaryProtocolTest {

    // Shortened answers of the REST page/definition endpoint, as it answers for "voy", "gewoon" and "pa'"
    private val voy = """
        {"other":[{"partOfSpeech":"Verb","language":"Middle French","definitions":[{"definition":"<span class=\"form-of-definition use-with-mention\"><a href=\"/wiki/Appendix:Glossary#first_person\" title=\"Appendix:Glossary\">first-person</a> <a href=\"/wiki/Appendix:Glossary#singular_number\" title=\"Appendix:Glossary\">singular</a> <a href=\"/wiki/Appendix:Glossary#present_tense\" title=\"Appendix:Glossary\">present</a> <a href=\"/wiki/Appendix:Glossary#indicative_mood\" title=\"Appendix:Glossary\">indicative</a> of <span class=\"form-of-definition-link\"><i class=\"Latn mention\" lang=\"frm\"><a href=\"/wiki/veoir#Middle_French\" title=\"veoir\">veoir</a></i></span></span>"}]}],
        "es":[{"partOfSpeech":"Verb","language":"Spanish","definitions":[{"definition":"<span class=\"form-of-definition use-with-mention\"><a href=\"/wiki/Appendix:Glossary#first_person\" title=\"Appendix:Glossary\">first-person</a> <a href=\"/wiki/Appendix:Glossary#singular_number\" title=\"Appendix:Glossary\">singular</a> <a href=\"/wiki/Appendix:Glossary#present_tense\" title=\"Appendix:Glossary\">present</a> <a href=\"/wiki/Appendix:Glossary#indicative_mood\" title=\"Appendix:Glossary\">indicative</a> of <span class=\"form-of-definition-link\"><i class=\"Latn mention\" lang=\"es\"><a href=\"/wiki/ir#Spanish\" title=\"ir\">ir</a></i></span></span>"}]}]}
    """.trimIndent().replace("\n", "")

    private val gewoon = """
        {"af":[{"partOfSpeech":"Adjective","language":"Afrikaans","definitions":[{"definition":"<a href=\"/wiki/usual\" title=\"usual\">usual</a>, <a href=\"/wiki/ordinary\" title=\"ordinary\">ordinary</a>"}]}],
        "nl":[{"partOfSpeech":"Adjective","language":"Dutch","definitions":[
        {"definition":"<a href=\"/wiki/regular\" title=\"regular\">regular</a>, <a href=\"/wiki/ordinary\" title=\"ordinary\">ordinary</a>, <a href=\"/wiki/usual\" title=\"usual\">usual</a>","examples":["Dit is maar een <b>gewone</b> postzegel."]},
        {"definition":"<a href=\"/wiki/normal\" title=\"normal\">normal</a>, <a href=\"/wiki/common\" title=\"common\">common</a>"},
        {"definition":"<a href=\"/wiki/wont\" title=\"wont\">wont</a>, having a certain <a href=\"/wiki/habit\" title=\"habit\">habit</a> <style data-mw-deduplicate=\"TemplateStyles:r90144471\">.mw-parser-output .object-usage-tag{font-style:italic}</style><span>[</span><i>with</i><span> </span><b class=\"Latn\" lang=\"nl\"><a href=\"/wiki/om#Dutch\" title=\"om\">om</a></b><span> </span><small>‘to’</small><span>]</span>"}]},
        {"partOfSpeech":"Adverb","language":"Dutch","definitions":[{"definition":"<a href=\"/wiki/simply\" title=\"simply\">simply</a>, <a href=\"/wiki/just\" title=\"just\">just</a>"}]}]}
    """.trimIndent().replace("\n", "")

    private val pa = """
        {"other":[{"partOfSpeech":"Preposition","language":"Sassarese","definitions":[{"definition":"<span class=\"Latn\" lang=\"en\"><a href=\"/wiki/for#English\" title=\"for\">for</a></span>"}]}],
        "es":[{"partOfSpeech":"Preposition","language":"Spanish","definitions":[{"definition":"<span class=\"usage-label-sense\"></span> <span class=\"form-of-definition use-with-mention\"><a href=\"/wiki/Appendix:Glossary#apocopic\" title=\"Appendix:Glossary\">apocopic</a> form of <span class=\"form-of-definition-link\"><i class=\"Latn mention\" lang=\"es\"><a href=\"/wiki/para#Spanish\" title=\"para\">para</a></i></span></span><link rel=\"mw:PageProp/Category\" href=\"./Category:Spanish_apocopic_forms#PA'\">"}]}]}
    """.trimIndent().replace("\n", "")

    @Test
    fun readsTheEntriesOfEveryLanguage() {
        assertEquals(listOf("Middle French", "Spanish"), WiktionaryProtocol.parseEntries(voy).map { it.language })
        assertEquals(listOf("Afrikaans", "Dutch", "Dutch"), WiktionaryProtocol.parseEntries(gewoon).map { it.language })
        assertEquals(emptyList(), WiktionaryProtocol.parseEntries("not json"))
        assertEquals(emptyList(), WiktionaryProtocol.parseEntries("""{"type":"not_found"}"""))
    }

    @Test
    fun picksTheEntriesOfTheLanguageOfTheWord() {
        val entries = WiktionaryProtocol.parseEntries(voy)
        assertEquals(listOf("Spanish"), WiktionaryProtocol.entriesFor(entries, "Spanish").map { it.language })
        assertEquals(listOf("Spanish"), WiktionaryProtocol.entriesFor(entries, " spanish ").map { it.language })
        // A language the page does not have, and several that it does: nothing to choose
        assertEquals(emptyList(), WiktionaryProtocol.entriesFor(entries, "Dutch"))
        assertEquals(emptyList(), WiktionaryProtocol.entriesFor(entries, null))
    }

    @Test
    fun aPageWithOneLanguageIsReadWhateverTheLanguageGiven() {
        val entries = WiktionaryProtocol.parseEntries("""{"es":[{"partOfSpeech":"Noun","language":"Spanish","definitions":[{"definition":"x"}]}]}""")
        assertEquals(1, WiktionaryProtocol.entriesFor(entries, "Dutch").size)
        assertEquals(1, WiktionaryProtocol.entriesFor(entries, null).size)
    }

    @Test
    fun readsTheFormAndTheDictionaryFormOfAnInflectedWord() {
        val spanish = WiktionaryProtocol.entriesFor(WiktionaryProtocol.parseEntries(voy), "Spanish")
        val sense = WiktionaryProtocol.senses(spanish).single()
        assertEquals("Verb", sense.partOfSpeech)
        assertEquals("first-person singular present indicative of ir", sense.formOf)
        assertEquals("ir", sense.lemma)
        assertTrue(sense.formDetails.isEmpty())
        assertTrue(sense.glosses.isEmpty())
    }

    @Test
    fun readsAnElidedWordAsAFormOfTheFullWord() {
        val spanish = WiktionaryProtocol.entriesFor(WiktionaryProtocol.parseEntries(pa), "Spanish")
        val sense = WiktionaryProtocol.senses(spanish).single()
        assertEquals("apocopic form of para", sense.formOf)
        assertEquals("para", sense.lemma)
    }

    @Test
    fun readsTheMeaningsOfADictionaryForm() {
        val dutch = WiktionaryProtocol.entriesFor(WiktionaryProtocol.parseEntries(gewoon), "Dutch")
        val senses = WiktionaryProtocol.senses(dutch)
        assertEquals(listOf("Adjective", "Adverb"), senses.map { it.partOfSpeech })
        assertNull(senses[0].lemma)
        assertNull(senses[0].formOf)
        assertEquals(
            listOf("regular, ordinary, usual", "normal, common", "wont, having a certain habit [with om ‘to’]"),
            senses[0].glosses,
        )
        assertEquals(listOf("simply, just"), senses[1].glosses)
    }

    // The Dutch entries of "jou" as the endpoint answers: a pronoun with a meaning and a misspelling, and a verb that is only a form
    private val jou = """
        {"nl":[{"partOfSpeech":"Pronoun","language":"Dutch","definitions":[
        {"definition":"<a href=\"/wiki/you\" title=\"you\">you</a>; <span class=\"use-with-mention\">second-person singular objective personal pronoun</span>","examples":["Ik zal dit wel even doen voor <b>jou</b>."]},
        {"definition":"<span class=\"form-of-definition use-with-mention\">misspelling of <span class=\"form-of-definition-link\"><i class=\"Latn mention\" lang=\"nl\"><a href=\"/wiki/jouw#Dutch\" title=\"jouw\">jouw</a></i> <span class=\"mention-gloss-paren annotation-paren\">(</span><span class=\"mention-gloss-double-quote\">“</span><span class=\"mention-gloss\">your</span><span class=\"mention-gloss-double-quote\">”</span><span class=\"mention-gloss-paren annotation-paren\">)</span></span></span><link rel=\"mw:PageProp/Category\" href=\"./Category:Dutch_misspellings#JOU\">"}]},
        {"partOfSpeech":"Verb","language":"Dutch","definitions":[
        {"definition":"<span class=\"form-of-definition use-with-mention\">inflection of <span class=\"form-of-definition-link\"><i class=\"Latn mention\" lang=\"nl\"><a href=\"/wiki/jouen#Dutch\" title=\"jouen\">jouen</a></i></span>:</span><span>\n</span><ol><li><span class=\"form-of-definition use-with-mention\"><a href=\"/wiki/Appendix:Glossary#first_person\" title=\"Appendix:Glossary\">first-person</a> <a href=\"/wiki/Appendix:Glossary#singular_number\" title=\"Appendix:Glossary\">singular</a> <a href=\"/wiki/Appendix:Glossary#present_tense\" title=\"Appendix:Glossary\">present</a> <a href=\"/wiki/Appendix:Glossary#indicative_mood\" title=\"Appendix:Glossary\">indicative</a></span></li><li><span class=\"form-of-definition use-with-mention\"><a href=\"/wiki/Appendix:Glossary#imperative_mood\" title=\"Appendix:Glossary\">imperative</a></span></li></ol>"},
        {"definition":"<span class=\"form-of-definition use-with-mention\"><a href=\"/wiki/Appendix:Glossary#first_person\" title=\"Appendix:Glossary\">first-person</a> <a href=\"/wiki/Appendix:Glossary#singular_number\" title=\"Appendix:Glossary\">singular</a> <a href=\"/wiki/Appendix:Glossary#present_tense\" title=\"Appendix:Glossary\">present</a> <a href=\"/wiki/Appendix:Glossary#indicative_mood\" title=\"Appendix:Glossary\">indicative</a></span>"},
        {"definition":"<span class=\"form-of-definition use-with-mention\">(in case of <a href=\"/wiki/Appendix:Glossary#inversion\" title=\"Appendix:Glossary\">inversion</a>) <a href=\"/wiki/Appendix:Glossary#second_person\" title=\"Appendix:Glossary\">second-person</a> <a href=\"/wiki/Appendix:Glossary#singular_number\" title=\"Appendix:Glossary\">singular</a> <a href=\"/wiki/Appendix:Glossary#present_tense\" title=\"Appendix:Glossary\">present</a> <a href=\"/wiki/Appendix:Glossary#indicative_mood\" title=\"Appendix:Glossary\">indicative</a></span>"},
        {"definition":"<span class=\"form-of-definition use-with-mention\"><a href=\"/wiki/Appendix:Glossary#imperative_mood\" title=\"Appendix:Glossary\">imperative</a></span>"}]}]}
    """.trimIndent().replace("\n", "")

    @Test
    fun aMisspellingIsLeftOutOfAnEntryThatHasAMeaning() {
        val pronoun = WiktionaryProtocol.senses(WiktionaryProtocol.parseEntries(jou)).first { it.partOfSpeech == "Pronoun" }
        assertNull(pronoun.formOf)
        assertEquals(listOf("you; second-person singular objective personal pronoun"), pronoun.glosses)
    }

    private fun entry(partOfSpeech: String, vararg definitions: String) =
        WiktionaryEntryDto(partOfSpeech, "Dutch", definitions.map { WiktionaryDefinitionDto(it) })

    private val misspellingOf = """<span class="form-of-definition use-with-mention">misspelling of <span class="form-of-definition-link"><i><a href="/wiki/jouw#Dutch" title="jouw">jouw</a></i></span></span>"""

    @Test
    fun anEntryThatIsOnlyAMisspellingIsLeftOutWhenThePageHasSomethingElse() {
        val senses = WiktionaryProtocol.senses(listOf(entry("Noun", misspellingOf), entry("Verb", "to <b>go</b>")))
        assertEquals(listOf("Verb"), senses.map { it.partOfSpeech })
    }

    @Test
    fun aMisspellingIsShownWhenItIsAllThePageHas() {
        val sense = WiktionaryProtocol.senses(listOf(entry("Noun", misspellingOf))).single()
        assertEquals("misspelling of jouw", sense.formOf)
        assertEquals("jouw", sense.lemma)
    }

    @Test
    fun aMisspellingMeaningIsShownWhenItIsTheOnlyMeaning() {
        val sense = WiktionaryProtocol.senses(listOf(entry("Noun", "misspelling of <i>x</i>"))).single()
        assertEquals(listOf("misspelling of x"), sense.glosses)
    }

    @Test
    fun aWordThatIsOnlyAnInflectionGivesItsFormsUnderTheHeadline() {
        val verb = WiktionaryProtocol.senses(WiktionaryProtocol.parseEntries(jou)).first { it.partOfSpeech == "Verb" }
        assertEquals("inflection of jouen", verb.formOf)
        assertEquals("jouen", verb.lemma)
        assertEquals(
            listOf(
                "first-person singular present indicative",
                "(in case of inversion) second-person singular present indicative",
                "imperative",
            ),
            verb.formDetails,
        )
        assertTrue(verb.glosses.isEmpty())
    }

    @Test
    fun theMeaningsOfADictionaryFormLeaveOutItsFormsOfOtherWords() {
        val entry = WiktionaryProtocol.parseEntries(jou).first { it.partOfSpeech == "Pronoun" }
        assertEquals(listOf("you; second-person singular objective personal pronoun"), WiktionaryProtocol.glosses(entry))
    }

    @Test
    fun takesHtmlToPlainText() {
        assertEquals("a & b < c", WiktionaryProtocol.toPlainText("<b>a</b>  &amp; b &lt; <style>.x{}</style>c"))
        assertEquals("it's", WiktionaryProtocol.toPlainText("it&#39;s"))
        assertEquals("it's", WiktionaryProtocol.toPlainText("it&#x27;s"))
    }

    @Test
    fun triesTheWordAsWrittenThenInLowerCase() {
        assertEquals(listOf("Voy", "voy"), WiktionaryProtocol.candidates("Voy,"))
        assertEquals(listOf("voy"), WiktionaryProtocol.candidates("(voy)"))
        assertEquals(listOf("pa'"), WiktionaryProtocol.candidates("pa’"))
        assertEquals(emptyList(), WiktionaryProtocol.candidates("..."))
    }

    @Test
    fun buildsLinks() {
        assertEquals("https://en.wiktionary.org/wiki/voy#Spanish", WiktionaryProtocol.pageUrl("voy", "Spanish"))
        assertEquals("https://en.wiktionary.org/wiki/voy", WiktionaryProtocol.pageUrl("voy"))
        assertEquals("https://en.wiktionary.org/wiki/dos_mil#Middle_French", WiktionaryProtocol.pageUrl("dos mil", "Middle French"))
        assertEquals("https://en.wiktionary.org/w/index.php?search=voy", WiktionaryProtocol.searchUrl("voy"))
    }

    @Test
    fun anEntryWithNothingInItIsReadWithoutCrashing() {
        val entries = WiktionaryProtocol.parseEntries("""{"es":[{}]}""")
        assertEquals(1, entries.size)
        assertEquals(emptyList(), WiktionaryProtocol.senses(entries).flatMap { it.glosses })
        assertEquals(emptyList(), WiktionaryProtocol.entriesFor(entries, "Dutch"))
    }
}
