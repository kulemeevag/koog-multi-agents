package ru.kulemeev.app.chat

import ai.koog.prompt.dsl.Prompt
import ai.koog.prompt.dsl.prompt
import ai.koog.prompt.executor.clients.LLMClient
import ai.koog.prompt.executor.clients.openrouter.OpenRouterParams
import ai.koog.prompt.llm.LLModel
import ai.koog.prompt.message.Message
import ai.koog.prompt.params.LLMParams
import ai.koog.prompt.streaming.StreamFrame
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.util.*

/**
 * ChatAgent is the primary entity of our application, fulfilling the Day 6 requirement.
 * It encapsulates the interaction logic and history management.
 */
class ChatAgent(
    private val executor: LLMClient,
    var model: LLModel,
    var systemPrompt: String,
    var temperature: Double = 0.7,
    var maxTokens: Int? = null,
    var stopSequences: List<String> = emptyList()
) {
    // We maintain history in a Prompt object, which is the 'koog' way.
    private var currentPrompt: Prompt = prompt(UUID.randomUUID().toString()) {
        system(systemPrompt)
    }

    /**
     * Day 6: Main entry point for chat.
     */
    fun processMessageStreaming(
        userInput: String,
        overrideParams: LLMParams? = null
    ): Flow<StreamFrame> = flow {
        // 1. Prepare updated prompt with new user message
        val params = createParams(overrideParams)

        // We use the DSL-like approach to update the prompt
        val nextPrompt = currentPrompt.copy(
            id = UUID.randomUUID().toString(),
            params = params,
            messages = currentPrompt.messages + prompt(UUID.randomUUID().toString()) { user(userInput) }.messages
        )

        var accumulatedText = ""

        executor.executeStreaming(nextPrompt, model).collect { frame ->
            emit(frame)
            if (frame is StreamFrame.TextDelta) accumulatedText += frame.text
        }

        // 2. Update the persistent prompt with the assistant's response
        if (accumulatedText.isNotBlank()) {
            val assistantMsg = prompt(UUID.randomUUID().toString()) { assistant(accumulatedText) }.messages.first()
            currentPrompt = nextPrompt.copy(
                messages = nextPrompt.messages + assistantMsg
            )
        }
    }

    /**
     * Supports Day 2, 3, 5: Stateless request.
     */
    fun sendSingleRequestStreaming(
        userInput: String,
        overrideParams: LLMParams? = null
    ): Flow<StreamFrame> = flow {
        val params = createParams(overrideParams)
        val singlePrompt = prompt(UUID.randomUUID().toString(), params) {
            system(systemPrompt)
            user(userInput)
        }
        executor.executeStreaming(singlePrompt, model).collect { emit(it) }
    }

    fun clearHistory() {
        currentPrompt = prompt(UUID.randomUUID().toString()) {
            system(systemPrompt)
        }
    }

    fun getHistoryMessages(): List<Message> = currentPrompt.messages

    suspend fun getAvailableModels(): List<LLModel> = executor.models()

    private fun createParams(overrideParams: LLMParams? = null): LLMParams {
        return overrideParams ?: OpenRouterParams(
            temperature = temperature,
            maxTokens = maxTokens,
            stop = stopSequences.takeIf { it.isNotEmpty() }
        )
    }
}
