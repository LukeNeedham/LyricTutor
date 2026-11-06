package com.lukeneedham.lyrictutor.data.catalog

import com.lukeneedham.common.logger.Logger
import com.lukeneedham.lyrictutor.data.apiJson
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** The Deezer API (public, no key): album art, and the song's page on Deezer. */
class DeezerCatalog(private val client: HttpClient) : SongCatalog {

    override suspend fun find(title: String, artist: String, durationSeconds: Int?): CatalogMatch? = try {
        val response = client.get("https://api.deezer.com/search") {
            parameter("q", "$title $artist")
            parameter("limit", 10)
        }
        if (!response.status.isSuccess()) {
            Logger.w("Deezer search answered ${response.status}")
            null
        } else {
            apiJson.decodeFromString<DeezerAnswer>(response.bodyAsText()).data
                .firstOrNull { SongMatcher.isMatch(title, artist, durationSeconds, it.title, it.artist?.name, it.duration) }
                ?.let { CatalogMatch(coverUrl = it.album?.coverXl?.takeIf { cover -> cover.isNotBlank() }, pageUrl = it.link) }
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Logger.w("Deezer cover search failed for $title", e)
        null
    }

    @Serializable
    private class DeezerAnswer(val data: List<DeezerTrack> = emptyList())

    @Serializable
    private class DeezerTrack(
        val title: String? = null,
        val link: String? = null,
        val duration: Int? = null,
        val artist: DeezerArtist? = null,
        val album: DeezerAlbum? = null,
    )

    @Serializable
    private class DeezerArtist(val name: String? = null)

    @Serializable
    private class DeezerAlbum(@SerialName("cover_xl") val coverXl: String? = null)
}
