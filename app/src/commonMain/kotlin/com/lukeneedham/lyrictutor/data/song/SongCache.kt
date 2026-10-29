package com.lukeneedham.lyrictutor.data.song

import com.lukeneedham.lyrictutor.data.lyrics.Lyrics
import com.lukeneedham.lyrictutor.data.track.TrackInfo
import kotlinx.coroutines.flow.Flow

/** What the app keeps on the device: the songs the user opened and their lyrics. Implemented with Room in `androidMain`. */
interface SongCache {
    /** The songs, most recently opened first. */
    val recents: Flow<List<TrackInfo>>

    suspend fun getTrack(trackId: String): TrackInfo?

    /** Saves [track] and marks it as opened now. */
    suspend fun saveTrack(track: TrackInfo)

    suspend fun getLyrics(trackId: String): Lyrics?

    suspend fun saveLyrics(trackId: String, lyrics: Lyrics)
}
