package com.lukeneedham.lyrictutor.ui

/** The list of models of the key in Settings, fetched when the user asks for it. */
sealed interface ModelListUiState {
    data object Idle : ModelListUiState

    data object Loading : ModelListUiState

    data class Loaded(val models: List<String>) : ModelListUiState

    /** There is no key to ask with. */
    data object MissingKey : ModelListUiState

    data object InvalidKey : ModelListUiState

    data object Failed : ModelListUiState
}
