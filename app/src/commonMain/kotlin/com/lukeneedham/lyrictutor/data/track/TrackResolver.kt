package com.lukeneedham.lyrictutor.data.track

/** Turns a Spotify track id into its [TrackInfo]. An interface, because the source (a public web page) can change or break. */
interface TrackResolver {
    /** Fails with a [TrackResolveException]. */
    suspend fun resolve(trackId: String): Result<TrackInfo>
}

sealed class TrackResolveException(message: String, cause: Throwable? = null) : Exception(message, cause) {
    /** The page could not be fetched: no connection, or Spotify refused. */
    class Network(cause: Throwable) : TrackResolveException("Could not fetch the track page", cause)

    /** The page came back, but without the tags the app reads. */
    class Unreadable(message: String) : TrackResolveException(message)
}
