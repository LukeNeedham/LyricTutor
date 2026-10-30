package com.lukeneedham.lyrictutor.data.art

/** Reads the colours of an album cover. The pixels of an image can only be read on the platform, so it is implemented there. */
interface ArtColorExtractor {
    /** The colours of the image at [coverUrl], or null when it could not be loaded. Never throws. */
    suspend fun extract(coverUrl: String): ArtColors?
}
