package com.lukeneedham.lyrictutor.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lukeneedham.lyrictutor.data.annotation.AnnotationException
import com.lukeneedham.lyrictutor.data.annotation.AnnotationRepository
import com.lukeneedham.lyrictutor.data.annotation.WordAnnotation
import com.lukeneedham.lyrictutor.data.lyrics.Lyrics
import com.lukeneedham.lyrictutor.data.lyrics.LyricsRepository
import com.lukeneedham.lyrictutor.data.song.SongCache
import com.lukeneedham.lyrictutor.data.track.TrackInfo
import com.lukeneedham.lyrictutor.data.track.TrackResolveException
import com.lukeneedham.lyrictutor.data.track.TrackResolver
import kotlinx.coroutines.Job
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
    private val annotationRepository: AnnotationRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<SongUiState>(SongUiState.Loading)
    val uiState: StateFlow<SongUiState> = _uiState.asStateFlow()

    private var annotationJob: Job? = null

    init {
        load()
    }

    fun onRetry() = load()

    fun onRetryLyrics() {
        val loaded = _uiState.value as? SongUiState.Loaded ?: return
        updateLoaded { it.copy(lyrics = LyricsUiState.Loading) }
        loadLyrics(loaded.track)
    }

    fun onRetryAnnotations() {
        val loaded = _uiState.value as? SongUiState.Loaded ?: return
        val lyrics = (loaded.lyrics as? LyricsUiState.Found)?.lyrics ?: return
        annotate(loaded.track, lyrics)
    }

    fun onWordClick(word: WordAnnotation) = updateLoaded { it.copy(selectedWord = word) }

    fun onWordDismiss() = updateLoaded { it.copy(selectedWord = null) }

    fun onOpenPaste() = updateLoaded { it.copy(pasteText = "") }

    fun onPasteChange(text: String) = updateLoaded { if (it.pasteText != null) it.copy(pasteText = text) else it }

    fun onClosePaste() = updateLoaded { it.copy(pasteText = null) }

    fun onSavePaste() {
        val loaded = _uiState.value as? SongUiState.Loaded ?: return
        val text = loaded.pasteText ?: return
        viewModelScope.launch {
            val lyrics = lyricsRepository.savePasted(loaded.track, text) ?: return@launch
            updateLoaded { it.copy(lyrics = LyricsUiState.Found(lyrics), pasteText = null) }
            annotate(loaded.track, lyrics)
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
            if (lyrics is LyricsUiState.Found) annotate(track, lyrics.lyrics)
        }
    }

    private fun annotate(track: TrackInfo, lyrics: Lyrics) {
        annotationJob?.cancel()
        updateLoaded { it.copy(annotationStatus = AnnotationStatus.Loading) }
        annotationJob = viewModelScope.launch {
            val result = annotationRepository.annotate(track, lyrics.lines.map { it.text }) { batch ->
                updateLoaded { it.copy(annotations = it.annotations + batch.associateBy { annotation -> annotation.index }) }
            }
            val status = result.fold(
                onSuccess = { AnnotationStatus.Idle },
                onFailure = {
                    when (it) {
                        is AnnotationException.MissingKey -> AnnotationStatus.MissingKey
                        is AnnotationException.InvalidKey -> AnnotationStatus.InvalidKey
                        is AnnotationException.RateLimited -> AnnotationStatus.RateLimited
                        else -> AnnotationStatus.Failed
                    }
                },
            )
            updateLoaded { it.copy(annotationStatus = status) }
        }
    }

    private fun updateLoaded(transform: (SongUiState.Loaded) -> SongUiState.Loaded) {
        _uiState.update { if (it is SongUiState.Loaded) transform(it) else it }
    }
}
