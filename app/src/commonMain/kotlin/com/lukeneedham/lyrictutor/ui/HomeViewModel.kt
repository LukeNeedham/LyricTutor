package com.lukeneedham.lyrictutor.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lukeneedham.lyrictutor.data.song.SongCache
import com.lukeneedham.lyrictutor.data.track.TrackIds
import com.lukeneedham.lyrictutor.data.track.TrackInfo
import com.lukeneedham.lyrictutor.data.track.TitleCleaner
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class HomeViewModel(private val songCache: SongCache) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _events = Channel<HomeEvent>(Channel.BUFFERED)
    val events: Flow<HomeEvent> = _events.receiveAsFlow()

    init {
        viewModelScope.launch {
            songCache.recents.collect { recents -> _uiState.update { it.copy(recents = recents) } }
        }
        viewModelScope.launch {
            songCache.languages.collect { languages ->
                // A language no song has any more cannot stay selected
                _uiState.update { state ->
                    state.copy(songLanguages = languages, selectedLanguage = state.selectedLanguage?.takeIf { selected -> languages.values.any { selected in it } })
                }
            }
        }
    }

    fun onInputChange(input: String) {
        _uiState.update { it.copy(input = input, showInvalidLink = false) }
    }

    /** Narrows the songs to [language]; the same language again shows them all. */
    fun onLanguageClick(language: String) {
        _uiState.update { it.copy(selectedLanguage = if (it.selectedLanguage == language) null else language) }
    }

    fun onModeChange(mode: LookupMode) {
        _uiState.update { it.copy(mode = mode, showInvalidLink = false, showMissingName = false) }
    }

    fun onSongNameChange(songName: String) {
        _uiState.update { it.copy(songName = songName, showMissingName = false) }
    }

    fun onArtistNameChange(artistName: String) {
        _uiState.update { it.copy(artistName = artistName, showMissingName = false) }
    }

    fun onSubmit() {
        when (_uiState.value.mode) {
            LookupMode.Link -> submitLink()
            LookupMode.Name -> submitName()
        }
    }

    private fun submitLink() {
        val trackId = TrackIds.parseLink(_uiState.value.input)
        if (trackId == null) {
            _uiState.update { it.copy(showInvalidLink = true) }
        } else {
            // The box is empty again when the user comes back to the page
            _uiState.update { it.copy(input = "") }
            _events.trySend(HomeEvent.OpenSong(trackId))
        }
    }

    /** A song typed in has no page to read, so it is saved here, and the song page finds it in the cache. */
    private fun submitName() {
        val state = _uiState.value
        val title = state.songName.trim()
        val artist = state.artistName.trim()
        if (title.isEmpty() || artist.isEmpty()) {
            _uiState.update { it.copy(showMissingName = true) }
            return
        }
        val track = TrackInfo(
            trackId = TrackIds.byName(title, artist),
            title = title,
            searchTitle = TitleCleaner.clean(title),
            artists = listOf(artist),
            coverUrl = null,
            durationSeconds = null,
            releaseDate = null,
        )
        _uiState.update { it.copy(songName = "", artistName = "") }
        viewModelScope.launch {
            if (songCache.getTrack(track.trackId) == null) songCache.saveTrack(track)
            _events.trySend(HomeEvent.OpenSong(track.trackId))
        }
    }
}
