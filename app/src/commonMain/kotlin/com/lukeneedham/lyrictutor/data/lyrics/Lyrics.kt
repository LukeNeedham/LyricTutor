package com.lukeneedham.lyrictutor.data.lyrics

import kotlinx.serialization.Serializable

@Serializable
data class LyricLine(
    val text: String,
    /** When the line starts in the song, or null when the source has no timing. Kept for highlighting the current line later. */
    val startMs: Long? = null,
)

@Serializable
data class Lyrics(
    val lines: List<LyricLine>,
    val source: LyricsSourceKind,
)

@Serializable
enum class LyricsSourceKind { Lrclib, Pasted }
