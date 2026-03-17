package ru.kulemeev.app.config

import kotlinx.serialization.Serializable

@Serializable
data class ChatConfig(
    val temperature: Double = 0.7,
    val systemPrompt: String = "Ты — полезный AI-ассистент.",
    val modelId: String = "openrouter/free"
)
