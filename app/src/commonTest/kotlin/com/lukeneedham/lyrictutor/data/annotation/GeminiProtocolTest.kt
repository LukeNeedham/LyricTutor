package com.lukeneedham.lyrictutor.data.annotation

import com.lukeneedham.lyrictutor.data.track.TrackInfo
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GeminiProtocolTest {

    private val track = TrackInfo(
        trackId = "id",
        title = "Si Me Voy (with The Marías)",
        searchTitle = "Si Me Voy",
        artists = listOf("Cuco", "The Marías"),
        coverUrl = null,
        durationSeconds = 203,
        releaseDate = null,
    )

    @Test
    fun thePromptHasTheSongAndTheNumberedLines() {
        val prompt = GeminiProtocol.prompt(track, listOf(AnnotationLine(0, "primera linea"), AnnotationLine(7, "otra linea")))
        assertTrue("Si Me Voy (with The Marías)" in prompt)
        assertTrue("Cuco, The Marías" in prompt)
        assertTrue("0: primera linea" in prompt)
        assertTrue("7: otra linea" in prompt)
    }

    @Test
    fun theRequestAsksForJsonWithASchema() {
        val body = GeminiProtocol.requestBody(track, listOf(AnnotationLine(0, "hola"))).toString()
        assertTrue("\"responseMimeType\":\"application/json\"" in body)
        assertTrue("responseSchema" in body)
        assertTrue("0: hola" in body)
    }

    @Test
    fun gemini3ModelsAreAskedToThinkLittle() {
        val gemini3 = GeminiProtocol.requestBody(track, listOf(AnnotationLine(0, "hola")), model = "gemini-3.8-flash").toString()
        val older = GeminiProtocol.requestBody(track, listOf(AnnotationLine(0, "hola")), model = "gemini-2.5-flash").toString()
        assertTrue("\"thinkingLevel\":\"low\"" in gemini3)
        assertTrue("thinkingLevel" !in older)
    }

    // The text of the first candidate holds the JSON that the schema asked for
    private val answer = """
        {"candidates":[{"content":{"parts":[{"text":"{\"lines\":[{\"index\":0,\"translation\":\"One two\",\"language\":\"Spanish\",
        \"otherLanguages\":[{\"word\":\"hey\",\"language\":\"English\"}]},{\"index\":1,\"translation\":\"Three\"}]}"}]},
        "finishReason":"STOP"}],"usageMetadata":{"totalTokenCount":10}}
    """.trimIndent().replace("\n", "")

    @Test
    fun readsTheAnnotationsOfAnAnswer() {
        val lines = GeminiProtocol.parseResponse(answer).getOrThrow()
        assertEquals(2, lines.size)
        assertEquals("One two", lines[0].translation)
        assertEquals("Spanish", lines[0].language)
        assertEquals(listOf(WordLanguage("hey", "English")), lines[0].otherLanguages)
        assertTrue(lines[0].isComplete)
        // The language and the other languages may be missing: the line is then not complete
        assertEquals(LineAnnotation(index = 1, translation = "Three"), lines[1])
        assertTrue(!lines[1].isComplete)
    }

    @Test
    fun theLanguageOfAWordIsThatOfTheLineUnlessAnotherWasGiven() {
        val line = LineAnnotation(0, "x", "Spanish", listOf(WordLanguage("Hey,", "English")))
        assertEquals("English", line.languageOf("hey"))
        assertEquals("Spanish", line.languageOf("dos"))
        assertNull(LineAnnotation(0, "x").languageOf("dos"))
    }

    @Test
    fun theRequestAsksForNoWordByWord() {
        val prompt = GeminiProtocol.prompt(track, listOf(AnnotationLine(0, "hola")))
        assertTrue("lemma" !in prompt && "etymology" !in prompt && "cognates" !in prompt)
        val schema = GeminiProtocol.requestBody(track, listOf(AnnotationLine(0, "hola"))).toString()
        assertTrue("otherLanguages" in schema)
        assertTrue("partOfSpeech" !in schema)
    }

    @Test
    fun tellsADailyQuotaFromAPerMinuteOne() {
        val daily = """{"error":{"code":429,"message":"Quota exceeded for metric: generate_content_free_tier_requests, Generate content requests per day limit exceeded"}}"""
        val perMinute = """{"error":{"code":429,"message":"Quota exceeded for metric: GenerateRequestsPerMinutePerProjectPerModel-FreeTier"}}"""
        assertTrue(GeminiProtocol.isDailyQuota(daily))
        assertTrue(!GeminiProtocol.isDailyQuota(perMinute))
        assertEquals("Quota exceeded", GeminiProtocol.errorMessage("""{"error":{"message":"Quota exceeded"}}"""))
    }

    @Test
    fun readsTheTokensOfAnAnswer() {
        val usage = """{"candidates":[],"usageMetadata":{"promptTokenCount":812,"candidatesTokenCount":3401,"thoughtsTokenCount":150,"totalTokenCount":4363}}"""
        assertEquals(TokenUsage(input = 812, output = 3401, thinking = 150), GeminiProtocol.parseUsage(usage))
        assertEquals(TokenUsage(input = 5), GeminiProtocol.parseUsage("""{"usageMetadata":{"promptTokenCount":5}}"""))
        assertNull(GeminiProtocol.parseUsage("""{"candidates":[]}"""))
        assertNull(GeminiProtocol.parseUsage("not json"))
    }

    @Test
    fun addsTokensAndWritesThemForTheLog() {
        val total = TokenUsage(1, 2, 3) + TokenUsage(10, 20, 30)
        assertEquals(TokenUsage(11, 22, 33), total)
        assertEquals("in 11, out 22, thinking 33", total.toString())
    }

    @Test
    fun listsTheModelsThatCanAnswerAPrompt() {
        val body = """
            {"models":[
            {"name":"models/gemini-3.8-flash","supportedGenerationMethods":["generateContent","countTokens"]},
            {"name":"models/gemini-2.5-flash-lite","supportedGenerationMethods":["generateContent"]},
            {"name":"models/gemini-embedding-001","supportedGenerationMethods":["embedContent"]},
            {"name":"models/imagen-4.0","supportedGenerationMethods":["predict"]},
            {"name":"models/gemma-3-27b","supportedGenerationMethods":["generateContent"]}]}
        """.trimIndent().replace("\n", "")
        assertEquals(listOf("gemini-2.5-flash-lite", "gemini-3.8-flash"), GeminiProtocol.parseModels(body))
        assertEquals(emptyList(), GeminiProtocol.parseModels("not json"))
    }

    @Test
    fun theChoiceOfAPartOfSpeechIsAskedAsAnEnum() {
        val options = listOf("Noun", "Verb")
        val body = GeminiProtocol.choiceRequestBody("een regel", "dingen", "Dutch", options, model = "gemini-3.8-flash").toString()
        assertTrue("\"responseMimeType\":\"text/x.enum\"" in body)
        assertTrue("\"enum\":[\"Noun\",\"Verb\"]" in body)
        assertTrue("een regel" in body && "dingen" in body && "Dutch" in body)
        // The question is a pick from a list: the least thinking there is. The translations keep "low"
        assertTrue("\"thinkingLevel\":\"minimal\"" in body)
        assertTrue("thinkingLevel" !in GeminiProtocol.choiceRequestBody("x", "y", null, options, model = "gemini-2.5-flash").toString())
    }

    @Test
    fun readsTheOptionAnAnswerNames() {
        val options = listOf("Noun", "Verb")
        fun answer(text: String) = """{"candidates":[{"content":{"parts":[{"text":"$text"}]}}]}"""
        assertEquals("Noun", GeminiProtocol.parseChoice(answer("Noun"), options))
        assertEquals("Verb", GeminiProtocol.parseChoice(answer(" verb\\n"), options))
        assertEquals("Noun", GeminiProtocol.parseChoice(answer("\\\"Noun\\\""), options))
        assertNull(GeminiProtocol.parseChoice(answer("Adjective"), options))
        assertNull(GeminiProtocol.parseChoice("""{"candidates":[]}""", options))
        assertNull(GeminiProtocol.parseChoice("not json", options))
    }

    @Test
    fun anAnswerWithoutTextFails() {
        val result = GeminiProtocol.parseResponse("""{"candidates":[{"finishReason":"SAFETY"}]}""")
        assertIs<AnnotationException.Failed>(result.exceptionOrNull())
    }

    @Test
    fun anAnswerThatIsNotJsonFails() {
        val result = GeminiProtocol.parseResponse("""{"candidates":[{"content":{"parts":[{"text":"sorry, I cannot"}]}}]}""")
        assertIs<AnnotationException.Failed>(result.exceptionOrNull())
    }
}
