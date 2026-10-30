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
        \"meaning\":\"I go\",\"note\":null,\"etymology\":\"From Latin vadere\",\"cognates\":[\"evade\"]},
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
