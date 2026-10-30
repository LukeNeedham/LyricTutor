package com.lukeneedham.lyrictutor.ui

import com.lukeneedham.lyrictutor.data.annotation.LineAnnotation
import com.lukeneedham.lyrictutor.data.art.ArtColors
import com.lukeneedham.lyrictutor.data.track.TrackInfo

sealed interface SongUiState {
    data object Loading : SongUiState

    data class Loaded(
        val track: TrackInfo,
        val lyrics: LyricsUiState,
        /** The two colours of the cover, once read: the song's page is painted with them. */
        val artColors: ArtColors? = null,
        /** Whether reading the colours is over (with or without colours): until then the parts of the page painted with them wait. */
        val isArtResolved: Boolean = false,
        /** The text in the paste box, or null when the box is closed. */
        val pasteText: String? = null,
        /** The translation and words of each line that has them, by the index of the line. */
        val annotations: Map<Int, LineAnnotation> = emptyMap(),
        val annotationStatus: AnnotationStatus = AnnotationStatus.Idle,
        /** Why the last annotation attempt failed, for [AnnotationStatus.Failed]: shown as it is, to help find the cause. */
        val annotationDetail: String? = null,
        /** The word whose sheet is open, or null. */
        val selectedWord: SelectedWord? = null,
        /** The Wiktionary link of [selectedWord]. */
        val wiktionary: WiktionaryUiState = WiktionaryUiState.Loading,
    ) : SongUiState

    /** [isNetwork]: no connection, as opposed to a page the app could not read. */
    data class Error(val isNetwork: Boolean) : SongUiState
}
