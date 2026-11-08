package com.lukeneedham.lyrictutor.ui

import com.lukeneedham.lyrictutor.data.settings.AiProvider

data class SettingsUiState(
    /** Whether the debug pages are on (debug builds): the page shows the way to them only then. */
    val isDebugEnabled: Boolean,
    /** The service that writes the translations. */
    val provider: AiProvider = AiProvider.Gemini,
    /** The API keys being edited, one for each service. They are shown hidden. */
    val geminiApiKey: String = "",
    val geminiModel: String = "",
    val deepSeekApiKey: String = "",
    val deepSeekModel: String = "",
    /** Set after a save, until the next edit. */
    val isSaved: Boolean = false,
    /** The models of the key of [provider], once asked for. */
    val models: ModelListUiState = ModelListUiState.Idle,
) {
    /** The key being edited: the one of [provider]. */
    val apiKey: String get() = if (provider == AiProvider.DeepSeek) deepSeekApiKey else geminiApiKey

    /** The model being edited: the one of [provider]. */
    val model: String get() = if (provider == AiProvider.DeepSeek) deepSeekModel else geminiModel
}
