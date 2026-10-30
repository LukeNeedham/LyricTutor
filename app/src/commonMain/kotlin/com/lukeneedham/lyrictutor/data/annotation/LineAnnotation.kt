package com.lukeneedham.lyrictutor.data.annotation

import kotlinx.serialization.Serializable

/** A word of a line that is not in the language of the rest of the line. */
@Serializable
data class WordLanguage(
    val word: String,
    /** The English name of the language, for example "Spanish". */
    val language: String,
)

/**
 * What the AI says about a line: only what nothing else can give. Everything about a single word (its dictionary form, its form,
 * its meaning) comes from Wiktionary when the user taps the word.
 */
@Serializable
data class LineAnnotation(
    /** The index of the line in the song's lyrics. */
    val index: Int,
    val translation: String? = null,
    /** The English name of the language of the line, for example "Spanish": it says which language of a Wiktionary page to read. */
    val language: String? = null,
    /** The words of the line in another language than [language]: a song can mix languages. */
    val otherLanguages: List<WordLanguage> = emptyList(),
) {
    /** Whether the AI gave the translation and the language. An answer without them is asked for again. */
    val isComplete: Boolean
        get() = !translation.isNullOrBlank() && !language.isNullOrBlank()

    /** The language of [word] in this line: the language of the line, unless the AI named another for this word. */
    fun languageOf(word: String): String? =
        otherLanguages.firstOrNull { it.word.normalizedWord() == word.normalizedWord() }?.language ?: language

    private fun String.normalizedWord(): String = lowercase().filter { it.isLetterOrDigit() }
}
