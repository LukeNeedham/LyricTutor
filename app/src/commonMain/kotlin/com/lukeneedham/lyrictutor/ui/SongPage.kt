package com.lukeneedham.lyrictutor.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/** The page: connects to the ViewModel and the navigation, and delegates all UI. No preview. */
@Composable
fun SongPage(trackId: String) {
    val viewModel = koinViewModel<SongViewModel>(key = trackId) { parametersOf(trackId) }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    SongPageContent(state = state, onRetry = viewModel::onRetry)
}
