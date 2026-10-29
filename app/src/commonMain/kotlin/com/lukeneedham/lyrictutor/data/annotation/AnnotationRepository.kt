package com.lukeneedham.lyrictutor.data.annotation

import com.lukeneedham.lyrictutor.data.song.SongCache
import com.lukeneedham.lyrictutor.data.track.TrackInfo

/**
 * Annotates the lines of a song a few at a time (the free tier limits requests), keeping every batch on the device so that a line
 * is never paid for twice. A failed batch is tried once more, then the rest is left for a later try.
 */
class AnnotationRepository(
    private val songCache: SongCache,
    private val annotator: LyricsAnnotator,
) {
    /**
     * Calls [onBatch] with the annotations already on the device, then with each new batch as it arrives.
     * Fails with the first [AnnotationException] that survives the retry; batches before it stay delivered and kept.
     */
    suspend fun annotate(track: TrackInfo, lines: List<String>, onBatch: (List<LineAnnotation>) -> Unit): Result<Unit> {
        val cached = songCache.getAnnotations(track.trackId).filter { it.index in lines.indices }
        if (cached.isNotEmpty()) onBatch(cached)

        val done = cached.map { it.index }.toSet()
        val missing = lines.mapIndexedNotNull { index, text ->
            if (index in done || text.isBlank()) null else AnnotationLine(index, text)
        }
        for (batch in missing.chunked(BATCH_SIZE)) {
            val result = annotator.annotate(track, batch).let { first ->
                // A missing or refused key will not get better by asking again
                if (first.isSuccess || first.exceptionOrNull() !is AnnotationException.Failed) first else annotator.annotate(track, batch)
            }
            val annotations = result.getOrElse { return Result.failure(it) }
                .filter { annotation -> batch.any { it.index == annotation.index } }
            songCache.saveAnnotations(track.trackId, annotator.providerName, annotations)
            onBatch(annotations)
        }
        return Result.success(Unit)
    }

    companion object {
        const val BATCH_SIZE = 15
    }
}
