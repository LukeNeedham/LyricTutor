package com.lukeneedham.lyrictutor.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lukeneedham.lyrictutor.data.song.SongCache
import com.lukeneedham.lyrictutor.data.track.SpotifyLinkParser
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class HomeViewModel(songCache: SongCache) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _events = Channel<HomeEvent>(Channel.BUFFERED)
    val events: Flow<HomeEvent> = _events.receiveAsFlow()

    init {
        viewModelScope.launch {
            songCache.recents.collect { recents -> _uiState.update { it.copy(recents = recents) } }
        }
    }

    fun onInputChange(input: String) {
        _uiState.update { it.copy(input = input, showInvalidLink = false) }
    }

    fun onSubmit() {
        val trackId = SpotifyLinkParser.parseTrackId(_uiState.value.input)
        if (trackId == null) {
            _uiState.update { it.copy(showInvalidLink = true) }
        } else {
            _events.trySend(HomeEvent.OpenSong(trackId))
        }
    }
}
