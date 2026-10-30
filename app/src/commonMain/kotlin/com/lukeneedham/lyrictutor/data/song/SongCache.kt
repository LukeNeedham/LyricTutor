package com.lukeneedham.lyrictutor.data.song

import com.lukeneedham.lyrictutor.data.annotation.LineAnnotation
import com.lukeneedham.lyrictutor.data.lyrics.Lyrics
import com.lukeneedham.lyrictutor.data.track.TrackInfo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/** What the app keeps on the device: the songs the user opened and their lyrics. Implemented with Room in `androidMain`. */
interface SongCache {
    /** The songs, most recently opened first. */
    val recents: Flow<List<TrackInfo>>

    /** The languages of each song that has translations, by track id: the most common language first. */
    val languages: Flow<Map<String, List<String>>>
        get() = flowOf(emptyMap())

    suspend fun getTrack(trackId: String): TrackInfo?

    /** Saves [track] and marks it as opened now. */
    suspend fun saveTrack(track: TrackInfo)

    suspend fun getLyrics(trackId: String): Lyrics?

    suspend fun saveLyrics(trackId: String, lyrics: Lyrics)

    suspend fun getAnnotations(trackId: String): List<LineAnnotation>

    /** Forgets the translations of the song, so that the next try asks for them again. */
    suspend fun deleteAnnotations(trackId: String)

    /** Keeps [annotations] of the song, replacing those of the same lines. [provider] says where they came from. */
    suspend fun saveAnnotations(trackId: String, provider: String, annotations: List<LineAnnotation>)
}
