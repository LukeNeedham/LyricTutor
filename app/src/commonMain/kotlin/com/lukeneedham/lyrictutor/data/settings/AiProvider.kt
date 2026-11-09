package com.lukeneedham.lyrictutor.data.settings

/**
 * The AI service that writes the translations. Each has its own key and model in [AiSettings].
 * [chatBaseUrl] is where an OpenAI-style chat completions API is (`<base>/chat/completions`); null for Gemini, which has its own API.
 * [codingPlanBaseUrl] is the endpoint of a subscription for coding tools, for a service that has one: its key is refused on [chatBaseUrl] and the other way round.
 * [reasoning] is how a chat completions service is asked to think little.
 */
enum class AiProvider(val displayName: String, val defaultModel: String, val chatBaseUrl: String? = null, val reasoning: ChatReasoning = ChatReasoning.Off, val codingPlanBaseUrl: String? = null) {
    Gemini("Gemini", "gemini-3.8-flash"),
    DeepSeek("DeepSeek", "deepseek-v4-flash", "https://api.deepseek.com"),

    /** Z.AI (Zhipu, the GLM models), on its general API: a key of the GLM Coding Plan is for the coding endpoint and is refused here. */
    ZAi("Z.AI", "glm-5.3-flash", "https://api.z.ai/api/paas/v4", ChatReasoning.Low, "https://api.z.ai/api/coding/paas/v4"),
}

/** Where the chat completions API of this service is, for a key of the Coding Plan or a pay-as-you-go one. Null when it has none. */
fun AiProvider.chatUrl(useCodingPlan: Boolean): String? = if (useCodingPlan) codingPlanBaseUrl ?: chatBaseUrl else chatBaseUrl
