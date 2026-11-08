package com.lukeneedham.lyrictutor.data.settings

/** The AI service that writes the translations. Each has its own key and model in [AiSettings]. */
enum class AiProvider(val defaultModel: String) {
    Gemini("gemini-3.8-flash"),
    DeepSeek("deepseek-v4-flash"),
}
