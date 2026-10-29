package com.lukeneedham.lyrictutor.data.track

import com.lukeneedham.common.Logger
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.isSuccess
import kotlinx.coroutines.CancellationException

/** Reads the public page of a track. Scraping, for personal use: it can break without notice, hence the [TrackResolver] interface. */
class SpotifyTrackResolver(private val client: HttpClient) : TrackResolver {

    override suspend fun resolve(trackId: String): Result<TrackInfo> {
        val html = try {
            val response = client.get("https://open.spotify.com/track/$trackId") {
                header(HttpHeaders.UserAgent, BROWSER_USER_AGENT)
                header(HttpHeaders.AcceptLanguage, "en")
            }
            if (!response.status.isSuccess()) {
                return Result.failure(TrackResolveException.Unreadable("The track page answered ${response.status}"))
            }
            response.bodyAsText()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Logger.e("Could not fetch the track page of $trackId", e)
            return Result.failure(TrackResolveException.Network(e))
        }
        return SpotifyPageParser.parse(trackId, html).onFailure { Logger.e("Could not read the track page of $trackId", it) }
    }

    private companion object {
        const val BROWSER_USER_AGENT =
            "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0 Mobile Safari/537.36"
    }
}
