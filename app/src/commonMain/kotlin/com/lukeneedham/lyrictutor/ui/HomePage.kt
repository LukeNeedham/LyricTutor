package com.lukeneedham.lyrictutor.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel

/** The page: connects to the ViewModel and the navigation, and delegates all UI. No preview. */
@Composable
fun HomePage(onOpenSong: (trackId: String) -> Unit, onOpenSettings: () -> Unit) {
    val viewModel = koinViewModel<HomeViewModel>()
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is HomeEvent.OpenSong -> onOpenSong(event.trackId)
            }
        }
    }

    HomePageContent(
        state = state,
        onInputChange = viewModel::onInputChange,
        onSubmit = viewModel::onSubmit,
        onLanguageClick = viewModel::onLanguageClick,
        onOpenSettings = onOpenSettings,
        onOpenSong = onOpenSong,
    )
}
