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

/** The iTunes Search API (public, no key): album art, and the song's page in Apple Music. */
class ItunesCatalog(private val client: HttpClient) : SongCatalog {

    override suspend fun find(title: String, artist: String, durationSeconds: Int?): CatalogMatch? = try {
        val response = client.get("https://itunes.apple.com/search") {
            parameter("term", "$title $artist")
            parameter("entity", "song")
            parameter("limit", 10)
        }
        if (!response.status.isSuccess()) {
            Logger.w("iTunes search answered ${response.status}")
            null
        } else {
            apiJson.decodeFromString<ItunesAnswer>(response.bodyAsText()).results
                .firstOrNull {
                    SongMatcher.isMatch(title, artist, durationSeconds, it.trackName, it.artistName, it.trackTimeMillis?.div(1000)?.toInt())
                }
                ?.let { CatalogMatch(coverUrl = it.artworkUrl100?.let(::larger), pageUrl = it.trackViewUrl) }
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Logger.w("iTunes cover search failed for $title", e)
        null
    }

    @Serializable
    private class ItunesAnswer(val results: List<ItunesTrack> = emptyList())

    @Serializable
    private class ItunesTrack(
        val trackName: String? = null,
        val artistName: String? = null,
        val trackTimeMillis: Long? = null,
        val artworkUrl100: String? = null,
        val trackViewUrl: String? = null,
    )

    companion object {
        private val size = Regex("/\\d+x\\d+(bb)?\\.")

        /** iTunes serves the same image at any size: ask for 600 pixels instead of 100. */
        fun larger(artworkUrl: String): String = artworkUrl.replace(size, "/600x600bb.")
    }
}
