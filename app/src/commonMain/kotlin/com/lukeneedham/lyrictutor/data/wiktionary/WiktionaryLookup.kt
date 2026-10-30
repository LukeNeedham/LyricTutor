package com.lukeneedham.lyrictutor.data.wiktionary

/** One part of speech of a word, as Wiktionary has it. */
data class WiktionarySense(
    /** For example "Verb" or "Adjective". */
    val partOfSpeech: String,
    /**
     * For a word that is an inflected or variant form of another, how it is formed, for example
     * "first-person singular present indicative"; null for a word that is a dictionary form itself.
     */
    val formText: String?,
    /** The dictionary form that [formText] is a form of, for example "ir". */
    val lemma: String?,
    /** The first meanings: those of the word itself, or of its [lemma] for a form. */
    val glosses: List<String>,
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
