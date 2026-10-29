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
    private val json = Json { ignoreUnknownKeys = true; isLenient = true; coerceInputValues = true }

    fun prompt(song: TrackInfo, lines: List<AnnotationLine>): String = buildString {
        appendLine("You are a language tutor. The user is learning from the lyrics of the song \"${song.title}\" by ${song.artists.joinToString(", ")}.")
        appendLine("For each numbered lyric line below, give an English translation and an explanation of each word of the line.")
        appendLine()
        appendLine("Rules:")
        appendLine("- Detect the language of each word yourself. Songs mix languages, so never assume one language for the whole song.")
        appendLine("- Give every word of the line, in order, as written in the line (\"surface\"). Leave out nothing but punctuation.")
        appendLine("- \"lemma\" is the dictionary form. \"partOfSpeech\" is a plain word like noun, verb or adjective.")
        appendLine("- \"morphology\" says how the word is formed: for a verb, tense, mood, person and number; for a noun or adjective, gender and number. Use null when there is nothing to say.")
        appendLine("- \"meaning\" is what the word means in this line, in a few English words.")
        appendLine("- \"note\" explains slang, idioms and elisions (for example \"pa'\" is \"para\"). Use null when there is nothing to say.")
        appendLine("- \"etymology\" is one short sentence on where the word comes from. If you are not sure, say \"unknown\". Never invent an etymology.")
        appendLine("- \"cognates\" are English words with the same root, to help remember the word. Use an empty list when there are none.")
        appendLine("- Keep every text short.")
        appendLine("- Answer for every line, with the same \"index\" as the line.")
        appendLine()
        appendLine("Lines:")
        lines.forEach { appendLine("${it.index}: ${it.text}") }
    }

    fun requestBody(song: TrackInfo, lines: List<AnnotationLine>): JsonObject = buildJsonObject {
        putJsonArray("contents") {
            addJsonObject {
                putJsonArray("parts") { addJsonObject { put("text", prompt(song, lines)) } }
            }
        }
        putJsonObject("generationConfig") {
            put("responseMimeType", "application/json")
            put("temperature", 0.2)
            put("responseSchema", responseSchema())
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
                put("language", string(nullable = true))
                put("lemma", string(nullable = true))
                put("partOfSpeech", string(nullable = true))
                put("morphology", string(nullable = true))
                put("meaning", string(nullable = true))
                put("note", string(nullable = true))
                put("etymology", string(nullable = true))
                putJsonObject("cognates") {
                    put("type", "ARRAY")
                    put("items", string())
                }
            }
            put("required", buildJsonArray { add(JsonPrimitive("surface")) })
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
    private data class ErrorResponse(val error: ErrorBody? = null)

    @Serializable
    private data class ErrorBody(val message: String? = null)

    @Serializable
    private data class Response(val candidates: List<Candidate> = emptyList())

    @Serializable
    private data class Candidate(val content: Content? = null, val finishReason: String? = null)

    @Serializable
    private data class Content(val parts: List<Part> = emptyList())

    @Serializable
    private data class Part(val text: String? = null)

    @Serializable
    private data class Batch(val lines: List<LineAnnotation> = emptyList())
}
