package com.lukeneedham.lyrictutor.data.annotation

import com.lukeneedham.common.logger.Logger
import com.lukeneedham.lyrictutor.data.settings.AiProvider
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess
import kotlinx.coroutines.CancellationException

/** The models of an OpenAI-style API (`GET <base>/models`), for DeepSeek and Z.AI. */
class ChatCompletionsModelCatalog(private val provider: AiProvider, private val client: HttpClient) : ModelCatalog {

    private val baseUrl = requireNotNull(provider.chatBaseUrl) { "${provider.displayName} has no chat completions API" }

    override suspend fun list(apiKey: String): Result<List<String>> {
        if (apiKey.isBlank()) return Result.failure(AnnotationException.MissingKey())
        return try {
            val response = client.get("$baseUrl/models") {
                header(HttpHeaders.Authorization, "Bearer $apiKey")
            }
            val text = response.bodyAsText()
            when {
                response.status == HttpStatusCode.Unauthorized -> Result.failure(AnnotationException.InvalidKey())
                !response.status.isSuccess() -> {
                    Logger.e("Listing the models: ${provider.displayName} answered ${response.status}: ${text.take(300)}")
                    Result.failure(AnnotationException.Failed("${provider.displayName} answered ${response.status.value}"))
                }
                else -> Result.success(ChatCompletionsProtocol.parseModels(text))
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Logger.e("Listing the models failed", e)
            Result.failure(AnnotationException.Failed("${provider.displayName} could not be reached", e))
        }
    }
}
