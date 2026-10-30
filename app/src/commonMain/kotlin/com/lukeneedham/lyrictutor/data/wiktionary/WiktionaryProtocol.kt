package com.lukeneedham.lyrictutor.data.wiktionary

import io.ktor.http.encodeURLParameter
import io.ktor.http.encodeURLPath
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

/**
 * The pieces of the Wiktionary lookup that need no network: reading the answer of the REST `page/definition/<word>` endpoint, and the
 * words and links. The answer is an object by language code ("es", "nl", "other") of entries, one for each part of speech.
 */
object WiktionaryProtocol {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    /** One part of speech of one language on a page: [definitions] are HTML. */
    @Serializable
    data class Entry(
        val partOfSpeech: String = "",
        val language: String = "",
        val definitions: List<Definition> = emptyList(),
    )

    @Serializable
    data class Definition(val definition: String = "")

    /** A definition that says "<form> of <lemma>". */
    data class FormOf(val formText: String, val lemma: String)

    private const val MAX_GLOSSES = 4
    private const val MAX_SENSES = 3
    private const val FORM_OF_MARKER = "form-of-definition-link"
    private val styleOrScript = Regex("<(style|script)[^>]*>.*?</\\1>", setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE))
    private val tag = Regex("<[^>]*>")
    private val spaces = Regex("\\s+")
    private val numericEntity = Regex("&#(x?)([0-9a-fA-F]+);")
    private val firstLinkTitle = Regex("title=\"([^\"]+)\"")

    /** The entries of every language on the page, in the order of the answer; empty when the answer is not one. */
    fun parseEntries(body: String): List<Entry> = try {
        json.decodeFromString(MapSerializer(String.serializer(), ListSerializer(Entry.serializer())), body).values.flatten()
    } catch (e: SerializationException) {
        emptyList()
    } catch (e: IllegalArgumentException) {
        emptyList()
    }

    /**
     * The entries to read for [language] (its English name): those of that language; when the page has none of it but only one
     * language, that language's. Empty when there are none to choose.
     */
    fun entriesFor(entries: List<Entry>, language: String?): List<Entry> {
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

    /** The senses of [entries]: a sense for each, with the meanings it has itself and, for a form of another word, how it is formed. */
    fun senses(entries: List<Entry>): List<WiktionarySense> =
        entries.take(MAX_SENSES).map { entry ->
            val forms = entry.definitions.mapNotNull { formOf(it.definition) }
            WiktionarySense(
                partOfSpeech = entry.partOfSpeech,
                formText = forms.map { it.formText }.filter { it.isNotEmpty() }.distinct().joinToString("; ").ifEmpty { null },
                lemma = forms.firstOrNull()?.lemma,
                glosses = glosses(entry),
            )
        }

    /** The meanings of an entry that are not "form of" another word: the first few, as plain text. */
    fun glosses(entry: Entry): List<String> =
        entry.definitions
            .filter { FORM_OF_MARKER !in it.definition }
            .map { toPlainText(it.definition) }
            .filter { it.isNotEmpty() }
            .take(MAX_GLOSSES)

    /** The "<form> of <lemma>" of a definition, or null for a definition that is a meaning. */
    fun formOf(definitionHtml: String): FormOf? {
        val marker = definitionHtml.indexOf("class=\"$FORM_OF_MARKER\"")
        if (marker < 0) return null
        val lemma = firstLinkTitle.find(definitionHtml, marker)?.groupValues?.get(1) ?: return null
        // What stands before the link is "first-person singular present indicative of": the form, and "of"
        val before = toPlainText(definitionHtml.substring(0, definitionHtml.lastIndexOf('<', marker)))
        return FormOf(formText = before.removeSuffix("of").trim(), lemma = lemma)
    }

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
