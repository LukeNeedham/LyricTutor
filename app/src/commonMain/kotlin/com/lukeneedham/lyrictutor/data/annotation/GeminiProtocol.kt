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
        appendLine("For each numbered lyric line below, give an English translation and an explanation of each word of the line.")
        appendLine()
        appendLine("Rules:")
        appendLine("- Detect the language of each word yourself. Songs mix languages, so never assume one language for the whole song.")
        appendLine("- Give every word of the line, in order, as written in the line (\"surface\"), one entry per word. Leave out nothing but punctuation. Never repeat a word's entry.")
        appendLine("- Always fill \"language\", \"lemma\", \"partOfSpeech\" and \"meaning\" of every word, also for little words like \"het\", \"en\" or \"me\".")
        appendLine("- Every text is very short. Never list alternatives, never repeat yourself, never write more than the limit below.")
        appendLine("- \"translation\": the natural English meaning of the whole line.")
        appendLine("- \"language\": the English name of the language of this word, for example \"Spanish\" or \"Dutch\".")
        appendLine("- \"lemma\": the dictionary form, one word. \"partOfSpeech\": one word, like noun, verb, adjective, article or pronoun.")
        appendLine("- \"morphology\": at most 8 words saying how this one word is formed, for example \"present indicative, 1st person singular\" for a verb or \"feminine plural\" for a noun. Use null only for a word that is never inflected, like an adverb or a preposition.")
        appendLine("- \"meaning\": at most 5 English words: what the word means in this line.")
        appendLine("- \"note\": at most 15 words on slang, an idiom or an elision (for example \"pa'\" is \"para\"). Use null when there is nothing to say.")
        appendLine("- \"cognates\": at most 3 English words with the same root, to help remember the word. Use an empty list when there are none, and give only cognates you are sure of.")
        appendLine("- Answer for every line, with the same \"index\" as the line.")
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
        fun string(nullable: Boolean = false) = buildJsonObject {
            put("type", "STRING")
            if (nullable) put("nullable", true)
        }

        val word = buildJsonObject {
            put("type", "OBJECT")
            putJsonObject("properties") {
                put("surface", string())
                put("language", string())
                put("lemma", string())
                put("partOfSpeech", string())
                put("morphology", string(nullable = true))
                put("meaning", string())
                put("note", string(nullable = true))
                putJsonObject("cognates") {
                    put("type", "ARRAY")
                    put("items", string())
                }
            }
            // Everything but the note is required: left optional, a smaller model leaves most of it out
            put(
                "required",
                buildJsonArray {
                    listOf("surface", "language", "lemma", "partOfSpeech", "morphology", "meaning", "cognates").forEach { add(JsonPrimitive(it)) }
                },
            )
        }
        val line = buildJsonObject {
            put("type", "OBJECT")
            putJsonObject("properties") {
                putJsonObject("index") { put("type", "INTEGER") }
                put("translation", string())
                putJsonObject("words") {
                    put("type", "ARRAY")
                    put("items", word)
                }
            }
            put("required", buildJsonArray { add(JsonPrimitive("index")); add(JsonPrimitive("translation")); add(JsonPrimitive("words")) })
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

    /** The message of an error answer (`{"error":{"message":"..."}}`), or null when the body is not one. */
    fun errorMessage(body: String): String? = try {
        json.decodeFromString(ErrorResponse.serializer(), body).error?.message
    } catch (e: IllegalArgumentException) {
        null
    }

    /** Whether the body of a 429 answer says that a limit per day was hit (it names the quota, for example "...PerDay...limit exceeded"). */
    fun isDailyQuota(body: String): Boolean = body.contains("PerDay", ignoreCase = true) || body.contains("per day", ignoreCase = true)

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
