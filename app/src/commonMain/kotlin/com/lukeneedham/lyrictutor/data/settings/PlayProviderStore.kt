package com.lukeneedham.lyrictutor.data.settings

import com.lukeneedham.lyrictutor.data.track.PlayProvider
import kotlinx.coroutines.flow.Flow

/** The service the play button of every song page uses. Implemented in `androidMain`. */
interface PlayProviderStore {
    /** The chosen service; [PlayProvider.Spotify] until the user chooses. Every change is sent to every collector, so all the pages agree. */
    val provider: Flow<PlayProvider>

    suspend fun set(provider: PlayProvider)
}
