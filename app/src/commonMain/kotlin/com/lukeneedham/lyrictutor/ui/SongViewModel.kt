package com.lukeneedham.lyrictutor.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lukeneedham.lyrictutor.data.annotation.AnnotationException
import com.lukeneedham.lyrictutor.data.annotation.AnnotationRepository
import com.lukeneedham.lyrictutor.data.lyrics.Lyrics
import com.lukeneedham.lyrictutor.data.lyrics.LyricsRepository
import com.lukeneedham.lyrictutor.data.song.SongCache
import com.lukeneedham.lyrictutor.data.track.TrackInfo
import com.lukeneedham.lyrictutor.data.track.TrackResolveException
import com.lukeneedham.lyrictutor.data.track.TrackResolver
import com.lukeneedham.lyrictutor.data.wiktionary.WiktionaryLookup
import com.lukeneedham.lyrictutor.data.wiktionary.WiktionaryProtocol
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
    private val wiktionaryLookup: WiktionaryLookup,
) : ViewModel() {

    private val _uiState = MutableStateFlow<SongUiState>(SongUiState.Loading)
    val uiState: StateFlow<SongUiState> = _uiState.asStateFlow()

    private var annotationJob: Job? = null
    private var wiktionaryJob: Job? = null
    private val wiktionaryCache = mutableMapOf<String, WiktionaryUiState>()

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

    /** Throws the translations of the song away and asks for them all again: the AI can get a song wrong. */
    fun onTranslateAgain() {
        val loaded = _uiState.value as? SongUiState.Loaded ?: return
        val lyrics = (loaded.lyrics as? LyricsUiState.Found)?.lyrics ?: return
        annotate(loaded.track, lyrics, forgetFirst = true)
    }

    fun onWordClick(selected: SelectedWord) {
        wiktionaryJob?.cancel()
        val key = "${selected.language}|${selected.surface.lowercase()}"
        val known = wiktionaryCache[key]
        updateLoaded { it.copy(selectedWord = selected, wiktionary = known ?: WiktionaryUiState.Loading) }
        if (known != null) return
        wiktionaryJob = viewModelScope.launch {
            val state = wiktionaryLookup.find(selected.surface, selected.language).fold(
                onSuccess = { result ->
                    if (result != null) WiktionaryUiState.Found(result)
                    else WiktionaryUiState.NotFound(WiktionaryProtocol.searchUrl(selected.surface))
                },
                onFailure = { WiktionaryUiState.Error },
            )
            // A failure is not kept, so that tapping the word again asks again
            if (state !is WiktionaryUiState.Error) wiktionaryCache[key] = state
            updateLoaded { if (it.selectedWord == selected) it.copy(wiktionary = state) else it }
        }
    }

    fun onWordDismiss() {
        wiktionaryJob?.cancel()
        updateLoaded { it.copy(selectedWord = null) }
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

    private fun annotate(track: TrackInfo, lyrics: Lyrics, forgetFirst: Boolean = false) {
        annotationJob?.cancel()
        updateLoaded {
            val kept = if (forgetFirst) emptyMap() else it.annotations
            it.copy(
                annotations = kept,
                annotationStatus = AnnotationStatus.Loading,
                annotationDetail = null,
                selectedWord = if (forgetFirst) null else it.selectedWord,
            )
        }
        annotationJob = viewModelScope.launch {
            if (forgetFirst) annotationRepository.forget(track)
            val result = annotationRepository.annotate(track, lyrics.lines.map { it.text }) { batch ->
                updateLoaded { it.copy(annotations = it.annotations + batch.associateBy { annotation -> annotation.index }) }
            }
            val status = result.fold(
                onSuccess = { AnnotationStatus.Idle },
                onFailure = {
                    when (it) {
                        is AnnotationException.MissingKey -> AnnotationStatus.MissingKey
                        is AnnotationException.InvalidKey -> AnnotationStatus.InvalidKey
                        is AnnotationException.RateLimited -> if (it.isDaily) AnnotationStatus.DailyLimit else AnnotationStatus.RateLimited
                        else -> AnnotationStatus.Failed
                    }
                },
            )
            val detail = if (status == AnnotationStatus.Failed) result.exceptionOrNull()?.message else null
            updateLoaded { it.copy(annotationStatus = status, annotationDetail = detail) }
        }
    }

    private fun updateLoaded(transform: (SongUiState.Loaded) -> SongUiState.Loaded) {
        _uiState.update { if (it is SongUiState.Loaded) transform(it) else it }
    }
}
