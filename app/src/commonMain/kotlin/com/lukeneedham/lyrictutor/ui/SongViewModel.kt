package com.lukeneedham.lyrictutor.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lukeneedham.lyrictutor.data.annotation.AnnotationException
import com.lukeneedham.lyrictutor.data.art.ArtColorExtractor
import com.lukeneedham.lyrictutor.data.annotation.AnnotationRepository
import com.lukeneedham.lyrictutor.data.annotation.PartOfSpeechChooser
import com.lukeneedham.lyrictutor.data.lyrics.Lyrics
import com.lukeneedham.lyrictutor.data.lyrics.LyricsRepository
import com.lukeneedham.lyrictutor.data.song.SongCache
import com.lukeneedham.lyrictutor.data.track.TrackInfo
import com.lukeneedham.lyrictutor.data.track.TrackResolveException
import com.lukeneedham.lyrictutor.data.track.TrackResolver
import com.lukeneedham.lyrictutor.data.wiktionary.WiktionaryLookup
import com.lukeneedham.lyrictutor.data.wiktionary.WiktionaryProtocol
import com.lukeneedham.lyrictutor.data.wiktionary.WiktionarySense
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
    private val partOfSpeechChooser: PartOfSpeechChooser,
    private val artColorExtractor: ArtColorExtractor,
) : ViewModel() {

    private val _uiState = MutableStateFlow<SongUiState>(SongUiState.Loading)
    val uiState: StateFlow<SongUiState> = _uiState.asStateFlow()

    private var annotationJob: Job? = null
    private var wiktionaryJob: Job? = null
    private val wiktionaryCache = mutableMapOf<String, WiktionaryUiState>()
    private val partOfSpeechChoices = mutableMapOf<String, String>()

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
        updateLoaded { it.copy(selectedWord = selected, wiktionary = WiktionaryUiState.Loading) }
        wiktionaryJob = viewModelScope.launch {
            val state = loadWiktionary(selected)
            updateLoaded { if (it.selectedWord == selected) it.copy(wiktionary = state) else it }
        }
    }

    /** What Wiktionary says of the word, for the part of speech the word is used as in its line when its page has several. */
    private suspend fun loadWiktionary(selected: SelectedWord): WiktionaryUiState {
        val key = "${selected.language}|${selected.surface.lowercase()}"
        val page = wiktionaryCache[key] ?: wiktionaryLookup.find(selected.surface, selected.language).fold(
            onSuccess = { result ->
                if (result != null) WiktionaryUiState.Found(result)
                else WiktionaryUiState.NotFound(WiktionaryProtocol.searchUrl(WiktionaryProtocol.displayWord(selected.surface)))
            },
            onFailure = { WiktionaryUiState.Error },
        ).also { state ->
            // A failure is not kept, so that tapping the word again asks again
            if (state !is WiktionaryUiState.Error) wiktionaryCache[key] = state
        }
        if (page !is WiktionaryUiState.Found) return page
        return WiktionaryUiState.Found(page.result.copy(senses = senseOfUse(page.result.senses, selected)))
    }

    /**
     * The senses of the part of speech that the word is used as in its line. A word with one part of speech needs no question; for
     * several the AI chooses among them, which costs a few tokens. With no answer (no key, a failure, an answer that is none of
     * them) every sense is kept, as Wiktionary has them.
     */
    private suspend fun senseOfUse(senses: List<WiktionarySense>, selected: SelectedWord): List<WiktionarySense> {
        val options = senses.map { it.partOfSpeech }.distinct()
        if (options.size < 2) return senses
        val line = ((_uiState.value as? SongUiState.Loaded)?.lyrics as? LyricsUiState.Found)?.lyrics?.lines
            ?.getOrNull(selected.lineIndex)?.text ?: return senses

        val key = "${selected.language}|${selected.surface.lowercase()}|$line"
        val chosen = partOfSpeechChoices[key]
            ?: partOfSpeechChooser.choose(line, selected.surface, selected.language, options).getOrNull()
                ?.also { partOfSpeechChoices[key] = it }
        return senses.filter { it.partOfSpeech.equals(chosen, ignoreCase = true) }.ifEmpty { senses }
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
            loadArtColors(track)
        }
    }

    /** The page is painted with the colours of the cover once they are read; until then, and without a cover, it keeps the app's own. */
    private fun loadArtColors(track: TrackInfo) {
        val coverUrl = track.coverUrl
        if (coverUrl == null) {
            updateLoaded { it.copy(isArtResolved = true) }
            return
        }
        viewModelScope.launch {
            val colors = artColorExtractor.extract(coverUrl)
            updateLoaded { it.copy(artColors = colors, isArtResolved = true) }
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
