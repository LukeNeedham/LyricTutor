package com.lukeneedham.lyrictutor.data.annotation

import com.lukeneedham.common.logger.Logger
import com.lukeneedham.lyrictutor.data.song.SongCache
import com.lukeneedham.lyrictutor.data.track.TrackInfo
import kotlinx.coroutines.delay

/**
 * Annotates the lines of a song a few at a time (a small batch answers sooner, so the first lines show quickly), keeping every batch on the device so that a line
 * is never paid for twice. A failed batch is tried once more, then the rest is left for a later try.
 */
class AnnotationRepository(
    private val songCache: SongCache,
    private val annotator: LyricsAnnotator,
) {
    /**
     * Calls [onBatch] with the annotations already on the device, then with each new batch as it arrives.
     * Fails with the first [AnnotationException] that survives the retry (batches before it stay delivered and kept), or, after
     * going through every batch, when the model left lines out.
     */
    suspend fun annotate(track: TrackInfo, lines: List<String>, onBatch: (List<LineAnnotation>) -> Unit): Result<Unit> {
        val cached = songCache.getAnnotations(track.trackId).filter { it.index in lines.indices }
        if (cached.isNotEmpty()) onBatch(cached)

        val done = cached.map { it.index }.toSet()
        val missing = lines.mapIndexedNotNull { index, text ->
            if (index in done || text.isBlank()) null else AnnotationLine(index, text)
        }
        var leftOut = 0
        for (batch in missing.chunked(BATCH_SIZE)) {
            leftOut += annotateBatch(track, batch, onBatch).getOrElse { return Result.failure(it) }
        }
        return if (leftOut == 0) Result.success(Unit) else Result.failure(AnnotationException.Failed("The model left out $leftOut lines"))
    }

    /**
     * Asks for [batch] and, when the answer fails or lacks some lines, once more for what is still missing.
     * Success holds the number of lines left out; failure, an error that no second try fixes or an answer without a single line.
     */
    private suspend fun annotateBatch(track: TrackInfo, batch: List<AnnotationLine>, onBatch: (List<LineAnnotation>) -> Unit): Result<Int> {
        var pending = batch
        var lastError: Throwable? = null
        var delivered = false
        for (pass in 0 until MAX_PASSES) {
            if (pass > 0) delay(RETRY_DELAY_MILLIS)
            val asked = pending
            val answered = annotator.annotate(track, asked).fold(
                onSuccess = { it },
                onFailure = { error ->
                    // A missing or refused key will not get better by asking again
                    if (error !is AnnotationException.Failed) return Result.failure(error)
                    Logger.e("Annotating lines ${asked.first().index}-${asked.last().index} of ${track.title} failed", error)
                    lastError = error
                    emptyList()
                },
            ).filter { annotation -> asked.any { it.index == annotation.index } }

            if (answered.isNotEmpty()) {
                songCache.saveAnnotations(track.trackId, annotator.providerName, answered)
                onBatch(answered)
                delivered = true
            }
            pending = asked.filter { line -> answered.none { it.index == line.index } }
            if (pending.isEmpty()) return Result.success(0)
        }
        return if (delivered) Result.success(pending.size) else Result.failure(lastError ?: AnnotationException.Failed("The model answered no line"))
    }

    companion object {
        const val BATCH_SIZE = 6
        private const val MAX_PASSES = 2
        private const val RETRY_DELAY_MILLIS = 2_000L
    }
}
