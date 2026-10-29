package com.lukeneedham.lyrictutor.data.lyrics

import com.lukeneedham.lyrictutor.data.song.SongCache
import com.lukeneedham.lyrictutor.data.track.TrackInfo
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest

class LyricsRepositoryTest {

    private val track = TrackInfo(
        trackId = "id",
        title = "Song",
        searchTitle = "Song",
        artists = listOf("Artist"),
        coverUrl = null,
        durationSeconds = 200,
        releaseDate = null,
    )
    private val lyrics = Lyrics(listOf(LyricLine("uno")), LyricsSourceKind.Lrclib)

    private class FakeCache : SongCache {
        val savedLyrics = mutableMapOf<String, Lyrics>()
        override val recents: Flow<List<TrackInfo>> = flowOf(emptyList())
        override suspend fun getTrack(trackId: String): TrackInfo? = null
        override suspend fun saveTrack(track: TrackInfo) = Unit
        override suspend fun getLyrics(trackId: String): Lyrics? = savedLyrics[trackId]
        override suspend fun saveLyrics(trackId: String, lyrics: Lyrics) { savedLyrics[trackId] = lyrics }
    }

    private class FakeSource(private val result: Result<Lyrics?>) : LyricsSource {
        var calls = 0
        override suspend fun find(track: TrackInfo): Result<Lyrics?> {
            calls++
            return result
        }
    }

    @Test
    fun fetchesAndKeepsLyrics() = runTest {
        val cache = FakeCache()
        val source = FakeSource(Result.success(lyrics))
        val repository = LyricsRepository(cache, source)

        assertEquals(lyrics, repository.load(track).getOrThrow())
        assertEquals(lyrics, repository.load(track).getOrThrow())
        assertEquals(1, source.calls)
        assertEquals(lyrics, cache.savedLyrics["id"])
    }

    @Test
    fun doesNotKeepAMiss() = runTest {
        val cache = FakeCache()
        val source = FakeSource(Result.success(null))
        val repository = LyricsRepository(cache, source)

        assertNull(repository.load(track).getOrThrow())
        assertNull(repository.load(track).getOrThrow())
        assertEquals(2, source.calls)
        assertTrue(cache.savedLyrics.isEmpty())
    }

    @Test
    fun passesOnAFailure() = runTest {
        val repository = LyricsRepository(FakeCache(), FakeSource(Result.failure(IllegalStateException("down"))))
        assertTrue(repository.load(track).isFailure)
    }

    @Test
    fun keepsPastedLyricsAndReadsThemBack() = runTest {
        val cache = FakeCache()
        val source = FakeSource(Result.success(null))
        val repository = LyricsRepository(cache, source)

        val pasted = repository.savePasted(track, "uno\n\ndos")!!
        assertEquals(listOf("uno", "dos"), pasted.lines.map { it.text })
        assertEquals(LyricsSourceKind.Pasted, pasted.source)
        assertEquals(pasted, repository.load(track).getOrThrow())
        assertEquals(0, source.calls)
    }

    @Test
    fun ignoresBlankPastedText() = runTest {
        assertNull(LyricsRepository(FakeCache(), FakeSource(Result.success(null))).savePasted(track, "  \n "))
    }
}
