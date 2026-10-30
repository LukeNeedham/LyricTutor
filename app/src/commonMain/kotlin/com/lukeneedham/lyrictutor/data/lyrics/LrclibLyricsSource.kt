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
import kotlinx.coroutines.delay
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
        // A step that fails (LRCLIB answers 503 now and then) does not stop the next one; only when nothing was found is the failure reported,
        // because "not found" after a failed step would be a lie
        var failure: Exception? = null
        for (artist in artistNames) {
            for (step in listOf(::exactLookup, ::search)) {
                try {
                    step(track, artist)?.let { return it }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Logger.w("LRCLIB lookup step failed for ${track.title}", e)
                    failure = e
                }
            }
        }
        failure?.let { throw it }
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

    /** One request at a time. A timeout, a dropped connection, a 429 or a 5xx is tried again a couple of times: LRCLIB is often slow on a cold lookup. */
    private suspend fun request(path: String, parameters: HttpRequestBuilder.() -> Unit): HttpResponse {
        var attempt = 1
        while (true) {
            try {
                val response = oneAtATime.withLock {
                    client.get("https://lrclib.net/api/$path") {
                        header(HttpHeaders.UserAgent, USER_AGENT)
                        parameters()
                    }
                }
                if (!response.status.isTransientFailure() || attempt >= MAX_ATTEMPTS) return response
                Logger.w("LRCLIB request /$path answered ${response.status} (attempt $attempt)")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (attempt >= MAX_ATTEMPTS) throw e
                Logger.w("LRCLIB request /$path failed (attempt $attempt)", e)
            }
            delay(RETRY_DELAY_MILLIS * attempt)
            attempt++
        }
    }

    private fun HttpStatusCode.isTransientFailure() = this == HttpStatusCode.TooManyRequests || value >= 500

    private companion object {
        const val MAX_ATTEMPTS = 3
        const val RETRY_DELAY_MILLIS = 1_000L
        const val USER_AGENT = "LyricTutor (https://github.com/LukeNeedham/LyricTutor)"
    }
}
