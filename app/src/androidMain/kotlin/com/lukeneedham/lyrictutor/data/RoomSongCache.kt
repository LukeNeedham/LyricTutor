package com.lukeneedham.lyrictutor.data

import com.lukeneedham.lyrictutor.data.annotation.LineAnnotation
import com.lukeneedham.lyrictutor.data.lyrics.Lyrics
import com.lukeneedham.lyrictutor.data.song.SongCache
import com.lukeneedham.lyrictutor.data.track.TrackInfo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import com.lukeneedham.lyrictutor.data.annotation.SongLanguages
import kotlinx.coroutines.flow.map
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

class RoomSongCache(private val dao: SongDao) : SongCache {

    private val json = Json { ignoreUnknownKeys = true }
    private val artistsSerializer = ListSerializer(String.serializer())

    override val recents: Flow<List<TrackInfo>> = dao.observeSongs().map { songs -> songs.map { it.toTrack() } }

    override val languages: Flow<Map<String, List<String>>> =
        combine(dao.observeLyrics(), dao.observeAnnotations()) { lyricsRows, annotationRows ->
            val annotationsByTrack = annotationRows.groupBy { it.trackId }
            lyricsRows.associate { row ->
                val lyrics = json.decodeFromString(Lyrics.serializer(), row.json)
                val annotations = annotationsByTrack[row.trackId].orEmpty()
                    .map { json.decodeFromString(LineAnnotation.serializer(), it.json) }
                    .associateBy { it.index }
                row.trackId to SongLanguages.ranked(lyrics.lines, annotations)
            }.filterValues { it.isNotEmpty() }
        }

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

    override suspend fun getAnnotations(trackId: String): List<LineAnnotation> =
        dao.getAnnotations(trackId).map { json.decodeFromString(LineAnnotation.serializer(), it.json) }

    override suspend fun deleteAnnotations(trackId: String) = dao.deleteAnnotations(trackId)

    override suspend fun saveAnnotations(trackId: String, provider: String, annotations: List<LineAnnotation>) {
        val now = System.currentTimeMillis()
        dao.insertAnnotations(
            annotations.map {
                AnnotationEntity(
                    trackId = trackId,
                    lineIndex = it.index,
                    json = json.encodeToString(LineAnnotation.serializer(), it),
                    provider = provider,
                    createdAt = now,
                )
            },
        )
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
