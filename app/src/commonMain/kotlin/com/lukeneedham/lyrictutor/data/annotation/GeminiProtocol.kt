package com.lukeneedham.lyrictutor.data.annotation

import com.lukeneedham.lyrictutor.data.track.TrackInfo
import kotlinx.serialization.SerializationException
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject

/** The request and the response of Gemini's `generateContent`, as pure functions so that they are tested without a network. */
object GeminiProtocol {
    private const val MAX_OUTPUT_TOKENS = 16_384

    private val json = Json { ignoreUnknownKeys = true; isLenient = true; coerceInputValues = true }

    fun prompt(song: TrackInfo, lines: List<AnnotationLine>): String = buildString {
        appendLine("You are a language tutor. The user is learning from the lyrics of the song \"${song.title}\" by ${song.artists.joinToString(", ")}.")
        appendLine("For each numbered lyric line below, give its English translation and its language.")
        appendLine()
        appendLine("Rules:")
        appendLine("- \"translation\": the natural English meaning of the whole line. If the line is already English, repeat it.")
        appendLine("- \"language\": the English name of the language of the line, for example \"Spanish\" or \"Dutch\". Songs mix languages, so decide for each line.")
        appendLine("- \"otherLanguages\": the words of the line that are in another language than the line, each with its language, for example a word of English in a Spanish line. Use an empty list when the whole line is in one language.")
        appendLine("- Answer for every line, with the same \"index\" as the line, and never explain anything else.")
        appendLine()
        appendLine("Lines:")
        lines.forEach { appendLine("${it.index}: ${it.text}") }
    }

    fun requestBody(song: TrackInfo, lines: List<AnnotationLine>, model: String = ""): JsonObject = buildJsonObject {
        putJsonArray("contents") {
            addJsonObject {
                putJsonArray("parts") { addJsonObject { put("text", prompt(song, lines)) } }
            }
        }
        putJsonObject("generationConfig") {
            put("responseMimeType", "application/json")
            // No temperature: Gemini 3 is meant to run at its default, and a lower one made it loop on a word's fields
            // A cap on the answer, so a runaway answer ends instead of taking minutes
            put("maxOutputTokens", MAX_OUTPUT_TOKENS)
            put("responseSchema", responseSchema())
            // Gemini 3 models think before they answer, which makes a batch take minutes; the task needs little thinking
            if (model.startsWith("gemini-3")) {
                putJsonObject("thinkingConfig") { put("thinkingLevel", "low") }
            }
        }
        // Lyrics can be crude, and a blocked answer helps nobody
        putJsonArray("safetySettings") {
            listOf("HARASSMENT", "HATE_SPEECH", "SEXUALLY_EXPLICIT", "DANGEROUS_CONTENT").forEach { category ->
                addJsonObject {
                    put("category", "HARM_CATEGORY_$category")
                    put("threshold", "BLOCK_NONE")
                }
            }
        }
    }

    private fun responseSchema(): JsonElement {
        fun string() = buildJsonObject { put("type", "STRING") }

        val otherLanguage = buildJsonObject {
            put("type", "OBJECT")
            putJsonObject("properties") {
                put("word", string())
                put("language", string())
            }
            put("required", buildJsonArray { add(JsonPrimitive("word")); add(JsonPrimitive("language")) })
        }
        val line = buildJsonObject {
            put("type", "OBJECT")
            putJsonObject("properties") {
                putJsonObject("index") { put("type", "INTEGER") }
                put("translation", string())
                put("language", string())
                putJsonObject("otherLanguages") {
                    put("type", "ARRAY")
                    put("items", otherLanguage)
                }
            }
            put(
                "required",
                buildJsonArray { listOf("index", "translation", "language", "otherLanguages").forEach { add(JsonPrimitive(it)) } },
            )
        }
        return buildJsonObject {
            put("type", "OBJECT")
            putJsonObject("properties") {
                putJsonObject("lines") {
                    put("type", "ARRAY")
                    put("items", line)
                }
            }
            put("required", JsonArray(listOf(JsonPrimitive("lines"))))
        }
    }

    /** The tokens an answer used, or null when the answer does not say. */
    fun parseUsage(body: String): TokenUsage? = try {
        json.decodeFromString(Response.serializer(), body).usageMetadata?.let { usage ->
            TokenUsage(
                input = usage.promptTokenCount ?: 0,
                output = usage.candidatesTokenCount ?: 0,
                thinking = usage.thoughtsTokenCount ?: 0,
            )
        }
    } catch (e: IllegalArgumentException) {
        null
    }

    /** The models of a `models` list answer that can answer a prompt (`generateContent`), as the codes to put in the request. */
    fun parseModels(body: String): List<String> = try {
        json.decodeFromString(ModelList.serializer(), body).models
            .filter { "generateContent" in it.supportedGenerationMethods }
            .map { it.name.removePrefix("models/") }
            .filter { it.startsWith("gemini") }
            .sorted()
    } catch (e: IllegalArgumentException) {
        emptyList()
    }

    /** The annotations in a `generateContent` response body. Fails with [AnnotationException.Failed] when there is no usable answer. */
    fun parseResponse(body: String): Result<List<LineAnnotation>> = try {
        val response = json.decodeFromString(Response.serializer(), body)
        val candidate = response.candidates.firstOrNull()
        val text = candidate?.content?.parts?.mapNotNull { it.text }?.joinToString("")
        if (text.isNullOrBlank()) {
            Result.failure(AnnotationException.Failed("No answer from Gemini (finish reason: ${candidate?.finishReason ?: "none"})"))
        } else {
            Result.success(json.decodeFromString(Batch.serializer(), text).lines)
        }
    } catch (e: SerializationException) {
        Result.failure(AnnotationException.Failed("Gemini's answer could not be read", e))
    } catch (e: IllegalArgumentException) {
        Result.failure(AnnotationException.Failed("Gemini's answer could not be read", e))
    }

    @Serializable
    private data class ModelList(val models: List<ModelDto> = emptyList())

    @Serializable
    private data class ModelDto(val name: String = "", val supportedGenerationMethods: List<String> = emptyList())

    @Serializable
    private data class ErrorResponse(val error: ErrorBody? = null)

    @Serializable
    private data class ErrorBody(val message: String? = null)

    @Serializable
    private data class Response(val candidates: List<Candidate> = emptyList(), val usageMetadata: Usage? = null)

    @Serializable
    private data class Usage(val promptTokenCount: Int? = null, val candidatesTokenCount: Int? = null, val thoughtsTokenCount: Int? = null)

    @Serializable
    private data class Candidate(val content: Content? = null, val finishReason: String? = null)

    @Serializable
    private data class Content(val parts: List<Part> = emptyList())

    @Serializable
    private data class Part(val text: String? = null)

    @Serializable
    private data class Batch(val lines: List<LineAnnotation> = emptyList())
}
