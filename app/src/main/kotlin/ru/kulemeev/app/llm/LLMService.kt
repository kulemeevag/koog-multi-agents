package ru.kulemeev.app.llm

import ai.koog.prompt.dsl.prompt
import ai.koog.prompt.executor.ollama.client.OllamaClient
import ai.koog.prompt.executor.ollama.client.OllamaModels
import ai.koog.prompt.params.LLMParams
import ai.koog.prompt.streaming.StreamFrame
import kotlinx.coroutines.flow.Flow
import ru.kulemeev.app.Message
import java.util.UUID

class LLMService(
    private val client: OllamaClient,
    private val systemPromt: String,
    private val temperature: Double
) {
    fun streamResponse(history: List<Message>): Flow<StreamFrame> {
        val prompt = prompt(UUID.randomUUID().toString(), LLMParams(temperature = temperature)) {
            system(systemPromt)
            history.forEach { message ->
                when (message) {
                    is Message.User -> user(message.content)
                    is Message.Assistant -> assistant(message.content)
                }
            }
        }
        return client.executeStreaming(prompt, OllamaModels.Meta.LLAMA_3_2)
    }
}
