package com.lukeneedham.lyrictutor.di

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

    viewModel { HomeViewModel() }
    viewModel { (trackId: String) -> SongViewModel(trackId = trackId, trackResolver = get()) }
    viewModel { SettingsViewModel(isDebugEnabled = DebugKit.enabled) }
}
