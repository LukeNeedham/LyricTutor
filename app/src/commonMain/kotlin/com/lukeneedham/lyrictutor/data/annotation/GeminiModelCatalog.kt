package com.lukeneedham.lyrictutor.data.annotation

import com.lukeneedham.common.logger.Logger
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess
import kotlinx.coroutines.CancellationException

class GeminiModelCatalog(private val client: HttpClient) : ModelCatalog {

    override suspend fun list(apiKey: String): Result<List<String>> {
        if (apiKey.isBlank()) return Result.failure(AnnotationException.MissingKey())
        return try {
            val response = client.get("https://generativelanguage.googleapis.com/v1beta/models") {
                header("x-goog-api-key", apiKey)
                parameter("pageSize", 1000)
            }
            val text = response.bodyAsText()
            when {
                response.status == HttpStatusCode.Unauthorized || response.status == HttpStatusCode.Forbidden ||
                    response.status == HttpStatusCode.BadRequest -> Result.failure(AnnotationException.InvalidKey())
                !response.status.isSuccess() -> {
                    Logger.e("Listing the models: Gemini answered ${response.status}: ${text.take(300)}")
                    Result.failure(AnnotationException.Failed("Gemini answered ${response.status.value}"))
                }
                else -> Result.success(GeminiProtocol.parseModels(text))
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Logger.e("Listing the models failed", e)
            Result.failure(AnnotationException.Failed("Gemini could not be reached", e))
        }
    }
}
