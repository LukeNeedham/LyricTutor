package com.lukeneedham.lyrictutor.data.annotation

import com.lukeneedham.lyrictutor.data.apiJson
import com.lukeneedham.lyrictutor.data.track.TrackInfo
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject

/**
 * The request and the response of DeepSeek's chat completions (the OpenAI format), as pure functions so that they are tested without a
 * network. DeepSeek has no schema for its answer, only a JSON mode: the shape is described in the prompt.
 */
object DeepSeekProtocol {
    const val BASE_URL = "https://api.deepseek.com"

    // A whole song (up to 80 lines) is about 4,000 tokens of JSON; 8,192 is the largest answer every model accepts
    private const val MAX_OUTPUT_TOKENS = 8_192
    private const val CHOICE_MAX_OUTPUT_TOKENS = 32

    private val json = apiJson

    private const val FORMAT = """Answer with a single JSON object and nothing else, in this shape:
{"lines":[{"index":0,"translation":"...","language":"Spanish","otherLanguages":[{"word":"hey","language":"English"}]}]}"""

    fun requestBody(song: TrackInfo, lines: List<AnnotationLine>, model: String): JsonObject = buildJsonObject {
        put("model", model)
        putJsonArray("messages") {
            addJsonObject { put("role", "system"); put("content", FORMAT) }
            addJsonObject { put("role", "user"); put("content", GeminiProtocol.prompt(song, lines)) }
        }
        putJsonObject("response_format") { put("type", "json_object") }
        put("max_tokens", MAX_OUTPUT_TOKENS)
        // Translating needs no reasoning, which would only cost time and tokens
        putJsonObject("thinking") { put("type", "disabled") }
    }

    fun choiceRequestBody(line: String, word: String, language: String?, options: List<String>, model: String): JsonObject = buildJsonObject {
        put("model", model)
        putJsonArray("messages") {
            addJsonObject { put("role", "user"); put("content", GeminiProtocol.choicePrompt(line, word, language, options)) }
        }
        put("max_tokens", CHOICE_MAX_OUTPUT_TOKENS)
        putJsonObject("thinking") { put("type", "disabled") }
    }

    /** The text of the first choice of an answer, or null when there is none. */
    fun answerText(body: String): String? = try {
        json.decodeFromString(ChatResponseDto.serializer(), body).choices.orEmpty().firstOrNull()?.message?.content
    } catch (e: IllegalArgumentException) {
        null
    }

    /** The annotations in a chat completions answer. Fails with [AnnotationException.Failed] when there is no usable answer. */
    fun parseResponse(body: String): Result<List<LineAnnotation>> {
        val text = answerText(body)
        if (text.isNullOrBlank()) {
            val reason = try {
                json.decodeFromString(ChatResponseDto.serializer(), body).choices.orEmpty().firstOrNull()?.finishReason
            } catch (e: IllegalArgumentException) {
                null
            }
            return Result.failure(AnnotationException.Failed("No answer from DeepSeek (finish reason: ${reason ?: "none"})"))
        }
        return GeminiProtocol.parseLines(text, "DeepSeek")
    }

    /** The option an answer to [choiceRequestBody] names, or null when it names none. */
    fun parseChoice(body: String, options: List<String>): String? = GeminiProtocol.matchChoice(answerText(body), options)

    /** The tokens an answer used, or null when the answer does not say. */
    fun parseUsage(body: String): TokenUsage? = try {
        json.decodeFromString(ChatResponseDto.serializer(), body).usage?.let { usage ->
            val thinking = usage.details?.reasoningTokens ?: 0
            TokenUsage(input = usage.promptTokens ?: 0, output = (usage.completionTokens ?: 0) - thinking, thinking = thinking)
        }
    } catch (e: IllegalArgumentException) {
        null
    }

    /** The message of an error answer (`{"error":{"message":"..."}}`), or null when the body is not one. */
    fun errorMessage(body: String): String? = try {
        json.decodeFromString(ErrorResponseDto.serializer(), body).error?.message
    } catch (e: IllegalArgumentException) {
        null
    }

    /** The models of a `/models` answer, as the codes to put in the request. */
    fun parseModels(body: String): List<String> = try {
        json.decodeFromString(ModelListDto.serializer(), body).data.orEmpty().mapNotNull { it.id }.sorted()
    } catch (e: IllegalArgumentException) {
        emptyList()
    }

    @Serializable
    private data class ChatResponseDto(val choices: List<ChoiceDto>? = null, val usage: UsageDto? = null)

    @Serializable
    private data class ChoiceDto(val message: MessageDto? = null, @SerialName("finish_reason") val finishReason: String? = null)

    @Serializable
    private data class MessageDto(val content: String? = null)

    @Serializable
    private data class UsageDto(
        @SerialName("prompt_tokens") val promptTokens: Int? = null,
        @SerialName("completion_tokens") val completionTokens: Int? = null,
        @SerialName("completion_tokens_details") val details: UsageDetailsDto? = null,
    )

    @Serializable
    private data class UsageDetailsDto(@SerialName("reasoning_tokens") val reasoningTokens: Int? = null)

    @Serializable
    private data class ErrorResponseDto(val error: ErrorBodyDto? = null)

    @Serializable
    private data class ErrorBodyDto(val message: String? = null)

    @Serializable
    private data class ModelListDto(val data: List<ModelDto>? = null)

    @Serializable
    private data class ModelDto(val id: String? = null)
}
