package com.lukeneedham.lyrictutor.data.catalog

import kotlin.math.abs

/** Decides whether a result of a cover provider is the song that was asked for. Pure, so it is tested without a network. */
object SongMatcher {
    /** A result further than this from the song's duration is taken to be another recording. */
    const val MAX_DURATION_DIFFERENCE_SECONDS = 8

    fun isMatch(
        title: String,
        artist: String,
        durationSeconds: Int?,
        candidateTitle: String?,
        candidateArtist: String?,
        candidateDurationSeconds: Int?,
    ): Boolean {
        if (candidateTitle == null || candidateArtist == null) return false
        if (!overlaps(normalize(title), normalize(candidateTitle))) return false
        if (!overlaps(normalize(firstArtist(artist)), normalize(candidateArtist))) return false
        if (durationSeconds != null && candidateDurationSeconds != null) {
            return abs(durationSeconds - candidateDurationSeconds) <= MAX_DURATION_DIFFERENCE_SECONDS
        }
        return true
    }

    /** One contains the other: a provider adds "(feat. x)" or "- Remastered" to a name, or joins artists with "&". */
    private fun overlaps(a: String, b: String) = a.isNotEmpty() && b.isNotEmpty() && (a.contains(b) || b.contains(a))

    private fun firstArtist(artists: String) = artists.split(',', '&', ';').first()

    /** Lower case, only letters and digits, so punctuation and spacing never make two spellings differ. */
    private fun normalize(text: String) = text.lowercase().filter { it.isLetterOrDigit() }
}
