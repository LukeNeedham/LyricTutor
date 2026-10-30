package com.lukeneedham.lyrictutor.ui

data class SettingsUiState(
    /** Whether the debug pages are on (debug builds): the page shows the way to them only then. */
    val isDebugEnabled: Boolean,
    /** The Gemini API key being edited. It is shown hidden. */
    val apiKey: String = "",
    val model: String = "",
    /** Set after a save, until the next edit. */
    val isSaved: Boolean = false,
)
