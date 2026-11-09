package com.lukeneedham.lyrictutor.data.annotation

import com.lukeneedham.common.logger.Logger
import com.lukeneedham.lyrictutor.data.settings.AiProvider
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

/** Asks a chat completions API (DeepSeek, Z.AI) to choose between a few parts of speech: the answer is a word or two, matched against the options. */
class ChatCompletionsPartOfSpeechChooser(
    private val provider: AiProvider,
    private val client: HttpClient,
    private val settingsStore: SettingsStore,
) : PartOfSpeechChooser {

    private val baseUrl = requireNotNull(provider.chatBaseUrl) { "${provider.displayName} has no chat completions API" }

    override suspend fun choose(line: String, word: String, language: String?, options: List<String>): Result<String?> {
        val settings = settingsStore.load().of(provider)
        if (settings.apiKey.isBlank()) return Result.failure(AnnotationException.MissingKey())

        val mark = TimeSource.Monotonic.markNow()
        val body = try {
            val response = client.post("$baseUrl/chat/completions") {
                header(HttpHeaders.Authorization, "Bearer ${settings.apiKey}")
                contentType(ContentType.Application.Json)
                timeout {
                    requestTimeoutMillis = REQUEST_TIMEOUT_MILLIS
                    socketTimeoutMillis = REQUEST_TIMEOUT_MILLIS
                }
                setBody(
                    Json.encodeToString(
                        JsonObject.serializer(),
                        ChatCompletionsProtocol.choiceRequestBody(line, word, language, options, settings.model, provider.reasoning),
                    ),
                )
            }
            val text = response.bodyAsText()
            if (!response.status.isSuccess()) {
                Logger.e("${provider.displayName} answered ${response.status} when choosing a part of speech: ${text.take(300)}")
                return Result.failure(
                    when (response.status) {
                        HttpStatusCode.TooManyRequests -> AnnotationException.RateLimited()
                        HttpStatusCode.Unauthorized -> AnnotationException.InvalidKey()
                        else -> AnnotationException.Failed("${provider.displayName} answered ${response.status.value}")
                    },
                )
            }
            Logger.i("${settings.model} chose a part of speech in ${mark.elapsedNow()}, tokens: ${ChatCompletionsProtocol.parseUsage(text) ?: "not reported"}")
            text
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Logger.e("Choosing a part of speech failed after ${mark.elapsedNow()}", e)
            return Result.failure(AnnotationException.Failed("${provider.displayName} could not be reached", e))
        }
        return Result.success(ChatCompletionsProtocol.parseChoice(body, options))
    }

    private companion object {
        const val REQUEST_TIMEOUT_MILLIS = 30_000L
    }
}
