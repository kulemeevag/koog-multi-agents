package ru.kulemeev.app.chat

data class LLMResponse(
    val text: String = "",
    val finishReason: String? = null,
    val inputTokens: Int? = null,
    val outputTokens: Int? = null
)
