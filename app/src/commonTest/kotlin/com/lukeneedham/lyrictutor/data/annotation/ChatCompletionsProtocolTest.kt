package com.lukeneedham.lyrictutor.data.annotation

import com.lukeneedham.lyrictutor.data.settings.AiProvider
import com.lukeneedham.lyrictutor.data.track.TrackInfo
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ChatCompletionsProtocolTest {

    private val track = TrackInfo(
        trackId = "id",
        title = "Si Me Voy",
        searchTitle = "Si Me Voy",
        artists = listOf("Cuco"),
        coverUrl = null,
        durationSeconds = 203,
        releaseDate = null,
    )

    private val answer = """
        {"choices":[{"message":{"role":"assistant","content":"{\"lines\":[{\"index\":0,\"translation\":\"One two\",\"language\":\"Spanish\",
        \"otherLanguages\":[{\"word\":\"hey\",\"language\":\"English\"}]},{\"index\":1,\"translation\":\"Three\"}]}"},"finish_reason":"stop"}],
        "usage":{"prompt_tokens":900,"completion_tokens":300,"completion_tokens_details":{"reasoning_tokens":50}}}
    """.trimIndent().replace("\n", "")

    @Test
    fun theRequestAsksForJsonModeWithoutThinking() {
        val body = ChatCompletionsProtocol.requestBody(track, listOf(AnnotationLine(0, "hola"), AnnotationLine(4, "adios")), "deepseek-v4-flash").toString()
        assertTrue("\"model\":\"deepseek-v4-flash\"" in body)
        assertTrue("\"response_format\":{\"type\":\"json_object\"}" in body)
        assertTrue("\"thinking\":{\"type\":\"disabled\"}" in body)
        assertTrue("JSON" in body)
        assertTrue("0: hola" in body)
        assertTrue("4: adios" in body)
    }

    @Test
    fun readsTheAnnotationsOfAnAnswer() {
        val lines = ChatCompletionsProtocol.parseResponse(answer, "DeepSeek").getOrThrow()
        assertEquals(2, lines.size)
        assertEquals("One two", lines[0].translation)
        assertEquals("Spanish", lines[0].language)
        assertEquals(listOf(WordLanguage("hey", "English")), lines[0].otherLanguages)
        assertNull(lines[1].language)
    }

    @Test
    fun readsAnAnswerInAMarkdownFence() {
        val fenced = """{"choices":[{"message":{"content":"```json\n{\"lines\":[{\"index\":2,\"translation\":\"x\",\"language\":\"Dutch\"}]}\n```"}}]}"""
        assertEquals(2, ChatCompletionsProtocol.parseResponse(fenced, "DeepSeek").getOrThrow().single().index)
    }

    @Test
    fun anAnswerWithoutTextOrWithBrokenJsonFails() {
        assertIs<AnnotationException.Failed>(ChatCompletionsProtocol.parseResponse("""{"choices":[{"message":{"content":""},"finish_reason":"length"}]}""", "DeepSeek").exceptionOrNull())
        assertIs<AnnotationException.Failed>(ChatCompletionsProtocol.parseResponse("""{"choices":[{"message":{"content":"{\"lines\":[{\"index\""}}]}""", "DeepSeek").exceptionOrNull())
        assertIs<AnnotationException.Failed>(ChatCompletionsProtocol.parseResponse("not json", "DeepSeek").exceptionOrNull())
    }

    @Test
    fun readsTheTokensWithTheReasoningApart() {
        assertEquals(TokenUsage(input = 900, output = 250, thinking = 50), ChatCompletionsProtocol.parseUsage(answer))
        assertNull(ChatCompletionsProtocol.parseUsage("""{"choices":[]}"""))
    }

    @Test
    fun readsAnErrorMessage() {
        assertEquals("Insufficient Balance", ChatCompletionsProtocol.errorMessage("""{"error":{"message":"Insufficient Balance","type":"unknown_error"}}"""))
        assertNull(ChatCompletionsProtocol.errorMessage("oops"))
    }

    @Test
    fun readsTheModelList() {
        assertEquals(
            listOf("deepseek-v4-flash", "deepseek-v4-pro"),
            ChatCompletionsProtocol.parseModels("""{"object":"list","data":[{"id":"deepseek-v4-pro","object":"model"},{"id":"deepseek-v4-flash","object":"model"}]}"""),
        )
        assertEquals(emptyList(), ChatCompletionsProtocol.parseModels("oops"))
    }

    @Test
    fun theChoiceIsMatchedWithTheOptions() {
        val options = listOf("Noun", "Verb")
        assertEquals("Verb", ChatCompletionsProtocol.parseChoice("""{"choices":[{"message":{"content":" \"verb\" "}}]}""", options))
        assertNull(ChatCompletionsProtocol.parseChoice("""{"choices":[{"message":{"content":"Adjective"}}]}""", options))
        assertNull(ChatCompletionsProtocol.parseChoice("oops", options))
    }

    @Test
    fun aFailureNamesTheProvider() {
        val message = ChatCompletionsProtocol.parseResponse("""{"choices":[{"message":{"content":""}}]}""", "Z.AI").exceptionOrNull()?.message.orEmpty()
        assertTrue("Z.AI" in message)
    }

    @Test
    fun everyProviderButGeminiHasAChatApi() {
        AiProvider.entries.filter { it != AiProvider.Gemini }.forEach { assertTrue(it.chatBaseUrl?.startsWith("https://") == true) }
    }
}
