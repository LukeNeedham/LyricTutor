package com.lukeneedham.lyrictutor.data.lyrics

/** Turns lyrics text into [LyricLine]s. Pure, so it is tested without a network. */
object LrcParser {
    private val timestamp = Regex("\\[(\\d{1,3}):(\\d{2})(?:[.:](\\d{1,3}))?]")

    /** Line-timed LRC (`[01:23.45] text`). A line with several timestamps becomes one line per timestamp; tags like `[ar:x]` are skipped. */
    fun parseTimed(lrc: String): List<LyricLine> =
        lrc.lineSequence()
            .flatMap { raw ->
                val stamps = timestamp.findAll(raw).toList()
                if (stamps.isEmpty()) return@flatMap emptySequence()
                val text = raw.substring(stamps.last().range.last + 1).trim()
                if (text.isEmpty()) return@flatMap emptySequence()
                stamps.asSequence().map { LyricLine(text = text, startMs = it.toMillis()) }
            }
            .sortedBy { it.startMs }
            .toList()

    /** Plain text: one line per text line, blank lines dropped. */
    fun parsePlain(text: String): List<LyricLine> =
        text.lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .map { LyricLine(text = it) }
            .toList()

    private fun MatchResult.toMillis(): Long {
        val minutes = groupValues[1].toLong()
        val seconds = groupValues[2].toLong()
        val fraction = groupValues[3]
        // "45" is hundredths and "450" thousandths: pad to three digits
        val millis = if (fraction.isEmpty()) 0L else fraction.padEnd(3, '0').take(3).toLong()
        return (minutes * 60 + seconds) * 1000 + millis
    }
}
