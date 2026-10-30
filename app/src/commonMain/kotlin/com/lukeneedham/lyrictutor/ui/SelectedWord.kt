package com.lukeneedham.lyrictutor.ui

import com.lukeneedham.lyrictutor.data.annotation.WordAnnotation

/** The word whose sheet is open, and where it was tapped: the same word can be in several lines, and several times in one. */
data class SelectedWord(
    val word: WordAnnotation,
    /** The index of the line in the lyrics. */
    val lineIndex: Int,
    /** The place of the tapped word among the words of the line. */
    val tokenIndex: Int,
)
