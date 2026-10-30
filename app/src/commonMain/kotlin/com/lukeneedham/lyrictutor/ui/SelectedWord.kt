package com.lukeneedham.lyrictutor.ui

/** The word whose sheet is open, and where it was tapped: the same word can be in several lines, and several times in one. */
data class SelectedWord(
    /** The word as written in the line. */
    val surface: String,
    /** The English name of its language, when the AI has given it. */
    val language: String?,
    /** The index of the line in the lyrics. */
    val lineIndex: Int,
    /** The place of the tapped word among the words of the line. */
    val tokenIndex: Int,
)
