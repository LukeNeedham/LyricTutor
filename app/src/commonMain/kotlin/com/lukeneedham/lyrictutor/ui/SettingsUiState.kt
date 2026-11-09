package com.lukeneedham.lyrictutor.ui

import com.lukeneedham.lyrictutor.data.settings.AiProvider
import com.lukeneedham.lyrictutor.data.settings.ProviderSettings

data class SettingsUiState(
    /** Whether the debug pages are on (debug builds): the page shows the way to them only then. */
    val isDebugEnabled: Boolean,
    /** The service that writes the translations. */
    val provider: AiProvider = AiProvider.Gemini,
    /** The keys and models being edited, one for each service that has been touched. They are shown hidden. */
    val providers: Map<AiProvider, ProviderSettings> = emptyMap(),
    /** Set after a save, until the next edit. */
    val isSaved: Boolean = false,
    /** The models of the key of [provider], once asked for. */
    val models: ModelListUiState = ModelListUiState.Idle,
) {
    fun of(provider: AiProvider): ProviderSettings = providers[provider] ?: ProviderSettings(model = provider.defaultModel)

    /** The key being edited: the one of [provider]. */
    val apiKey: String get() = of(provider).apiKey

    /** The model being edited: the one of [provider]. */
    val model: String get() = of(provider).model

    /** Whether the key being edited is of a coding subscription. */
    val useCodingPlan: Boolean get() = of(provider).useCodingPlan
}
