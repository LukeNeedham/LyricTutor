package com.lukeneedham.lyrictutor.data.annotation

import kotlinx.serialization.Serializable

@Serializable
data class LineAnnotation(
    /** The index of the line in the song's lyrics. */
    val index: Int,
    val translation: String? = null,
    val words: List<WordAnnotation> = emptyList(),
) {
    /**
     * Whether the model gave what the sheet of a word needs: a translation, and for every word its dictionary form and its meaning.
     * A smaller model may skip the optional-looking fields, and an answer like that is asked for again.
     */
    val isComplete: Boolean
        get() = !translation.isNullOrBlank() && words.isNotEmpty() && words.all { !it.lemma.isNullOrBlank() && !it.meaning.isNullOrBlank() }
}
