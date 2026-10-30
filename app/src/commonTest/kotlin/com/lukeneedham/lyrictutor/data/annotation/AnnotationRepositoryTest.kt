package com.lukeneedham.lyrictutor.data.annotation

import com.lukeneedham.lyrictutor.data.lyrics.Lyrics
import com.lukeneedham.lyrictutor.data.song.SongCache
import com.lukeneedham.lyrictutor.data.track.TrackInfo
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest

/** An annotation with everything the sheet of a word needs. */
private fun complete(index: Int, translation: String) = LineAnnotation(index, translation, language = "Spanish")

class AnnotationRepositoryTest {

    private val track = TrackInfo(
        trackId = "id",
        title = "Song",
        searchTitle = "Song",
        artists = listOf("Artist"),
        coverUrl = null,
        durationSeconds = null,
        releaseDate = null,
    )

    private class FakeCache(val annotations: MutableMap<Int, LineAnnotation> = mutableMapOf()) : SongCache {
        override val recents: Flow<List<TrackInfo>> = flowOf(emptyList())
        override suspend fun getTrack(trackId: String): TrackInfo? = null
        override suspend fun saveTrack(track: TrackInfo) = Unit
        override suspend fun getLyrics(trackId: String): Lyrics? = null
        override suspend fun saveLyrics(trackId: String, lyrics: Lyrics) = Unit
        override suspend fun getAnnotations(trackId: String) = annotations.values.toList()
        override suspend fun deleteAnnotations(trackId: String) { annotations.clear() }
        override suspend fun saveAnnotations(trackId: String, provider: String, annotations: List<LineAnnotation>) {
            annotations.forEach { this.annotations[it.index] = it }
        }
    }

    /** Answers each batch with a translation of every line, or with the next failure of [failures]. */
    private class FakeAnnotator(private val failures: MutableList<AnnotationException> = mutableListOf()) : LyricsAnnotator {
        override val providerName = "fake"
        val batches = mutableListOf<List<Int>>()

        override suspend fun annotate(song: TrackInfo, lines: List<AnnotationLine>): Result<List<LineAnnotation>> {
            batches += lines.map { it.index }
            failures.removeFirstOrNull()?.let { return Result.failure(it) }
            return Result.success(lines.map { complete(it.index, "t${it.index}") })
        }
    }

    private fun lines(count: Int) = List(count) { "line $it" }

    @Test
    fun annotatesInBatchesAndKeepsThem() = runTest {
        val cache = FakeCache()
        val annotator = FakeAnnotator()
        val delivered = mutableListOf<LineAnnotation>()

        val result = AnnotationRepository(cache, annotator).annotate(track, lines(AnnotationRepository.BATCH_SIZE + 5)) { delivered += it }

        assertTrue(result.isSuccess)
        assertEquals(2, annotator.batches.size)
        assertEquals(AnnotationRepository.BATCH_SIZE + 5, delivered.size)
        assertEquals(AnnotationRepository.BATCH_SIZE + 5, cache.annotations.size)
    }

    @Test
    fun doesNotAskAgainForKeptLines() = runTest {
        val cache = FakeCache(mutableMapOf(0 to complete(0, "kept"), 1 to complete(1, "kept")))
        val annotator = FakeAnnotator()
        val delivered = mutableListOf<LineAnnotation>()

        AnnotationRepository(cache, annotator).annotate(track, lines(3)) { delivered += it }

        assertEquals(listOf(listOf(2)), annotator.batches)
        assertEquals(3, delivered.size)
    }

    @Test
    fun asksOnceForALineThatRepeatsAndGivesItToEveryCopy() = runTest {
        val cache = FakeCache()
        val annotator = FakeAnnotator()
        val delivered = mutableListOf<LineAnnotation>()

        // The same line again, in another case and with other spacing
        val result = AnnotationRepository(cache, annotator).annotate(track, listOf("hola", "adios", "hola", "HOLA  ", "adios")) { delivered += it }

        assertTrue(result.isSuccess)
        assertEquals(listOf(listOf(0, 1)), annotator.batches)
        assertEquals(setOf(0, 1, 2, 3, 4), cache.annotations.keys)
        assertEquals(listOf("t0", "t1", "t0", "t0", "t1"), (0..4).map { cache.annotations.getValue(it).translation })
        assertEquals(5, delivered.size)
    }

    @Test
    fun givesAKeptAnnotationToLinesThatRepeatItAndAreNew() = runTest {
        val cache = FakeCache(mutableMapOf(0 to complete(0, "kept")))
        val annotator = FakeAnnotator()
        val delivered = mutableListOf<LineAnnotation>()

        AnnotationRepository(cache, annotator).annotate(track, listOf("hola", "adios", "hola")) { delivered += it }

        assertEquals(listOf(listOf(1)), annotator.batches)
        assertEquals("kept", cache.annotations.getValue(2).translation)
        assertEquals(setOf(0, 1, 2), delivered.map { it.index }.toSet())
    }

    @Test
    fun skipsBlankLines() = runTest {
        val annotator = FakeAnnotator()
        AnnotationRepository(FakeCache(), annotator).annotate(track, listOf("uno", " ", "dos")) {}
        assertEquals(listOf(listOf(0, 2)), annotator.batches)
    }

    @Test
    fun triesAFailedBatchOnceMore() = runTest {
        val annotator = FakeAnnotator(mutableListOf(AnnotationException.Failed("boom")))
        val result = AnnotationRepository(FakeCache(), annotator).annotate(track, lines(2)) {}

        assertTrue(result.isSuccess)
        assertEquals(2, annotator.batches.size)
    }

    @Test
    fun stopsAfterTheRetryFailsToo() = runTest {
        val annotator = FakeAnnotator(mutableListOf(AnnotationException.Failed("a"), AnnotationException.Failed("b")))
        val cache = FakeCache()
        val result = AnnotationRepository(cache, annotator).annotate(track, lines(AnnotationRepository.BATCH_SIZE + 5)) {}

        assertIs<AnnotationException.Failed>(result.exceptionOrNull())
        assertEquals(2, annotator.batches.size)
        assertTrue(cache.annotations.isEmpty())
    }

    @Test
    fun doesNotRetryAMissingKey() = runTest {
        val annotator = FakeAnnotator(mutableListOf(AnnotationException.MissingKey()))
        val result = AnnotationRepository(FakeCache(), annotator).annotate(track, lines(2)) {}

        assertIs<AnnotationException.MissingKey>(result.exceptionOrNull())
        assertEquals(1, annotator.batches.size)
    }

    @Test
    fun asksAgainForLinesTheAnswerLeftOutAndThenSaysSo() = runTest {
        // Answers only the first line of each batch
        val annotator = object : LyricsAnnotator {
            override val providerName = "fake"
            val asked = mutableListOf<List<Int>>()
            override suspend fun annotate(song: TrackInfo, lines: List<AnnotationLine>): Result<List<LineAnnotation>> {
                asked += lines.map { it.index }
                return Result.success(listOf(complete(lines.first().index, "t")))
            }
        }
        val cache = FakeCache()
        val result = AnnotationRepository(cache, annotator).annotate(track, lines(3)) {}

        assertEquals(listOf(listOf(0, 1, 2), listOf(1, 2)), annotator.asked)
        assertEquals(setOf(0, 1), cache.annotations.keys)
        assertIs<AnnotationException.Failed>(result.exceptionOrNull())
    }

    @Test
    fun asksAgainWhenTheLanguageIsMissingAndShowsTheBestWithoutKeepingIt() = runTest {
        val answers = mutableListOf<List<Int>>()
        // Always answers the line, but without its language
        val annotator = object : LyricsAnnotator {
            override val providerName = "fake"
            override suspend fun annotate(song: TrackInfo, lines: List<AnnotationLine>): Result<List<LineAnnotation>> {
                answers += lines.map { it.index }
                return Result.success(lines.map { LineAnnotation(it.index, "t") })
            }
        }
        val cache = FakeCache()
        val delivered = mutableListOf<LineAnnotation>()
        val result = AnnotationRepository(cache, annotator).annotate(track, lines(1)) { delivered += it }

        assertEquals(listOf(listOf(0), listOf(0)), answers)
        assertEquals(1, delivered.size)
        assertTrue(cache.annotations.isEmpty())
        assertIs<AnnotationException.Failed>(result.exceptionOrNull())
    }

    @Test
    fun asksAgainForKeptAnnotationsThatAreNotComplete() = runTest {
        val cache = FakeCache(mutableMapOf(0 to LineAnnotation(0, "kept")))
        val annotator = FakeAnnotator()
        AnnotationRepository(cache, annotator).annotate(track, lines(1)) {}

        assertEquals(listOf(listOf(0)), annotator.batches)
        assertEquals("t0", cache.annotations.getValue(0).translation)
    }

    @Test
    fun ignoresAnnotationsOfLinesThatWereNotAsked() = runTest {
        val annotator = object : LyricsAnnotator {
            override val providerName = "fake"
            override suspend fun annotate(song: TrackInfo, lines: List<AnnotationLine>) =
                Result.success(listOf(complete(0, "asked"), complete(99, "stray")))
        }
        val cache = FakeCache()
        AnnotationRepository(cache, annotator).annotate(track, lines(1)) {}

        assertEquals(setOf(0), cache.annotations.keys)
    }
}
