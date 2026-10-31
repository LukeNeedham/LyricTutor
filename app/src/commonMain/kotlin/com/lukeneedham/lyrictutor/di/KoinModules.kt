package com.lukeneedham.lyrictutor.di

import com.lukeneedham.lyrictutor.data.annotation.AnnotationRepository
import com.lukeneedham.lyrictutor.data.annotation.GeminiAnnotator
import com.lukeneedham.lyrictutor.data.annotation.GeminiModelCatalog
import com.lukeneedham.lyrictutor.data.annotation.GeminiPartOfSpeechChooser
import com.lukeneedham.lyrictutor.data.annotation.LyricsAnnotator
import com.lukeneedham.lyrictutor.data.annotation.ModelCatalog
import com.lukeneedham.lyrictutor.data.annotation.PartOfSpeechChooser
import com.lukeneedham.lyrictutor.data.lyrics.LrclibLyricsSource
import com.lukeneedham.lyrictutor.data.lyrics.LyricsRepository
import com.lukeneedham.lyrictutor.data.lyrics.LyricsSource
import com.lukeneedham.lyrictutor.data.track.MultiSourceTrackResolver
import com.lukeneedham.lyrictutor.data.track.SpotifyTrackResolver
import com.lukeneedham.lyrictutor.data.track.YoutubeTrackResolver
import com.lukeneedham.lyrictutor.data.track.TrackResolver
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
        single<TrackResolver> { MultiSourceTrackResolver(spotify = SpotifyTrackResolver(client = get()), youtube = YoutubeTrackResolver(client = get())) }

        single<LyricsSource> { LrclibLyricsSource(client = get()) }
        single { LyricsRepository(songCache = get(), source = get()) }

        single<WiktionaryLookup> { WiktionaryClient(client = get(), pageCache = get()) }
        single<ModelCatalog> { GeminiModelCatalog(client = get()) }
        single<PartOfSpeechChooser> { GeminiPartOfSpeechChooser(client = get(), settingsStore = get()) }
        single<LyricsAnnotator> { GeminiAnnotator(client = get(), settingsStore = get()) }
        single { AnnotationRepository(songCache = get(), annotator = get()) }

        viewModel { HomeViewModel(songCache = get()) }
        viewModel { (trackId: String) -> SongViewModel(
                trackId = trackId,
                trackResolver = get(),
                songCache = get(),
                lyricsRepository = get(),
                annotationRepository = get(),
                wiktionaryLookup = get(),
                partOfSpeechChooser = get(),
                artColorExtractor = get(),
            ) }
        viewModel { SettingsViewModel(isDebugEnabled = DebugFlags.isEnabled, settingsStore = get(), modelCatalog = get()) }
    }

    val modules: List<Module> = listOf(app)
}
