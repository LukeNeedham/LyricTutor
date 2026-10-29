package com.lukeneedham.lyrictutor.data.lyrics

import com.lukeneedham.common.logger.Logger
import com.lukeneedham.lyrictutor.data.track.TrackInfo
import io.ktor.client.HttpClient
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Looks lyrics up on LRCLIB (crowd-sourced, no key). It rate-limits and dislikes parallel requests, so requests go one at a time.
 * Order: exact lookup with the first artist, search with the first artist, then both again with the whole artist string.
 */
class LrclibLyricsSource(private val client: HttpClient) : LyricsSource {

    private val oneAtATime = Mutex()

    override suspend fun find(track: TrackInfo): Result<Lyrics?> = try {
        Result.success(lookUp(track))
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Logger.e("LRCLIB lookup failed for ${track.title}", e)
        Result.failure(e)
    }

    private suspend fun lookUp(track: TrackInfo): Lyrics? {
        val artistNames = listOf(track.artists.first(), track.artists.joinToString(", ")).distinct()
        for (artist in artistNames) {
            exactLookup(track, artist)?.let { return it }
            search(track, artist)?.let { return it }
        }
        return null
    }

    private suspend fun exactLookup(track: TrackInfo, artist: String): Lyrics? {
        val response = request("get") {
            parameter("track_name", track.searchTitle)
            parameter("artist_name", artist)
            track.durationSeconds?.let { parameter("duration", it) }
        }
        if (response.status == HttpStatusCode.NotFound) return null
        if (!response.status.isSuccess()) error("LRCLIB answered ${response.status}")
        return LrclibTrack.parseOne(response.bodyAsText()).toLyrics()
    }

    private suspend fun search(track: TrackInfo, artist: String): Lyrics? {
        val response = request("search") {
            parameter("track_name", track.searchTitle)
            parameter("artist_name", artist)
        }
        if (response.status == HttpStatusCode.NotFound) return null
        if (!response.status.isSuccess()) error("LRCLIB answered ${response.status}")
        val results = LrclibTrack.parseList(response.bodyAsText())
        return LrclibTrack.pickBest(results, track.durationSeconds)?.toLyrics()
    }

    private suspend fun request(path: String, parameters: HttpRequestBuilder.() -> Unit): HttpResponse =
        oneAtATime.withLock {
            client.get("https://lrclib.net/api/$path") {
                header(HttpHeaders.UserAgent, USER_AGENT)
                parameters()
            }
        }

    private companion object {
        const val USER_AGENT = "LyricTutor (https://github.com/LukeNeedham/LyricTutor)"
    }
}
