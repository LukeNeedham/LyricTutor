package com.lukeneedham.lyrictutor.di

import com.lukeneedham.lyrictutor.data.annotation.AnnotationRepository
import com.lukeneedham.lyrictutor.data.annotation.LyricsAnnotator
import com.lukeneedham.lyrictutor.data.annotation.PartOfSpeechChooser
import com.lukeneedham.lyrictutor.data.catalog.SongCatalog
import com.lukeneedham.lyrictutor.data.catalog.DeezerCatalog
import com.lukeneedham.lyrictutor.data.catalog.FallbackCatalog
import com.lukeneedham.lyrictutor.data.catalog.ItunesCatalog
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
        single<PartOfSpeechChooser> { AiProviderFactory.partOfSpeechChooser(client = get(), settingsStore = get()) }
        single<LyricsAnnotator> { AiProviderFactory.annotator(client = get(), settingsStore = get()) }
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
        single { AiProviderFactory.modelCatalogs(client = get()) }
        viewModel { SettingsViewModel(isDebugEnabled = DebugFlags.isEnabled, settingsStore = get(), modelCatalogs = get()) }
    }

    val modules: List<Module> = listOf(app)
}
