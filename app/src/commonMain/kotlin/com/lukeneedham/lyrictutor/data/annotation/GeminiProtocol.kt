package com.lukeneedham.lyrictutor.data.annotation

import com.lukeneedham.lyrictutor.data.apiJson
import com.lukeneedham.lyrictutor.data.track.TrackInfo
import kotlinx.serialization.SerializationException
import kotlinx.serialization.Serializable
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
    private const val CHOICE_MAX_OUTPUT_TOKENS = 512

    private val json = apiJson

    fun prompt(song: TrackInfo, lines: List<AnnotationLine>): String = buildString {
        appendLine("You are a language tutor. The user is learning from the lyrics of the song \"${song.title}\" by ${song.artists.joinToString(", ")}.")
        appendLine("The numbered lines below are the lyrics of the song in order: use the whole song as context. For each line, give its English translation and its language.")
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

    /** The question of which part of speech [word] is used as in [line]. */
    fun choicePrompt(line: String, word: String, language: String?, options: List<String>): String = buildString {
        appendLine("In this line of a song${language?.takeIf { it.isNotBlank() }?.let { " (in $it)" } ?: ""}, which part of speech is the word \"$word\" used as?")
        appendLine("Line: $line")
        appendLine("Options: ${options.joinToString(", ")}")
        append("Answer with exactly one of the options.")
    }

    /** A request whose answer can only be one of [options]: Gemini's enum output, a string held to a fixed list. */
    fun choiceRequestBody(line: String, word: String, language: String?, options: List<String>, model: String = ""): JsonObject = buildJsonObject {
        putJsonArray("contents") {
            addJsonObject {
                putJsonArray("parts") { addJsonObject { put("text", choicePrompt(line, word, language, options)) } }
            }
        }
        putJsonObject("generationConfig") {
            put("responseMimeType", "text/x.enum")
            putJsonObject("responseSchema") {
                put("type", "STRING")
                putJsonArray("enum") { options.forEach { add(JsonPrimitive(it)) } }
            }
            // A few tokens are enough for the answer; the room left is for the thinking that counts against the cap
            put("maxOutputTokens", CHOICE_MAX_OUTPUT_TOKENS)
            if (model.startsWith("gemini-3")) {
                putJsonObject("thinkingConfig") { put("thinkingLevel", "low") }
            }
        }
        putJsonArray("safetySettings") {
            listOf("HARASSMENT", "HATE_SPEECH", "SEXUALLY_EXPLICIT", "DANGEROUS_CONTENT").forEach { category ->
                addJsonObject {
                    put("category", "HARM_CATEGORY_$category")
                    put("threshold", "BLOCK_NONE")
                }
            }
        }
    }

    /** The option an answer to [choiceRequestBody] names, or null when it names none (compared without case or quotes). */
    fun parseChoice(body: String, options: List<String>): String? = try {
        val text = json.decodeFromString(GeminiResponseDto.serializer(), body).candidates.orEmpty().firstOrNull()
            ?.content?.parts?.mapNotNull { it.text }?.joinToString("")?.trim()?.trim('"', '\'')?.trim()
        options.firstOrNull { it.equals(text, ignoreCase = true) }
    } catch (e: IllegalArgumentException) {
        null
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

    /** The message of an error answer (`{"error":{"message":"..."}}`), or null when the body is not one. */
    fun errorMessage(body: String): String? = try {
        json.decodeFromString(GeminiErrorResponseDto.serializer(), body).error?.message
    } catch (e: IllegalArgumentException) {
        null
    }

    /** Whether the body of a 429 answer says that a limit per day was hit (it names the quota, for example "...PerDay...limit exceeded"). */
    fun isDailyQuota(body: String): Boolean = body.contains("PerDay", ignoreCase = true) || body.contains("per day", ignoreCase = true)

    /** The tokens an answer used, or null when the answer does not say. */
    fun parseUsage(body: String): TokenUsage? = try {
        json.decodeFromString(GeminiResponseDto.serializer(), body).usageMetadata?.let { usage ->
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
        json.decodeFromString(GeminiModelListDto.serializer(), body).models.orEmpty()
            .filter { "generateContent" in it.supportedGenerationMethods.orEmpty() }
            .mapNotNull { it.name?.removePrefix("models/") }
            .filter { it.startsWith("gemini") }
            .sorted()
    } catch (e: IllegalArgumentException) {
        emptyList()
    }

    /** The annotations in a `generateContent` response body. Fails with [AnnotationException.Failed] when there is no usable answer. */
    fun parseResponse(body: String): Result<List<LineAnnotation>> = try {
        val response = json.decodeFromString(GeminiResponseDto.serializer(), body)
        val candidate = response.candidates.orEmpty().firstOrNull()
        val text = candidate?.content?.parts?.mapNotNull { it.text }?.joinToString("")
        if (text.isNullOrBlank()) {
            Result.failure(AnnotationException.Failed("No answer from Gemini (finish reason: ${candidate?.finishReason ?: "none"})"))
        } else {
            Result.success(json.decodeFromString(GeminiBatchDto.serializer(), text).lines.orEmpty().mapNotNull { it.toDomain() })
        }
    } catch (e: SerializationException) {
        Result.failure(AnnotationException.Failed("Gemini's answer could not be read", e))
    } catch (e: IllegalArgumentException) {
        Result.failure(AnnotationException.Failed("Gemini's answer could not be read", e))
    }

    @Serializable
    private data class GeminiModelListDto(val models: List<GeminiModelDto>? = null)

    @Serializable
    private data class GeminiModelDto(val name: String? = null, val supportedGenerationMethods: List<String>? = null)

    @Serializable
    private data class GeminiErrorResponseDto(val error: GeminiErrorBodyDto? = null)

    @Serializable
    private data class GeminiErrorBodyDto(val message: String? = null)

    @Serializable
    private data class GeminiResponseDto(val candidates: List<GeminiCandidateDto>? = null, val usageMetadata: GeminiUsageDto? = null)

    @Serializable
    private data class GeminiUsageDto(val promptTokenCount: Int? = null, val candidatesTokenCount: Int? = null, val thoughtsTokenCount: Int? = null)

    @Serializable
    private data class GeminiCandidateDto(val content: GeminiContentDto? = null, val finishReason: String? = null)

    @Serializable
    private data class GeminiContentDto(val parts: List<GeminiPartDto>? = null)

    @Serializable
    private data class GeminiPartDto(val text: String? = null)

    @Serializable
    private data class GeminiBatchDto(val lines: List<LineAnnotationDto>? = null)

    /** A line as the model writes it: the model can leave any field out. */
    @Serializable
    private data class LineAnnotationDto(
        val index: Int? = null,
        val translation: String? = null,
        val language: String? = null,
        val otherLanguages: List<WordLanguageDto>? = null,
    )

    @Serializable
    private data class WordLanguageDto(val word: String? = null, val language: String? = null)

    /** Null when the model gave no index: the line cannot be matched with a line of the song. */
    private fun LineAnnotationDto.toDomain(): LineAnnotation? {
        val index = index ?: return null
        return LineAnnotation(
            index = index,
            translation = translation,
            language = language,
            otherLanguages = otherLanguages.orEmpty().mapNotNull { it.toDomain() },
        )
    }

    private fun WordLanguageDto.toDomain(): WordLanguage? {
        val word = word ?: return null
        val language = language ?: return null
        return WordLanguage(word, language)
    }
}
