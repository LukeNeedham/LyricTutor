package com.lukeneedham.lyrictutor.data.track

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

/**
 * Reads a YouTube video through its public oEmbed answer (no key): the title and the channel name. The song and artist are guessed from
 * them by [YoutubeTitleParser], so the result can be wrong for a video that is not a plain music upload.
 */
class YoutubeTrackResolver(private val client: HttpClient) : TrackResolver {

    override suspend fun resolve(trackId: String): Result<TrackInfo> {
        val videoId = trackId.removePrefix("yt:")
        val body = try {
            val response = client.get("https://www.youtube.com/oembed") {
                parameter("url", YoutubeLinkParser.videoUrl(videoId))
                parameter("format", "json")
            }
            if (!response.status.isSuccess()) {
                return Result.failure(TrackResolveException.Unreadable("YouTube answered ${response.status}"))
            }
            response.bodyAsText()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Logger.e("Could not fetch the YouTube video $videoId", e)
            return Result.failure(TrackResolveException.Network(e))
        }
        val dto = try {
            apiJson.decodeFromString<OEmbedDto>(body)
        } catch (e: Exception) {
            Logger.e("Could not read the YouTube answer for $videoId", e)
            return Result.failure(TrackResolveException.Unreadable("The YouTube answer could not be read"))
        }
        val (title, artist) = YoutubeTitleParser.parse(dto.title.orEmpty(), dto.authorName)
            ?: return Result.failure(TrackResolveException.Unreadable("The video has no title"))
        return Result.success(
            TrackInfo(
                trackId = trackId,
                title = title,
                searchTitle = TitleCleaner.clean(title),
                artists = listOf(artist.ifBlank { dto.authorName.orEmpty() }.ifBlank { "YouTube" }),
                coverUrl = dto.thumbnailUrl,
                durationSeconds = null,
                releaseDate = null,
            ),
        )
    }

    @Serializable
    private class OEmbedDto(
        val title: String? = null,
        @SerialName("author_name") val authorName: String? = null,
        @SerialName("thumbnail_url") val thumbnailUrl: String? = null,
    )
}
