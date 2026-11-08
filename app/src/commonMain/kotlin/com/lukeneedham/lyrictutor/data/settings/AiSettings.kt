package com.lukeneedham.lyrictutor.data.settings

data class AiSettings(
    /** The service that writes the translations. */
    val provider: AiProvider = AiProvider.Gemini,
    /** The user's own Gemini API key. Empty until they enter it. */
    val geminiApiKey: String = "",
    val geminiModel: String = AiProvider.Gemini.defaultModel,
    /** The user's own DeepSeek API key. Empty until they enter it. */
    val deepSeekApiKey: String = "",
    val deepSeekModel: String = AiProvider.DeepSeek.defaultModel,
)
