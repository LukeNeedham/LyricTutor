package com.lukeneedham.lyrictutor.data.track

import io.ktor.http.encodeURLParameter

/** The link that plays a song on a service. Pure, so it is tested without a network. */
object TrackLinks {
    /**
     * The song's own link on [provider]: the one found for it, or the one its id gives. Without either, a search for it on that service,
     * so that every service can always be chosen.
     */
    fun playUrl(track: TrackInfo, provider: PlayProvider): String = when (provider) {
        PlayProvider.Spotify -> track.spotifyUrl
            ?: SpotifyLinkParser.trackUrl(track.trackId).takeIf { TrackIds.isSpotify(track.trackId) }
            ?: "https://open.spotify.com/search/${searchText(track).encodeURLParameter()}"

        PlayProvider.YouTube -> track.youtubeUrl
            ?: YoutubeLinkParser.videoUrl(track.trackId.removePrefix("yt:")).takeIf { TrackIds.isYoutube(track.trackId) }
            ?: "https://www.youtube.com/results?search_query=${searchText(track).encodeURLParameter()}"
    }

    private fun searchText(track: TrackInfo) = "${track.searchTitle} ${track.artists.first()}"
}
