package com.lukeneedham.lyrictutor.data.track

/** Sends a track id to the resolver of its source. A song typed in by name is always saved when it is opened, so it never gets here. */
class MultiSourceTrackResolver(private val spotify: TrackResolver, private val youtube: TrackResolver) : TrackResolver {
    override suspend fun resolve(trackId: String): Result<TrackInfo> = when {
        TrackIds.isYoutube(trackId) -> youtube.resolve(trackId)
        TrackIds.isByName(trackId) -> Result.failure(TrackResolveException.Unreadable("A song typed in by name is not on this device"))
        else -> spotify.resolve(trackId)
    }
}
