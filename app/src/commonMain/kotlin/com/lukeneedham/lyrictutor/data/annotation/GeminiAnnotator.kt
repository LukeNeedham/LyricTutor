package com.lukeneedham.lyrictutor.data.annotation

import com.lukeneedham.common.logger.Logger
import com.lukeneedham.lyrictutor.data.settings.SettingsStore
import com.lukeneedham.lyrictutor.data.track.TrackInfo
import io.ktor.client.HttpClient
import io.ktor.client.plugins.timeout
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.coroutines.CancellationException
import kotlin.time.TimeSource
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject

/** Annotates with the Gemini API, using the user's own key from Settings (it is never in the app). */
class GeminiAnnotator(
    private val client: HttpClient,
    private val settingsStore: SettingsStore,
) : LyricsAnnotator {

    override val providerName = "gemini"

    override suspend fun annotate(song: TrackInfo, lines: List<AnnotationLine>): Result<List<LineAnnotation>> {
        val settings = settingsStore.load()
        if (settings.apiKey.isBlank()) return Result.failure(AnnotationException.MissingKey())

        val mark = TimeSource.Monotonic.markNow()
        Logger.i("Asking ${settings.model} for lines ${lines.first().index}-${lines.last().index} of ${song.title}")
        val body = try {
            val response = client.post("https://generativelanguage.googleapis.com/v1beta/models/${settings.model}:generateContent") {
                header("x-goog-api-key", settings.apiKey)
                contentType(ContentType.Application.Json)
                timeout {
                    requestTimeoutMillis = REQUEST_TIMEOUT_MILLIS
                    // The wait for the first byte of the answer: the default of the OkHttp engine is ten seconds, too short for a batch
                    socketTimeoutMillis = REQUEST_TIMEOUT_MILLIS
                }
                setBody(Json.encodeToString(JsonObject.serializer(), GeminiProtocol.requestBody(song, lines, settings.model)))
            }
            val text = response.bodyAsText()
            if (!response.status.isSuccess()) {
                val reason = GeminiProtocol.errorMessage(text)
                Logger.e("Gemini answered ${response.status} for model ${settings.model}: ${text.take(500)}")
                return Result.failure(
                    when {
                        response.status == HttpStatusCode.TooManyRequests -> AnnotationException.RateLimited(isDaily = GeminiProtocol.isDailyQuota(text))
                        response.status == HttpStatusCode.Unauthorized || response.status == HttpStatusCode.Forbidden ||
                            response.status == HttpStatusCode.BadRequest && reason?.contains("API key", ignoreCase = true) == true ->
                            AnnotationException.InvalidKey()
                        else -> AnnotationException.Failed("Gemini answered ${response.status.value}: ${reason ?: text.take(200)}")
                    },
                )
            }
            Logger.i("${settings.model} answered in ${mark.elapsedNow()}")
            text
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Logger.e("Gemini request failed after ${mark.elapsedNow()}", e)
            return Result.failure(AnnotationException.Failed("Gemini could not be reached: ${e.message ?: e::class.simpleName}", e))
        }
        return GeminiProtocol.parseResponse(body).onFailure { Logger.e("Gemini answer unusable", it) }
    }

    private companion object {
        const val REQUEST_TIMEOUT_MILLIS = 120_000L
    }
}
