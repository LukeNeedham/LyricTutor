package com.lukeneedham.lyrictutor.data

import com.lukeneedham.lyrictutor.data.lyrics.Lyrics
import com.lukeneedham.lyrictutor.data.song.SongCache
import com.lukeneedham.lyrictutor.data.track.TrackInfo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

class RoomSongCache(private val dao: SongDao) : SongCache {

    private val json = Json { ignoreUnknownKeys = true }
    private val artistsSerializer = ListSerializer(String.serializer())

    override val recents: Flow<List<TrackInfo>> = dao.observeSongs().map { songs -> songs.map { it.toTrack() } }

    override suspend fun getTrack(trackId: String): TrackInfo? = dao.getSong(trackId)?.toTrack()

    override suspend fun saveTrack(track: TrackInfo) {
        dao.insertSong(
            SongEntity(
                trackId = track.trackId,
                title = track.title,
                searchTitle = track.searchTitle,
                artistsJson = json.encodeToString(artistsSerializer, track.artists),
                coverUrl = track.coverUrl,
                durationSeconds = track.durationSeconds,
                releaseDate = track.releaseDate,
                lastOpened = System.currentTimeMillis(),
            ),
        )
    }

    override suspend fun getLyrics(trackId: String): Lyrics? =
        dao.getLyrics(trackId)?.let { json.decodeFromString(Lyrics.serializer(), it.json) }

    override suspend fun saveLyrics(trackId: String, lyrics: Lyrics) {
        dao.insertLyrics(LyricsEntity(trackId = trackId, json = json.encodeToString(Lyrics.serializer(), lyrics)))
    }

    private fun SongEntity.toTrack() = TrackInfo(
        trackId = trackId,
        title = title,
        searchTitle = searchTitle,
        artists = json.decodeFromString(artistsSerializer, artistsJson),
        coverUrl = coverUrl,
        durationSeconds = durationSeconds,
        releaseDate = releaseDate,
    )
}
