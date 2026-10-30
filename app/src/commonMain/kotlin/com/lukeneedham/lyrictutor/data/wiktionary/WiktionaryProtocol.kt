package com.lukeneedham.lyrictutor.data.wiktionary

import io.ktor.http.encodeURLParameter
import io.ktor.http.encodeURLPath
import kotlinx.serialization.SerializationException
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** The pieces of the Wiktionary lookup that need no network: the words to try, the reading of the answer and the links. */
object WiktionaryProtocol {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    /** A section of a Wiktionary page. On the English Wiktionary a language is a section of level 2 ("Spanish"). */
    data class Section(val level: Int, val line: String, val anchor: String)

    data class Page(val title: String, val sections: List<Section>)

    /**
     * The page titles to try, best first: the word as it is written in the lyrics (without the punctuation around it), the same in
     * lower case (a line starts with a capital), then the dictionary form the model gave.
     */
    fun candidates(surface: String, lemma: String?): List<String> =
        listOfNotNull(surface, lemma)
            .map { it.replace('’', '\'').trim { char -> !char.isLetterOrDigit() && char != '\'' }.trimStart('\'') }
            .flatMap { listOf(it, it.lowercase()) }
            .filter { it.isNotEmpty() }
            .distinct()

    /** The page in the answer of `action=parse&prop=sections`, or null when the page does not exist (or the answer is not one). */
    fun parsePage(body: String): Page? = try {
        json.decodeFromString(ParseResponse.serializer(), body).parse?.let { parsed ->
            Page(
                title = parsed.title,
                sections = parsed.sections.map { Section(level = it.level.toIntOrNull() ?: 0, line = it.line, anchor = it.anchor) },
            )
        }
    } catch (e: SerializationException) {
        null
    } catch (e: IllegalArgumentException) {
        null
    }

    /** The anchor of the section of [languageName] (for example "Spanish"), or null when the page has none for that language. */
    fun languageAnchor(page: Page, languageName: String?): String? {
        if (languageName.isNullOrBlank()) return null
        return page.sections.firstOrNull { it.level == 2 && it.line.equals(languageName.trim(), ignoreCase = true) }?.anchor
    }

    /** The link to a page, to the section of a language when [anchor] is given. */
    fun pageUrl(title: String, anchor: String? = null): String =
        "https://en.wiktionary.org/wiki/${title.replace(' ', '_').encodeURLPath()}" + if (anchor.isNullOrEmpty()) "" else "#$anchor"

    /** The link to Wiktionary's search, for a word that has no page. */
    fun searchUrl(text: String): String = "https://en.wiktionary.org/w/index.php?search=${text.encodeURLParameter()}"

    @Serializable
    private data class ParseResponse(val parse: Parsed? = null)

    @Serializable
    private data class Parsed(val title: String = "", val sections: List<SectionDto> = emptyList())

    @Serializable
    private data class SectionDto(val level: String = "", val line: String = "", val anchor: String = "")
}
