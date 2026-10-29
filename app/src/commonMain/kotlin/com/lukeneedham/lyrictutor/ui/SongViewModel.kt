package com.lukeneedham.lyrictutor.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lukeneedham.lyrictutor.data.lyrics.LyricsRepository
import com.lukeneedham.lyrictutor.data.song.SongCache
import com.lukeneedham.lyrictutor.data.track.TrackInfo
import com.lukeneedham.lyrictutor.data.track.TrackResolveException
import com.lukeneedham.lyrictutor.data.track.TrackResolver
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SongViewModel(
    private val trackId: String,
    private val trackResolver: TrackResolver,
    private val songCache: SongCache,
    private val lyricsRepository: LyricsRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<SongUiState>(SongUiState.Loading)
    val uiState: StateFlow<SongUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun onRetry() = load()

    fun onRetryLyrics() {
        val loaded = _uiState.value as? SongUiState.Loaded ?: return
        updateLoaded { it.copy(lyrics = LyricsUiState.Loading) }
        loadLyrics(loaded.track)
    }

    fun onOpenPaste() = updateLoaded { it.copy(pasteText = "") }

    fun onPasteChange(text: String) = updateLoaded { if (it.pasteText != null) it.copy(pasteText = text) else it }

    fun onClosePaste() = updateLoaded { it.copy(pasteText = null) }

    fun onSavePaste() {
        val loaded = _uiState.value as? SongUiState.Loaded ?: return
        val text = loaded.pasteText ?: return
        viewModelScope.launch {
            val lyrics = lyricsRepository.savePasted(loaded.track, text) ?: return@launch
            updateLoaded { it.copy(lyrics = LyricsUiState.Found(lyrics), pasteText = null) }
        }
    }

    private fun load() {
        _uiState.value = SongUiState.Loading
        viewModelScope.launch {
            // A song opened before is on the device, so it opens without a network
            val track = songCache.getTrack(trackId)
                ?: trackResolver.resolve(trackId).getOrElse {
                    _uiState.value = SongUiState.Error(isNetwork = it is TrackResolveException.Network)
                    return@launch
                }
            songCache.saveTrack(track)
            _uiState.value = SongUiState.Loaded(track = track, lyrics = LyricsUiState.Loading)
            loadLyrics(track)
        }
    }

    private fun loadLyrics(track: TrackInfo) {
        viewModelScope.launch {
            val lyrics = lyricsRepository.load(track).fold(
                onSuccess = { if (it == null) LyricsUiState.NotFound else LyricsUiState.Found(it) },
                onFailure = { LyricsUiState.Error },
            )
            updateLoaded { it.copy(lyrics = lyrics) }
        }
    }

    private fun updateLoaded(transform: (SongUiState.Loaded) -> SongUiState.Loaded) {
        _uiState.update { if (it is SongUiState.Loaded) transform(it) else it }
    }
}
