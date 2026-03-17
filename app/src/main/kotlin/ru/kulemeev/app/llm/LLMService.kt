package ru.kulemeev.app.llm

import ai.koog.prompt.dsl.prompt
import ai.koog.prompt.executor.clients.LLMClient
import ai.koog.prompt.executor.ollama.client.OllamaModels
import ai.koog.prompt.llm.LLModel
import ai.koog.prompt.params.LLMParams
import ai.koog.prompt.streaming.StreamFrame
import kotlinx.coroutines.flow.Flow
import ru.kulemeev.app.ChatMessage
import java.util.UUID

class LLMService(
    private val client: LLMClient,
    private val model: LLModel,
    var systemPrompt: String,
    var temperature: Double
) {
    fun streamResponse(history: List<ChatMessage>): Flow<StreamFrame> {
        val prompt = prompt(UUID.randomUUID().toString(), LLMParams(temperature = temperature)) {
            system(systemPrompt)
            history.forEach { message ->
                when (message) {
                    is ChatMessage.User -> user(message.content)
                    is ChatMessage.Assistant -> assistant(message.content)
                }
            }
        }
        return client.executeStreaming(prompt, model)
    }
}
