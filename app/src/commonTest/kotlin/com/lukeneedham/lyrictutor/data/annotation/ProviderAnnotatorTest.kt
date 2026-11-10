package com.lukeneedham.lyrictutor.data.annotation

import com.lukeneedham.lyrictutor.data.settings.AiProvider
import com.lukeneedham.lyrictutor.data.settings.AiSettings
import com.lukeneedham.lyrictutor.data.settings.SettingsStore
import com.lukeneedham.lyrictutor.data.track.TrackInfo
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.test.runTest

class ProviderAnnotatorTest {

    private class FakeSettingsStore(var settings: AiSettings) : SettingsStore {
        override suspend fun load() = settings
        override suspend fun save(settings: AiSettings) { this.settings = settings }
    }

    private class FakeAnnotator(override val providerName: String) : LyricsAnnotator {
        var calls = 0
        override suspend fun annotate(song: TrackInfo, lines: List<AnnotationLine>): Result<List<LineAnnotation>> {
            calls++
            return Result.success(listOf(LineAnnotation(index = 0, translation = providerName, language = "Spanish")))
        }
    }

    private val track = TrackInfo("id", "Title", "Title", listOf("Artist"), null, null, null)
    private val lines = listOf(AnnotationLine(0, "hola"))

    @Test
    fun theProviderOfTheSettingsAnswersAndIsNamed() = runTest {
        val store = FakeSettingsStore(AiSettings(provider = AiProvider.DeepSeek))
        val gemini = FakeAnnotator("gemini")
        val deepSeek = FakeAnnotator("deepseek")
        val zai = FakeAnnotator("zai")
        val annotator = ProviderAnnotator(store, mapOf(AiProvider.Gemini to gemini, AiProvider.DeepSeek to deepSeek, AiProvider.ZAi to zai))

        assertEquals("deepseek", annotator.annotate(track, lines).getOrThrow().single().translation)
        assertEquals("deepseek", annotator.providerName)

        // A change in Settings applies to the next request
        store.settings = AiSettings(provider = AiProvider.Gemini)
        assertEquals("gemini", annotator.annotate(track, lines).getOrThrow().single().translation)
        assertEquals("gemini", annotator.providerName)
        store.settings = AiSettings(provider = AiProvider.ZAi)
        assertEquals("zai", annotator.annotate(track, lines).getOrThrow().single().translation)
        assertEquals("zai", annotator.providerName)
        assertEquals(1, gemini.calls)
        assertEquals(1, deepSeek.calls)
        assertEquals(1, zai.calls)
    }
}
