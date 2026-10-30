package com.lukeneedham.lyrictutor.data.wiktionary

/** One part of speech of a word, as Wiktionary has it. */
data class WiktionarySense(
    /** For example "Verb" or "Adjective". */
    val partOfSpeech: String,
    /** The meanings, in the order of the page: those of the word itself, or, for a word that is only a form, those of its [lemma]. */
    val glosses: List<String>,
    /**
     * For a word that is only a form of another word (it has no meaning of its own), what the page says of it, for example
     * "first-person singular present indicative of ir" or "inflection of jouen". Null for a word with meanings of its own.
     */
    val formOf: String? = null,
    /** The lines under [formOf] that say which forms it is, for example "imperative". */
    val formDetails: List<String> = emptyList(),
    /** The dictionary form that the word is a form of, for example "ir". */
    val lemma: String? = null,
)

/** What Wiktionary says about a word in a language. */
data class WiktionaryResult(
    /** The title of the page that was found, for example "voy". */
    val title: String,
    /** The English name of the language, for example "Spanish". */
    val language: String,
    /** The link to the page, at the section of the language. */
    val url: String,
    val senses: List<WiktionarySense>,
)

interface WiktionaryLookup {
    /**
     * Looks [word] up in the English Wiktionary, in [language] (null when not known). Success with null when there is no page for it
     * in that language. Failure when Wiktionary could not be asked.
     */
    suspend fun find(word: String, language: String?): Result<WiktionaryResult?>
}
