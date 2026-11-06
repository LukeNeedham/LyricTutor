package com.lukeneedham.lyrictutor.data.cover

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

/** Album art from the Deezer API (public, no key). */
class DeezerCoverFinder(private val client: HttpClient) : CoverFinder {

    override suspend fun find(title: String, artist: String, durationSeconds: Int?): String? = try {
        val response = client.get("https://api.deezer.com/search") {
            parameter("q", "$title $artist")
            parameter("limit", 10)
        }
        if (!response.status.isSuccess()) {
            Logger.w("Deezer search answered ${response.status}")
            null
        } else {
            apiJson.decodeFromString<DeezerAnswer>(response.bodyAsText()).data
                .firstOrNull { CoverMatcher.isMatch(title, artist, durationSeconds, it.title, it.artist?.name, it.duration) }
                ?.album?.coverXl?.takeIf { it.isNotBlank() }
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
        val duration: Int? = null,
        val artist: DeezerArtist? = null,
        val album: DeezerAlbum? = null,
    )

    @Serializable
    private class DeezerArtist(val name: String? = null)

    @Serializable
    private class DeezerAlbum(@SerialName("cover_xl") val coverXl: String? = null)
}
