package com.lukeneedham.lyrictutor.data.annotation

import com.lukeneedham.lyrictutor.data.track.TrackInfo

/** One line of the lyrics to annotate, with its place in the song. */
data class AnnotationLine(val index: Int, val text: String)

/** Translates lines and explains their words. An interface, so that a provider other than Gemini can be added. */
interface LyricsAnnotator {
    /** Stored with the annotations, to tell where they came from. */
    val providerName: String

    /** Fails with an [AnnotationException]. */
    suspend fun annotate(song: TrackInfo, lines: List<AnnotationLine>): Result<List<LineAnnotation>>
}

sealed class AnnotationException(message: String, cause: Throwable? = null) : Exception(message, cause) {
    /** No API key has been entered in Settings. */
    class MissingKey : AnnotationException("No API key")

    /** The provider refused the key. */
    class InvalidKey : AnnotationException("The API key was refused")

    /** The free tier's limits were hit: trying again later works. */
    class RateLimited : AnnotationException("Rate limited")

    class Failed(message: String, cause: Throwable? = null) : AnnotationException(message, cause)
}
