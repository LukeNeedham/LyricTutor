package com.lukeneedham.lyrictutor.data.annotation

import com.lukeneedham.common.logger.Logger
import com.lukeneedham.lyrictutor.data.song.SongCache
import com.lukeneedham.lyrictutor.data.track.TrackInfo
import kotlinx.coroutines.delay

/**
 * Annotates the lines of a song a few at a time (a small batch answers sooner and a bad answer loses less; the prompt is a small part of the cost), keeping every batch on the device so that a line
 * is never paid for twice. A failed batch is tried once more, then the rest is left for a later try.
 */
class AnnotationRepository(
    private val songCache: SongCache,
    private val annotator: LyricsAnnotator,
) {
    /**
     * Calls [onBatch] with the annotations already on the device, then with each new batch as it arrives.
     * A line that repeats (a chorus) is annotated once and the result is given to every identical line.
     * Fails with the first [AnnotationException] that survives the retry (batches before it stay delivered and kept), or, after
     * going through every batch, when the model left lines out.
     */
    suspend fun annotate(track: TrackInfo, lines: List<String>, onBatch: (List<LineAnnotation>) -> Unit): Result<Unit> {
        // Annotations that are not complete (from a model that skipped words or meanings) are asked for again
        val kept = songCache.getAnnotations(track.trackId).filter { it.index in lines.indices && it.isComplete }.associateBy { it.index }

        // The indexes of the lines that say the same, in the order of the song
        val groups = lines.indices
            .filter { lines[it].isNotBlank() }
            .groupBy { sameLineKey(lines[it]) }
            .values

        // A line whose twin was annotated before is given the same annotation
        val fromDevice = mutableListOf<LineAnnotation>()
        val copies = mutableListOf<LineAnnotation>()
        for (group in groups) {
            val known = group.firstNotNullOfOrNull { kept[it] } ?: continue
            for (index in group) {
                fromDevice += kept[index] ?: known.copy(index = index).also { copies += it }
            }
        }
        if (copies.isNotEmpty()) songCache.saveAnnotations(track.trackId, annotator.providerName, copies)
        if (fromDevice.isNotEmpty()) onBatch(fromDevice)

        // Only the first line of each group that has no annotation yet is asked for
        val unannotated = groups.filter { group -> group.none { it in kept } }
        val groupOf = unannotated.associateBy { it.first() }
        val toAsk = unannotated.map { AnnotationLine(it.first(), lines[it.first()]) }

        var leftOut = 0
        for (batch in toAsk.chunked(BATCH_SIZE)) {
            leftOut += annotateBatch(track, batch, groupOf, onBatch).getOrElse { return Result.failure(it) }
        }
        return if (leftOut == 0) Result.success(Unit) else Result.failure(AnnotationException.Failed("The model left out or did not finish $leftOut lines"))
    }

    /** Lines that differ only by case or spacing are the same line. */
    private fun sameLineKey(line: String): String = line.trim().lowercase().replace(Regex("\\s+"), " ")

    /**
     * Asks for [batch] and, when the answer fails or lacks some lines, or has lines that are not complete, once more for what is still
     * missing. [groupOf] says, for the first line of a group, all the lines of the group: they get the same annotation.
     * An incomplete annotation that never improves is shown but not kept, so that a later try asks again.
     * Success holds the number of lines that are not complete; failure, an error that no second try fixes or an answer without a
     * single line.
     */
    private suspend fun annotateBatch(
        track: TrackInfo,
        batch: List<AnnotationLine>,
        groupOf: Map<Int, List<Int>>,
        onBatch: (List<LineAnnotation>) -> Unit,
    ): Result<Int> {
        fun everyLine(annotations: List<LineAnnotation>) = annotations.flatMap { annotation ->
            (groupOf[annotation.index] ?: listOf(annotation.index)).map { annotation.copy(index = it) }
        }

        var pending = batch
        var lastError: Throwable? = null
        var delivered = false
        val incomplete = mutableMapOf<Int, LineAnnotation>()
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

            val (complete, partial) = answered.partition { it.isComplete }
            partial.forEach { incomplete[it.index] = it }
            if (complete.isNotEmpty()) {
                val everyLine = everyLine(complete)
                songCache.saveAnnotations(track.trackId, annotator.providerName, everyLine)
                onBatch(everyLine)
                delivered = true
            }
            pending = asked.filter { line -> complete.none { it.index == line.index } }
            if (pending.isEmpty()) return Result.success(0)
        }

        val partial = pending.mapNotNull { incomplete[it.index] }
        if (partial.isNotEmpty()) {
            Logger.w("The model left out words or meanings in lines ${partial.map { it.index }} of ${track.title}")
            onBatch(everyLine(partial))
            delivered = true
        }
        val notComplete = pending.sumOf { groupOf[it.index]?.size ?: 1 }
        return if (delivered) Result.success(notComplete) else Result.failure(lastError ?: AnnotationException.Failed("The model answered no line"))
    }

    companion object {
        const val BATCH_SIZE = 6
        private const val MAX_PASSES = 2
        private const val RETRY_DELAY_MILLIS = 4_000L
    }
}
