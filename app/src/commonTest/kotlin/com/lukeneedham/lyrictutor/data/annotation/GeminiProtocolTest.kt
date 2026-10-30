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
        val prompt = GeminiProtocol.prompt(track, listOf(AnnotationLine(0, "Si me voy"), AnnotationLine(7, "pa' ti")))
        assertTrue("Si Me Voy (with The Marías)" in prompt)
        assertTrue("Cuco, The Marías" in prompt)
        assertTrue("0: Si me voy" in prompt)
        assertTrue("7: pa' ti" in prompt)
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
        {"candidates":[{"content":{"parts":[{"text":"{\"lines\":[{\"index\":0,\"translation\":\"If I go\",\"words\":[
        {\"surface\":\"voy\",\"language\":\"es\",\"lemma\":\"ir\",\"partOfSpeech\":\"verb\",\"morphology\":\"present, 1st person singular\",
        \"meaning\":\"I go\",\"note\":null,\"cognates\":[\"evade\"]},
        {\"surface\":\"me\"}]}]}"}]},"finishReason":"STOP"}],"usageMetadata":{"totalTokenCount":10}}
    """.trimIndent().replace("\n", "")

    @Test
    fun readsTheAnnotationsOfAnAnswer() {
        val lines = GeminiProtocol.parseResponse(answer).getOrThrow()
        assertEquals(1, lines.size)
        assertEquals("If I go", lines[0].translation)
        val voy = lines[0].words[0]
        assertEquals("ir", voy.lemma)
        assertNull(voy.note)
        assertEquals(listOf("evade"), voy.cognates)
        // Only the surface is required
        assertEquals(WordAnnotation(surface = "me"), lines[0].words[1])
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
    fun describesTheTokensOfAnAnswer() {
        val usage = """{"candidates":[],"usageMetadata":{"promptTokenCount":812,"candidatesTokenCount":3401,"thoughtsTokenCount":150,"totalTokenCount":4363}}"""
        assertEquals("in 812, out 3401, thinking 150", GeminiProtocol.describeUsage(usage))
        assertEquals("in 5", GeminiProtocol.describeUsage("""{"usageMetadata":{"promptTokenCount":5}}"""))
        assertNull(GeminiProtocol.describeUsage("""{"candidates":[]}"""))
        assertNull(GeminiProtocol.describeUsage("not json"))
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
