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
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlin.time.TimeSource
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject

/** Asks DeepSeek to choose between a few parts of speech: the answer is a word or two, matched against the options. */
class DeepSeekPartOfSpeechChooser(
    private val client: HttpClient,
    private val settingsStore: SettingsStore,
) : PartOfSpeechChooser {

    override suspend fun choose(line: String, word: String, language: String?, options: List<String>): Result<String?> {
        val settings = settingsStore.load()
        if (settings.deepSeekApiKey.isBlank()) return Result.failure(AnnotationException.MissingKey())

        val mark = TimeSource.Monotonic.markNow()
        val body = try {
            val response = client.post("${DeepSeekProtocol.BASE_URL}/chat/completions") {
                header(HttpHeaders.Authorization, "Bearer ${settings.deepSeekApiKey}")
                contentType(ContentType.Application.Json)
                timeout {
                    requestTimeoutMillis = REQUEST_TIMEOUT_MILLIS
                    socketTimeoutMillis = REQUEST_TIMEOUT_MILLIS
                }
                setBody(
                    Json.encodeToString(
                        JsonObject.serializer(),
                        DeepSeekProtocol.choiceRequestBody(line, word, language, options, settings.deepSeekModel),
                    ),
                )
            }
            val text = response.bodyAsText()
            if (!response.status.isSuccess()) {
                Logger.e("DeepSeek answered ${response.status} when choosing a part of speech: ${text.take(300)}")
                return Result.failure(
                    when (response.status) {
                        HttpStatusCode.TooManyRequests -> AnnotationException.RateLimited()
                        HttpStatusCode.Unauthorized -> AnnotationException.InvalidKey()
                        else -> AnnotationException.Failed("DeepSeek answered ${response.status.value}")
                    },
                )
            }
            Logger.i("${settings.deepSeekModel} chose a part of speech in ${mark.elapsedNow()}, tokens: ${DeepSeekProtocol.parseUsage(text) ?: "not reported"}")
            text
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Logger.e("Choosing a part of speech failed after ${mark.elapsedNow()}", e)
            return Result.failure(AnnotationException.Failed("DeepSeek could not be reached", e))
        }
        return Result.success(DeepSeekProtocol.parseChoice(body, options))
    }

    private companion object {
        const val REQUEST_TIMEOUT_MILLIS = 30_000L
    }
}
