package com.lukeneedham.lyrictutor.data.annotation

import kotlinx.serialization.Serializable

/** What the model says about one word of a line. Only [surface] is required: the model may leave the rest out. */
@Serializable
data class WordAnnotation(
    /** The word as written in the line. */
    val surface: String,
    /** The English name of the language of this word, such as "Spanish" or "Dutch": a song can mix languages. */
    val language: String? = null,
    /** The dictionary form. */
    val lemma: String? = null,
    val partOfSpeech: String? = null,
    /** Tense, mood, person, number, gender, as plain words. */
    val morphology: String? = null,
    /** What the word means in this line. */
    val meaning: String? = null,
    /** Slang, an idiom, an elision such as "pa'" for "para". */
    val note: String? = null,
    /** English words with the same root, to help remember the word. */
    val cognates: List<String> = emptyList(),
)
