package com.lukeneedham.lyrictutor.di

import com.lukeneedham.lyrictutor.data.annotation.AnnotationRepository
import com.lukeneedham.lyrictutor.data.annotation.GeminiAnnotator
import com.lukeneedham.lyrictutor.data.annotation.LyricsAnnotator
import com.lukeneedham.lyrictutor.data.lyrics.LrclibLyricsSource
import com.lukeneedham.lyrictutor.data.lyrics.LyricsRepository
import com.lukeneedham.lyrictutor.data.lyrics.LyricsSource
import com.lukeneedham.lyrictutor.data.track.SpotifyTrackResolver
import com.lukeneedham.lyrictutor.data.wiktionary.WiktionaryClient
import com.lukeneedham.lyrictutor.data.wiktionary.WiktionaryLookup
import com.lukeneedham.lyrictutor.data.track.TrackResolver
import com.lukeneedham.lyrictutor.ui.HomeViewModel
import com.lukeneedham.lyrictutor.ui.SettingsViewModel
import com.lukeneedham.lyrictutor.ui.SongViewModel
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import com.lukeneedham.common.DebugKit
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    single {
        HttpClient {
            // The default of the OkHttp engine is ten seconds for each of these, too short for Gemini; its call sets its own limits
            install(HttpTimeout) {
                requestTimeoutMillis = 30_000
                connectTimeoutMillis = 15_000
                socketTimeoutMillis = 30_000
            }
        }
    }
    single<TrackResolver> { SpotifyTrackResolver(client = get()) }

    single<LyricsSource> { LrclibLyricsSource(client = get()) }
    single { LyricsRepository(songCache = get(), source = get()) }

    single<WiktionaryLookup> { WiktionaryClient(client = get()) }
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
        ) }
    viewModel { SettingsViewModel(isDebugEnabled = DebugKit.enabled, settingsStore = get()) }
}
