package ru.kulemeev.app.config

import kotlinx.serialization.Serializable

@Serializable
data class ChatConfig(
    val temperature: Double = 0.7,
    val systemPrompt: String = "Ты — полезный AI-ассистент.",
    val modelId: String = "openrouter/free",
    val maxTokens: Int? = null,
    val stopSequences: List<String> = emptyList(),
    val maxHistoryPairs: Int = 10
)
