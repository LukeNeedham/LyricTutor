package com.lukeneedham.lyrictutor.di

import com.lukeneedham.lyrictutor.data.lyrics.LrclibLyricsSource
import com.lukeneedham.lyrictutor.data.lyrics.LyricsRepository
import com.lukeneedham.lyrictutor.data.lyrics.LyricsSource
import com.lukeneedham.lyrictutor.data.track.SpotifyTrackResolver
import com.lukeneedham.lyrictutor.data.track.TrackResolver
import com.lukeneedham.lyrictutor.ui.HomeViewModel
import com.lukeneedham.lyrictutor.ui.SettingsViewModel
import com.lukeneedham.lyrictutor.ui.SongViewModel
import io.ktor.client.HttpClient
import com.lukeneedham.common.DebugKit
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    single { HttpClient() }
    single<TrackResolver> { SpotifyTrackResolver(client = get()) }

    single<LyricsSource> { LrclibLyricsSource(client = get()) }
    single { LyricsRepository(songCache = get(), source = get()) }

    viewModel { HomeViewModel(songCache = get()) }
    viewModel { (trackId: String) -> SongViewModel(
            trackId = trackId,
            trackResolver = get(),
            songCache = get(),
            lyricsRepository = get(),
        ) }
    viewModel { SettingsViewModel(isDebugEnabled = DebugKit.enabled) }
}
