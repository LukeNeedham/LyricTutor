package com.lukeneedham.lyrictutor.data.lyrics

import kotlin.math.abs
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import com.lukeneedham.lyrictutor.data.apiJson

/** One LRCLIB record. Every field is optional because the API is unofficial and read leniently. */
@Serializable
data class LrclibTrackDto(
    val trackName: String? = null,
    val artistName: String? = null,
    val albumName: String? = null,
    /** Seconds, with a fraction. */
    val duration: Double? = null,
    val instrumental: Boolean? = null,
    val plainLyrics: String? = null,
    val syncedLyrics: String? = null,
) {
    /** The synced lyrics if there are any, else the plain ones, else null (also for an instrumental). */
    fun toDomain(): Lyrics? {
        if (instrumental == true) return null
        val timed = syncedLyrics?.let(LrcParser::parseTimed).orEmpty()
        if (timed.isNotEmpty()) return Lyrics(timed, LyricsSourceKind.Lrclib)
        val plain = plainLyrics?.let(LrcParser::parsePlain).orEmpty()
        if (plain.isNotEmpty()) return Lyrics(plain, LyricsSourceKind.Lrclib)
        return null
    }

    companion object {
        /** Results further than this from the song's duration are taken to be another recording or another song. */
        const val MAX_DURATION_DIFFERENCE_SECONDS = 8.0

        fun parseOne(body: String): LrclibTrackDto = apiJson.decodeFromString(serializer(), body)

        fun parseList(body: String): List<LrclibTrackDto> = apiJson.decodeFromString(ListSerializer(serializer()), body)

        /** The result with lyrics whose duration is closest to [durationSeconds]; with no duration, the first with lyrics. */
        fun pickBest(results: List<LrclibTrackDto>, durationSeconds: Int?): LrclibTrackDto? {
            val withLyrics = results.filter { it.toDomain() != null }
            if (durationSeconds == null) return withLyrics.firstOrNull()
            return withLyrics
                .filter { it.duration != null && abs(it.duration - durationSeconds) <= MAX_DURATION_DIFFERENCE_SECONDS }
                .minByOrNull { abs(it.duration!! - durationSeconds) }
        }
    }
}
