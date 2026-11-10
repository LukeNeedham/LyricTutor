package com.lukeneedham.lyrictutor.di

import com.lukeneedham.lyrictutor.data.annotation.ChatCompletionsAnnotator
import com.lukeneedham.lyrictutor.data.annotation.ChatCompletionsModelCatalog
import com.lukeneedham.lyrictutor.data.annotation.ChatCompletionsPartOfSpeechChooser
import com.lukeneedham.lyrictutor.data.annotation.GeminiAnnotator
import com.lukeneedham.lyrictutor.data.annotation.GeminiModelCatalog
import com.lukeneedham.lyrictutor.data.annotation.GeminiPartOfSpeechChooser
import com.lukeneedham.lyrictutor.data.annotation.LyricsAnnotator
import com.lukeneedham.lyrictutor.data.annotation.ModelCatalogs
import com.lukeneedham.lyrictutor.data.annotation.PartOfSpeechChooser
import com.lukeneedham.lyrictutor.data.annotation.ProviderAnnotator
import com.lukeneedham.lyrictutor.data.annotation.ProviderPartOfSpeechChooser
import com.lukeneedham.lyrictutor.data.settings.AiProvider
import com.lukeneedham.lyrictutor.data.settings.SettingsStore
import io.ktor.client.HttpClient

/** Builds what depends on the chosen AI service, one for each [AiProvider], so that the Koin module only wires. */
object AiProviderFactory {

    fun annotator(client: HttpClient, settingsStore: SettingsStore): LyricsAnnotator = ProviderAnnotator(
        settingsStore = settingsStore,
        annotators = AiProvider.entries.associateWith { provider ->
            when (provider) {
                AiProvider.Gemini -> GeminiAnnotator(client = client, settingsStore = settingsStore)
                else -> ChatCompletionsAnnotator(provider = provider, client = client, settingsStore = settingsStore)
            }
        },
    )

    fun partOfSpeechChooser(client: HttpClient, settingsStore: SettingsStore): PartOfSpeechChooser = ProviderPartOfSpeechChooser(
        settingsStore = settingsStore,
        choosers = AiProvider.entries.associateWith { provider ->
            when (provider) {
                AiProvider.Gemini -> GeminiPartOfSpeechChooser(client = client, settingsStore = settingsStore)
                else -> ChatCompletionsPartOfSpeechChooser(provider = provider, client = client, settingsStore = settingsStore)
            }
        },
    )

    fun modelCatalogs(client: HttpClient): ModelCatalogs = ModelCatalogs(
        AiProvider.entries.associateWith { provider ->
            when (provider) {
                AiProvider.Gemini -> GeminiModelCatalog(client = client)
                else -> ChatCompletionsModelCatalog(provider = provider, client = client)
            }
        },
    )
}
