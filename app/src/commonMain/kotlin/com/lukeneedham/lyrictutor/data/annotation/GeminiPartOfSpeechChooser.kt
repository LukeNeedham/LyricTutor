package com.lukeneedham.lyrictutor.data.annotation

import com.lukeneedham.common.logger.Logger
import com.lukeneedham.lyrictutor.data.settings.SettingsStore
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
import kotlin.time.TimeSource
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject

/** Asks Gemini to choose between a few parts of speech: its answer is held to the options, so it is a word or two of tokens. */
class GeminiPartOfSpeechChooser(
    private val client: HttpClient,
    private val settingsStore: SettingsStore,
) : PartOfSpeechChooser {

    override suspend fun choose(line: String, word: String, language: String?, options: List<String>): Result<String?> {
        val settings = settingsStore.load()
        if (settings.apiKey.isBlank()) return Result.failure(AnnotationException.MissingKey())

        val mark = TimeSource.Monotonic.markNow()
        val text = try {
            val response = client.post("https://generativelanguage.googleapis.com/v1beta/models/${settings.model}:generateContent") {
                header("x-goog-api-key", settings.apiKey)
                contentType(ContentType.Application.Json)
                timeout {
                    requestTimeoutMillis = REQUEST_TIMEOUT_MILLIS
                    socketTimeoutMillis = REQUEST_TIMEOUT_MILLIS
                }
                setBody(Json.encodeToString(JsonObject.serializer(), GeminiProtocol.choiceRequestBody(line, word, language, options, settings.model)))
            }
            val body = response.bodyAsText()
            if (!response.status.isSuccess()) {
                Logger.e("Gemini answered ${response.status} when choosing a part of speech: ${body.take(300)}")
                return Result.failure(
                    when (response.status) {
                        HttpStatusCode.TooManyRequests -> AnnotationException.RateLimited(isDaily = GeminiProtocol.isDailyQuota(body))
                        HttpStatusCode.Unauthorized, HttpStatusCode.Forbidden -> AnnotationException.InvalidKey()
                        else -> AnnotationException.Failed("Gemini answered ${response.status.value}")
                    },
                )
            }
            Logger.i("${settings.model} chose a part of speech in ${mark.elapsedNow()}, tokens: ${GeminiProtocol.parseUsage(body) ?: "not reported"}")
            body
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Logger.e("Choosing a part of speech failed after ${mark.elapsedNow()}", e)
            return Result.failure(AnnotationException.Failed("Gemini could not be reached", e))
        }
        return Result.success(GeminiProtocol.parseChoice(text, options))
    }

    private companion object {
        const val REQUEST_TIMEOUT_MILLIS = 30_000L
    }
}
