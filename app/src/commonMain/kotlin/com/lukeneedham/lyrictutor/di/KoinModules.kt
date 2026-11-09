package com.lukeneedham.lyrictutor.di

import com.lukeneedham.lyrictutor.data.annotation.AnnotationRepository
import com.lukeneedham.lyrictutor.data.annotation.ChatCompletionsAnnotator
import com.lukeneedham.lyrictutor.data.annotation.ChatCompletionsModelCatalog
import com.lukeneedham.lyrictutor.data.annotation.ChatCompletionsPartOfSpeechChooser
import com.lukeneedham.lyrictutor.data.annotation.GeminiAnnotator
import com.lukeneedham.lyrictutor.data.annotation.GeminiModelCatalog
import com.lukeneedham.lyrictutor.data.annotation.GeminiPartOfSpeechChooser
import com.lukeneedham.lyrictutor.data.annotation.LyricsAnnotator
import com.lukeneedham.lyrictutor.data.annotation.PartOfSpeechChooser
import com.lukeneedham.lyrictutor.data.annotation.ProviderAnnotator
import com.lukeneedham.lyrictutor.data.annotation.ProviderPartOfSpeechChooser
import com.lukeneedham.lyrictutor.data.catalog.SongCatalog
import com.lukeneedham.lyrictutor.data.catalog.DeezerCatalog
import com.lukeneedham.lyrictutor.data.catalog.FallbackCatalog
import com.lukeneedham.lyrictutor.data.catalog.ItunesCatalog
import com.lukeneedham.lyrictutor.data.settings.AiProvider
import com.lukeneedham.lyrictutor.data.lyrics.LrclibLyricsSource
import com.lukeneedham.lyrictutor.data.lyrics.LyricsRepository
import com.lukeneedham.lyrictutor.data.lyrics.LyricsSource
import com.lukeneedham.lyrictutor.data.track.MultiSourceTrackResolver
import com.lukeneedham.lyrictutor.data.track.SpotifyTrackResolver
import com.lukeneedham.lyrictutor.data.track.YoutubeTrackResolver
import com.lukeneedham.lyrictutor.data.track.TrackResolver
import com.lukeneedham.lyrictutor.data.track.TrackSearcher
import com.lukeneedham.lyrictutor.data.wiktionary.WiktionaryClient
import com.lukeneedham.lyrictutor.data.wiktionary.WiktionaryLookup
import com.lukeneedham.lyrictutor.ui.HomeViewModel
import com.lukeneedham.lyrictutor.ui.SettingsViewModel
import com.lukeneedham.lyrictutor.ui.SongViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

object KoinModules {
    private val app = module {
        single<SongCatalog> { FallbackCatalog(listOf(ItunesCatalog(client = get()), DeezerCatalog(client = get()))) }
        single<TrackResolver> {
            MultiSourceTrackResolver(
                spotify = SpotifyTrackResolver(client = get()),
                youtube = YoutubeTrackResolver(client = get(), catalog = get()),
            )
        }

        single { LrclibLyricsSource(client = get()) }
        single<LyricsSource> { get<LrclibLyricsSource>() }
        single<TrackSearcher> { get<LrclibLyricsSource>() }
        single { LyricsRepository(songCache = get(), source = get()) }

        single<WiktionaryLookup> { WiktionaryClient(client = get(), pageCache = get()) }
        single<PartOfSpeechChooser> {
            ProviderPartOfSpeechChooser(
                settingsStore = get(),
                choosers = AiProvider.entries.associateWith { provider ->
                    if (provider == AiProvider.Gemini) {
                        GeminiPartOfSpeechChooser(client = get(), settingsStore = get())
                    } else {
                        ChatCompletionsPartOfSpeechChooser(provider = provider, client = get(), settingsStore = get())
                    }
                },
            )
        }
        single<LyricsAnnotator> {
            ProviderAnnotator(
                settingsStore = get(),
                annotators = AiProvider.entries.associateWith { provider ->
                    if (provider == AiProvider.Gemini) {
                        GeminiAnnotator(client = get(), settingsStore = get())
                    } else {
                        ChatCompletionsAnnotator(provider = provider, client = get(), settingsStore = get())
                    }
                },
            )
        }
        single { AnnotationRepository(songCache = get(), annotator = get()) }

        viewModel { HomeViewModel(songCache = get(), trackSearcher = get(), catalog = get()) }
        viewModel { (trackId: String) -> SongViewModel(
                trackId = trackId,
                trackResolver = get(),
                songCache = get(),
                lyricsRepository = get(),
                annotationRepository = get(),
                wiktionaryLookup = get(),
                partOfSpeechChooser = get(),
                artColorExtractor = get(),
                playProviderStore = get(),
            ) }
        viewModel {
            SettingsViewModel(
                isDebugEnabled = DebugFlags.isEnabled,
                settingsStore = get(),
                modelCatalogs = AiProvider.entries.associateWith { provider ->
                    if (provider == AiProvider.Gemini) GeminiModelCatalog(client = get()) else ChatCompletionsModelCatalog(provider = provider, client = get())
                },
            )
        }
    }

    val modules: List<Module> = listOf(app)
}
