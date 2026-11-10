package com.lukeneedham.lyrictutor.data.settings

/** The user's own API key for one [AiProvider], and the model to ask. */
data class ProviderSettings(
    /** Empty until the user enters it. */
    val apiKey: String = "",
    val model: String,
)

data class AiSettings(
    /** The service that writes the translations. */
    val provider: AiProvider = AiProvider.Gemini,
    /** What the user entered for each service: a missing one has no key and the default model. */
    val providers: Map<AiProvider, ProviderSettings> = emptyMap(),
) {
    fun of(provider: AiProvider): ProviderSettings = providers[provider] ?: ProviderSettings(model = provider.defaultModel)

    /** The settings of the chosen service. */
    val active: ProviderSettings get() = of(provider)

    fun with(provider: AiProvider, settings: ProviderSettings) = copy(providers = providers + (provider to settings))
}
