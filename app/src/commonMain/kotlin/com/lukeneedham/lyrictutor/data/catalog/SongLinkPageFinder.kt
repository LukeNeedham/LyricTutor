package com.lukeneedham.lyrictutor.data.catalog

import com.lukeneedham.common.logger.Logger
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.isSuccess
import kotlinx.coroutines.CancellationException

/**
 * Given a link to a song on one service, the links to it on the others, read from its public song.link page (Odesli retired its API;
 * the pages are not part of that). Scraping: it can break without notice, hence the [StreamingLinkFinder] interface.
 */
class SongLinkPageFinder(private val client: HttpClient) : StreamingLinkFinder {

    override suspend fun find(pageUrl: String): StreamingLinks? = try {
        // song.link redirects this to the page of the song
        val response = client.get("https://song.link/$pageUrl") {
            header(HttpHeaders.UserAgent, BROWSER_USER_AGENT)
            header(HttpHeaders.AcceptLanguage, "en")
        }
        if (!response.status.isSuccess()) {
            Logger.w("song.link answered ${response.status} for $pageUrl")
            null
        } else {
            val html = response.bodyAsText()
            SongLinkPageParser.parse(html)
                ?: run {
                    Logger.w("song.link page for $pageUrl has no Spotify or YouTube link (${html.length} characters)")
                    null
                }
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Logger.w("song.link lookup failed for $pageUrl", e)
        null
    }

    private companion object {
        const val BROWSER_USER_AGENT =
            "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0 Mobile Safari/537.36"
    }
}
