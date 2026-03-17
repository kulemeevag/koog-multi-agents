package ru.kulemeev.app.llm

import ai.koog.prompt.dsl.prompt
import ai.koog.prompt.executor.clients.LLMClient
import ai.koog.prompt.executor.clients.openrouter.OpenRouterParams
import ai.koog.prompt.llm.LLModel
import ai.koog.prompt.params.LLMParams
import ai.koog.prompt.streaming.StreamFrame
import kotlinx.coroutines.flow.Flow
import ru.kulemeev.app.ChatMessage
import java.util.*

class LLMService(
    private val client: LLMClient,
    var model: LLModel,
    var systemPrompt: String,
    var temperature: Double,
    var maxTokens: Int? = null,
    var stopSequences: List<String> = emptyList()
) {
    fun streamResponse(
        history: List<ChatMessage>,
        overrideParams: LLMParams? = null
    ): Flow<StreamFrame> {
        // If overrideParams is provided (e.g. from /compare), we use it.
        // Otherwise, we construct a fresh OpenRouterParams from our current state.
        val params = overrideParams ?: OpenRouterParams(
            temperature = temperature,
            maxTokens = maxTokens,
            stop = stopSequences.takeIf { it.isNotEmpty() }
        )

        val prompt = prompt(UUID.randomUUID().toString(), params) {
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
