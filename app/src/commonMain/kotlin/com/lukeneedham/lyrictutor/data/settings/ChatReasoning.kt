package com.lukeneedham.lyrictutor.data.settings

/** How a chat completions service is told to think before it answers: translating needs little or none. */
enum class ChatReasoning {
    /** `thinking: disabled`: the model can answer without thinking. */
    Off,

    /** `reasoning_effort: low`: the model always thinks and cannot be switched off (Z.AI's GLM models), so the least is asked. */
    Low,
}
