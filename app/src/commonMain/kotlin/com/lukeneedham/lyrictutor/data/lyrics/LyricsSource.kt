package com.lukeneedham.lyrictutor.data.lyrics

import com.lukeneedham.lyrictutor.data.track.TrackInfo

/** Finds the lyrics of a song on the network. An interface, so that a second provider can be added behind the same repository. */
interface LyricsSource {
    /** Success with null when the source does not have the song; failure when it could not be asked. */
    suspend fun find(track: TrackInfo): Result<Lyrics?>
}
