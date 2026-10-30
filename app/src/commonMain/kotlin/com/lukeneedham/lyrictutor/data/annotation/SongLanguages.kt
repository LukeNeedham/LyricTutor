package com.lukeneedham.lyrictutor.data.annotation

import com.lukeneedham.lyrictutor.data.lyrics.LyricLine

/** The languages of a song, found from its annotations. */
object SongLanguages {

    /**
     * The languages of the words of [lines], the one with the most words first (ties in alphabetical order). A word is in the language of its
     * line unless the AI named another for it. Lines without an annotation, or without a language, do not count.
     */
    fun ranked(lines: List<LyricLine>, annotations: Map<Int, LineAnnotation>): List<String> {
        val counts = mutableMapOf<String, Int>()
        for ((index, line) in lines.withIndex()) {
            val annotation = annotations[index] ?: continue
            for (word in line.text.split(' ').filter { it.isNotEmpty() }) {
                val language = annotation.languageOf(word)?.trim()?.takeIf { it.isNotEmpty() } ?: continue
                counts[language] = (counts[language] ?: 0) + 1
            }
        }
        return counts.entries.sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { it.key }).map { it.key }
    }
}
