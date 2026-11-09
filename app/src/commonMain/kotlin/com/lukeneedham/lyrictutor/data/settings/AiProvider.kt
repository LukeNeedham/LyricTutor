package com.lukeneedham.lyrictutor.data.settings

/**
 * The AI service that writes the translations. Each has its own key and model in [AiSettings].
 * [chatBaseUrl] is where an OpenAI-style chat completions API is (`<base>/chat/completions`); null for Gemini, which has its own API.
 * [reasoning] is how a chat completions service is asked to think little.
 */
enum class AiProvider(val displayName: String, val defaultModel: String, val chatBaseUrl: String? = null, val reasoning: ChatReasoning = ChatReasoning.Off) {
    Gemini("Gemini", "gemini-3.8-flash"),
    DeepSeek("DeepSeek", "deepseek-v4-flash", "https://api.deepseek.com"),

    /** Z.AI (Zhipu, the GLM models), on its general API: a key of the GLM Coding Plan is for the coding endpoint and is refused here. */
    ZAi("Z.AI", "glm-5.1", "https://api.z.ai/api/paas/v4", ChatReasoning.Low),
}
