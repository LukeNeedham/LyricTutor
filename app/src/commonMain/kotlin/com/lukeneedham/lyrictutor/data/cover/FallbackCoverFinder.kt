package com.lukeneedham.lyrictutor.data.cover

/** Asks each finder in turn and uses the first cover found. */
class FallbackCoverFinder(private val finders: List<CoverFinder>) : CoverFinder {
    override suspend fun find(title: String, artist: String, durationSeconds: Int?): String? {
        for (finder in finders) {
            finder.find(title, artist, durationSeconds)?.let { return it }
        }
        return null
    }
}
