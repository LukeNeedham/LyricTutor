package com.lukeneedham.lyrictutor.data.wiktionary

import com.lukeneedham.lyrictutor.data.apiJson
import io.ktor.http.encodeURLParameter
import io.ktor.http.encodeURLPath
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer

/**
 * The pieces of the Wiktionary lookup that need no network: reading the answer of the REST `page/definition/<word>` endpoint, and the
 * words and links. The answer is an object by language code ("es", "nl", "other") of entries, one for each part of speech.
 */
object WiktionaryProtocol {
    private val json = apiJson

    private const val MAX_GLOSSES = 4
    private const val MAX_SENSES = 3
    private const val FORM_OF_MARKER = "form-of-definition-link"
    private const val FORM_OF_CLASS = "form-of-definition"
    private val styleOrScript = Regex("<(style|script)[^>]*>.*?</\\1>", setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE))
    private val tag = Regex("<[^>]*>")
    private val spaces = Regex("\\s+")
    private val numericEntity = Regex("&#(x?)([0-9a-fA-F]+);")
    private val firstLinkTitle = Regex("title=\"([^\"]+)\"")

    /** The entries of every language on the page, in the order of the answer; empty when the answer is not one. */
    fun parseEntries(body: String): List<WiktionaryEntryDto> = try {
        json.decodeFromString(MapSerializer(String.serializer(), ListSerializer(WiktionaryEntryDto.serializer())), body).values.flatten()
    } catch (e: SerializationException) {
        emptyList()
    } catch (e: IllegalArgumentException) {
        emptyList()
    }

    /**
     * The entries to read for [language] (its English name): those of that language; when the page has none of it but only one
     * language, that language's. Empty when there are none to choose.
     */
    fun entriesFor(entries: List<WiktionaryEntryDto>, language: String?): List<WiktionaryEntryDto> {
        if (!language.isNullOrBlank()) {
            entries.filter { it.language.equals(language.trim(), ignoreCase = true) }.takeIf { it.isNotEmpty() }?.let { return it }
        }
        val languages = entries.map { it.language }.distinct()
        return if (languages.size == 1) entries else emptyList()
    }

    /** The pages to try, best first: the word as it is written in the lyrics (without the punctuation around it), then in lower case. */
    fun candidates(word: String): List<String> =
        word.replace('’', '\'')
            .trim { !it.isLetterOrDigit() && it != '\'' }
            .trimStart('\'')
            .let { listOf(it, it.lowercase()) }
            .filter { it.isNotEmpty() }
            .distinct()

    /** What a definition of an entry is: a meaning, a "form of" another word, or a line that details a "form of" above it. */
    private sealed interface Parsed {
        val text: String

        data class Meaning(override val text: String) : Parsed

        data class FormOf(override val text: String, val lemma: String) : Parsed

        data class Detail(override val text: String) : Parsed
    }

    private fun parse(html: String): Parsed? {
        val marker = html.indexOf("class=\"$FORM_OF_MARKER\"")
        if (marker >= 0) {
            val lemma = firstLinkTitle.find(html, marker)?.groupValues?.get(1)
            // An "inflection of <word>:" has the forms in a list inside it, which are again definitions of their own: cut them off
            val text = toPlainText(html.substringBefore("<ol")).removeSuffix(":").trim()
            if (lemma != null && text.isNotEmpty()) return Parsed.FormOf(text, lemma)
        }
        val text = toPlainText(html)
        if (text.isEmpty()) return null
        return if (marker < 0 && FORM_OF_CLASS in html) Parsed.Detail(text) else Parsed.Meaning(text)
    }

    /**
     * The senses of [entries]. A misspelling of another word ("misspelling of jouw") is left out: it is not what the word is, and it is
     * of no use to a learner. Only when that leaves nothing, because the page has nothing else, are the misspellings shown.
     */
    fun senses(entries: List<WiktionaryEntryDto>): List<WiktionarySense> {
        val withoutMisspellings = entries.map { sense(it, dropMisspellings = true) }.filter { it.hasContent() }
        val chosen = withoutMisspellings.ifEmpty { entries.map { sense(it, dropMisspellings = false) }.filter { it.hasContent() } }
        return chosen.take(MAX_SENSES)
    }

    private fun WiktionarySense.hasContent() = glosses.isNotEmpty() || formOf != null

    private fun isMisspelling(parsed: Parsed) = "misspelling of" in parsed.text.lowercase()

    /**
     * The sense of an entry. An entry with meanings of its own gives them in the order of the page. An entry that is only a form of
     * another word gives how it is formed, and the meanings of the other are added later.
     */
    private fun sense(entry: WiktionaryEntryDto, dropMisspellings: Boolean): WiktionarySense {
        val parsed = entry.definitions.orEmpty().mapNotNull { dto -> dto.definition?.let { parse(it) } }.filterNot { dropMisspellings && isMisspelling(it) }
        if (parsed.any { it is Parsed.Meaning }) {
            return WiktionarySense(
                partOfSpeech = entry.partOfSpeech.orEmpty(),
                glosses = parsed.filter { it !is Parsed.Detail }.map { it.text }.take(MAX_GLOSSES),
            )
        }
        val forms = parsed.filterIsInstance<Parsed.FormOf>()
        return WiktionarySense(
            partOfSpeech = entry.partOfSpeech.orEmpty(),
            glosses = emptyList(),
            formOf = forms.firstOrNull()?.text,
            formDetails = (forms.drop(1) + parsed.filterIsInstance<Parsed.Detail>()).map { it.text }.distinct(),
            lemma = forms.firstOrNull()?.lemma,
        )
    }

    /** The meanings of an entry, as plain text: the first few, not those that are a "form of" another word. */
    fun glosses(entry: WiktionaryEntryDto): List<String> =
        entry.definitions.orEmpty().mapNotNull { dto -> dto.definition?.let { parse(it) } }.filterIsInstance<Parsed.Meaning>().map { it.text }.take(MAX_GLOSSES)

    /** Takes the HTML of a definition to plain text: tags and style blocks gone, entities read, spaces collapsed. */
    fun toPlainText(html: String): String =
        html.replace(styleOrScript, "")
            .replace(tag, "")
            .replace(numericEntity) { match ->
                val radix = if (match.groupValues[1].isEmpty()) 10 else 16
                match.groupValues[2].toIntOrNull(radix)?.takeIf { it in 1..0xFFFF }?.let { it.toChar().toString() } ?: match.value
            }
            .replace("&nbsp;", " ")
            .replace("&quot;", "\"")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&amp;", "&")
            .replace(spaces, " ")
            .trim()

    /** The link to a page, at the section of a language when [language] is given (the section of "Spanish" is `#Spanish`). */
    fun pageUrl(title: String, language: String? = null): String =
        "https://en.wiktionary.org/wiki/${title.replace(' ', '_').encodeURLPath()}" +
            if (language.isNullOrBlank()) "" else "#${language.trim().replace(' ', '_')}"

    /** The link to Wiktionary's search, for a word that has no page. */
    fun searchUrl(text: String): String = "https://en.wiktionary.org/w/index.php?search=${text.encodeURLParameter()}"
}
