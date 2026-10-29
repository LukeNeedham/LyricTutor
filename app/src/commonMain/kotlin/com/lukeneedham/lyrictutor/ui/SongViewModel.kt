package com.lukeneedham.lyrictutor.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lukeneedham.lyrictutor.data.track.TrackResolveException
import com.lukeneedham.lyrictutor.data.track.TrackResolver
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SongViewModel(
    private val trackId: String,
    private val trackResolver: TrackResolver,
) : ViewModel() {

    private val _uiState = MutableStateFlow<SongUiState>(SongUiState.Loading)
    val uiState: StateFlow<SongUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun onRetry() = load()

    private fun load() {
        _uiState.value = SongUiState.Loading
        viewModelScope.launch {
            trackResolver.resolve(trackId)
                .onSuccess { _uiState.value = SongUiState.Loaded(it) }
                .onFailure { _uiState.value = SongUiState.Error(isNetwork = it is TrackResolveException.Network) }
        }
    }
}
