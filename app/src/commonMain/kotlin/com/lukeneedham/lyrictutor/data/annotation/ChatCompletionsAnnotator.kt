package com.lukeneedham.lyrictutor.data.annotation

import com.lukeneedham.common.logger.Logger
import com.lukeneedham.lyrictutor.data.settings.AiProvider
import com.lukeneedham.lyrictutor.data.settings.SettingsStore
import com.lukeneedham.lyrictutor.data.track.TrackInfo
import io.ktor.client.HttpClient
import io.ktor.client.plugins.timeout
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlin.time.TimeSource
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject

/** Annotates with an OpenAI-style chat completions API (DeepSeek, Z.AI), using the user's own key from Settings (it is never in the app). */
class ChatCompletionsAnnotator(
    private val provider: AiProvider,
    private val client: HttpClient,
    private val settingsStore: SettingsStore,
) : LyricsAnnotator {

    override val providerName = provider.name.lowercase()

    /** The tokens used for each song since the app started, for the log. */
    private val totals = mutableMapOf<String, TokenUsage>()

    override suspend fun annotate(song: TrackInfo, lines: List<AnnotationLine>): Result<List<LineAnnotation>> {
        val settings = settingsStore.load().of(provider)
        if (settings.apiKey.isBlank()) return Result.failure(AnnotationException.MissingKey())

        val mark = TimeSource.Monotonic.markNow()
        val url = ChatCompletionsProtocol.url(provider, settings.useCodingPlan)
        Logger.i("Asking ${settings.model} at $url for lines ${lines.first().index}-${lines.last().index} of ${song.title}")
        val body = try {
            val response = client.post("$url/chat/completions") {
                header(HttpHeaders.Authorization, "Bearer ${settings.apiKey}")
                contentType(ContentType.Application.Json)
                timeout {
                    requestTimeoutMillis = REQUEST_TIMEOUT_MILLIS
                    // The wait for the first byte of the answer: the default of the OkHttp engine is ten seconds, too short for a batch
                    socketTimeoutMillis = REQUEST_TIMEOUT_MILLIS
                }
                setBody(Json.encodeToString(JsonObject.serializer(), ChatCompletionsProtocol.requestBody(song, lines, settings.model, provider.reasoning)))
            }
            val text = response.bodyAsText()
            if (!response.status.isSuccess()) {
                val reason = ChatCompletionsProtocol.errorMessage(text)
                Logger.e("${provider.displayName} answered ${response.status} at $url for model ${settings.model}: ${text.take(500)}")
                return Result.failure(
                    when (response.status) {
                        HttpStatusCode.TooManyRequests -> AnnotationException.RateLimited()
                        HttpStatusCode.Unauthorized -> AnnotationException.InvalidKey()
                        // 402: the account has no balance left
                        else -> AnnotationException.Failed("${provider.displayName} answered ${response.status.value}: ${reason ?: text.take(200)}")
                    },
                )
            }
            val usage = ChatCompletionsProtocol.parseUsage(text)
            val songTotal = usage?.let { ((totals[song.trackId] ?: TokenUsage()) + it).also { total -> totals[song.trackId] = total } }
            Logger.i(
                "${settings.model} answered in ${mark.elapsedNow()}, tokens: ${usage ?: "not reported"}" +
                    (songTotal?.let { "; ${song.title} so far: $it" } ?: ""),
            )
            text
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Logger.e("${provider.displayName} request failed after ${mark.elapsedNow()}", e)
            return Result.failure(AnnotationException.Failed("${provider.displayName} could not be reached: ${e.message ?: e::class.simpleName}", e))
        }
        return ChatCompletionsProtocol.parseResponse(body, provider.displayName).onFailure { Logger.e("${provider.displayName} answer unusable", it) }
    }

    private companion object {
        const val REQUEST_TIMEOUT_MILLIS = 120_000L
    }
}
