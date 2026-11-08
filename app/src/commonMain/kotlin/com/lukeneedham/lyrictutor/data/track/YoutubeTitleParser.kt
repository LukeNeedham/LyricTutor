package com.lukeneedham.lyrictutor.data.track

/** Reads a song title and artist from what YouTube shows: a video title like "Artist - Song (Official Video)" and the channel name. */
object YoutubeTitleParser {
    private const val NOISE_WORDS =
        "official|video|audio|lyrics?|letra|oficial|clip|visuali[sz]er|hd|4k|music|m/v|mv|con letra|con lyrics|subtitulado|sub español|en vivo|live"
    private val bracketedNoise = Regex("\\s*[(\\[][^)\\]]*\\b(?:$NOISE_WORDS)\\b[^)\\]]*[)\\]]", RegexOption.IGNORE_CASE)
    private val dashedNoise = Regex("\\s+[-–|]\\s+(?:official\\s.*|(?:lyrics?|letra)(?:\\s.*)?|audio|video)$", RegexOption.IGNORE_CASE)
    private val separator = Regex("\\s+[-–—]\\s+")
    private val channelNoise = Regex("(?:\\s*-\\s*topic|\\s*vevo|\\s*official|\\s*oficial)$", RegexOption.IGNORE_CASE)

    /** The song and artist, or null when the title is empty. The artist is the channel name when the title has none. */
    fun parse(videoTitle: String, channel: String?): Pair<String, String>? {
        val title = videoTitle.replace(bracketedNoise, "").replace(dashedNoise, "").trim()
        if (title.isEmpty()) return null
        val parts = title.split(separator, limit = 2)
        if (parts.size == 2 && parts.all { it.isNotBlank() }) return parts[1].trim() to parts[0].trim()
        val artist = channel?.replace(channelNoise, "")?.trim().orEmpty()
        return title to artist
    }
}
