package com.lukeneedham.lyrictutor.data.annotation

/** The tokens of one or more answers: what a song costs is these, times the price of the model per token. */
data class TokenUsage(val input: Int = 0, val output: Int = 0, val thinking: Int = 0) {
    operator fun plus(other: TokenUsage) = TokenUsage(input + other.input, output + other.output, thinking + other.thinking)

    /** For example "in 812, out 3401, thinking 150". Thinking tokens are billed like output tokens. */
    override fun toString() = "in $input, out $output, thinking $thinking"
}
