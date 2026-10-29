package com.lukeneedham.lyrictutor.data.track

/** Strips the parts of a Spotify title that stop a lyrics search from matching. */
object TitleCleaner {
    private val bracketedSuffix = Regex(
        "\\s*[(\\[]\\s*(?:with|feat\\.?|ft\\.?|featuring|[^)\\]]*remaster[^)\\]]*)(?:\\s[^)\\]]*)?[)\\]]\\s*$",
        RegexOption.IGNORE_CASE,
    )
    private val dashedSuffix = Regex(
        "\\s+-\\s+(?:(?:feat\\.?|ft\\.?|with)\\s.*|.*remaster.*|single version|radio edit|mono|stereo)$",
        RegexOption.IGNORE_CASE,
    )

    fun clean(title: String): String {
        var result = title.trim()
        while (true) {
            val next = result.replace(bracketedSuffix, "").replace(dashedSuffix, "").trim()
            if (next == result || next.isEmpty()) return result
            result = next
        }
    }
}
