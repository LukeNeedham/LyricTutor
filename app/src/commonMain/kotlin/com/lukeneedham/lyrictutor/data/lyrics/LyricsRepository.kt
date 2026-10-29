package com.lukeneedham.lyrictutor.data.lyrics

import com.lukeneedham.lyrictutor.data.song.SongCache
import com.lukeneedham.lyrictutor.data.track.TrackInfo

/** The lyrics of a song: from the device if they are there, else from the network (and then kept), else pasted by the user. */
class LyricsRepository(
    private val songCache: SongCache,
    private val source: LyricsSource,
) {
    /** Success with null when no source has the song. A miss is not kept, so a later try asks again. */
    suspend fun load(track: TrackInfo): Result<Lyrics?> {
        songCache.getLyrics(track.trackId)?.let { return Result.success(it) }
        return source.find(track).onSuccess { lyrics ->
            if (lyrics != null) songCache.saveLyrics(track.trackId, lyrics)
        }
    }

    /** Keeps [text] as the lyrics of [track]. Null when it has no lines. */
    suspend fun savePasted(track: TrackInfo, text: String): Lyrics? {
        val lines = LrcParser.parsePlain(text)
        if (lines.isEmpty()) return null
        return Lyrics(lines, LyricsSourceKind.Pasted).also { songCache.saveLyrics(track.trackId, it) }
    }
}
