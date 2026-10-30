package com.lukeneedham.lyrictutor.data.settings

data class AiSettings(
    /** The user's own Gemini API key. Empty until they enter it. */
    val apiKey: String = "",
    val model: String = DEFAULT_MODEL,
) {
    companion object {
        const val DEFAULT_MODEL = "gemini-3.8-flash"
    }
}
