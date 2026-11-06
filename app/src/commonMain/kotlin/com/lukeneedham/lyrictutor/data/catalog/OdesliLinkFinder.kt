package com.lukeneedham.lyrictutor.data.catalog

import com.lukeneedham.common.logger.Logger
import com.lukeneedham.lyrictutor.data.apiJson
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.Serializable

/** Odesli (song.link): given a link to a song on one service, the links to it on the others. Public, no key, rate limited. */
class OdesliLinkFinder(private val client: HttpClient) : StreamingLinkFinder {

    override suspend fun find(pageUrl: String): StreamingLinks? = try {
        val response = client.get("https://api.song.link/v1-alpha.1/links") {
            parameter("url", pageUrl)
        }
        if (!response.status.isSuccess()) {
            Logger.w("Odesli answered ${response.status} for $pageUrl")
            null
        } else {
            val platforms = apiJson.decodeFromString<OdesliAnswer>(response.bodyAsText()).linksByPlatform
            val links = StreamingLinks(spotifyUrl = platforms["spotify"]?.url, youtubeUrl = platforms["youtube"]?.url)
            links.takeIf { it.spotifyUrl != null || it.youtubeUrl != null }
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Logger.w("Odesli lookup failed for $pageUrl", e)
        null
    }

    @Serializable
    private class OdesliAnswer(val linksByPlatform: Map<String, OdesliLink> = emptyMap())

    @Serializable
    private class OdesliLink(val url: String? = null)
}
