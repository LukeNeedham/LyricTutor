package com.lukeneedham.lyrictutor.data.track

import com.lukeneedham.common.logger.Logger
import com.lukeneedham.lyrictutor.data.apiJson
import com.lukeneedham.lyrictutor.data.catalog.TrackExtrasFinder
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
class YoutubeTrackResolver(private val client: HttpClient, private val extrasFinder: TrackExtrasFinder) : TrackResolver {

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
        val artistName = artist.ifBlank { dto.authorName.orEmpty() }.ifBlank { "YouTube" }
        // The album art and the Spotify link when the song is found (the title and artist are guesses, so it has to match both);
        // else the frame YouTube shows
        val extras = extrasFinder.find(TitleCleaner.clean(title), artistName, durationSeconds = null)
        return Result.success(
            TrackInfo(
                trackId = trackId,
                title = title,
                searchTitle = TitleCleaner.clean(title),
                artists = listOf(artistName),
                coverUrl = extras.coverUrl ?: dto.thumbnailUrl,
                spotifyUrl = extras.spotifyUrl,
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
