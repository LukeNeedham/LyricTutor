package com.lukeneedham.lyrictutor.data.wiktionary

import com.lukeneedham.lyrictutor.data.annotation.WordAnnotation

/** A page of Wiktionary, at the section of the word's language when the page has one. */
data class WiktionaryEntry(val title: String, val url: String)

interface WiktionaryLookup {
    /** Success with null when no page was found for the word. Failure when Wiktionary could not be asked. */
    suspend fun find(word: WordAnnotation): Result<WiktionaryEntry?>
}
