package com.lukeneedham.lyrictutor.data.annotation

import kotlinx.serialization.Serializable

@Serializable
data class LineAnnotation(
    /** The index of the line in the song's lyrics. */
    val index: Int,
    val translation: String? = null,
    val words: List<WordAnnotation> = emptyList(),
)
