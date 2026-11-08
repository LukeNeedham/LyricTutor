package com.lukeneedham.lyrictutor.data.annotation

import com.lukeneedham.common.logger.Logger
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess
import kotlinx.coroutines.CancellationException

class DeepSeekModelCatalog(private val client: HttpClient) : ModelCatalog {

    override suspend fun list(apiKey: String): Result<List<String>> {
        if (apiKey.isBlank()) return Result.failure(AnnotationException.MissingKey())
        return try {
            val response = client.get("${DeepSeekProtocol.BASE_URL}/models") {
                header(HttpHeaders.Authorization, "Bearer $apiKey")
            }
            val text = response.bodyAsText()
            when {
                response.status == HttpStatusCode.Unauthorized -> Result.failure(AnnotationException.InvalidKey())
                !response.status.isSuccess() -> {
                    Logger.e("Listing the models: DeepSeek answered ${response.status}: ${text.take(300)}")
                    Result.failure(AnnotationException.Failed("DeepSeek answered ${response.status.value}"))
                }
                else -> Result.success(DeepSeekProtocol.parseModels(text))
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Logger.e("Listing the models failed", e)
            Result.failure(AnnotationException.Failed("DeepSeek could not be reached", e))
        }
    }
}
