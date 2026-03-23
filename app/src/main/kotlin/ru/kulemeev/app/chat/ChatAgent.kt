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
 * It encapsulates the interaction logic, history management, and current configuration.
 */
class ChatAgent(
    private val executor: LLMClient,
    var model: LLModel,
    var systemPrompt: String,
    var temperature: Double = 0.7,
    var maxTokens: Int? = null,
    var stopSequences: List<String> = emptyList(),
    var maxHistoryPairs: Int = 10
) {
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
        // 1. Automatic maintenance: Trim history BEFORE adding new exchange.
        // If we want to stay within maxHistoryPairs, we should have (maxHistoryPairs - 1)
        // pairs before adding the current one.
        trimHistory(maxHistoryPairs - 1)

        // 2. Prepare updated prompt
        val params = overrideParams ?: createParams()

        // Always inject the LATEST system prompt and current history
        val historyWithoutSystem = currentPrompt.messages.filter { it.role != Message.Role.System }
        val currentSystemMessage = prompt(UUID.randomUUID().toString()) { system(systemPrompt) }.messages

        val nextPrompt = currentPrompt.copy(
            id = UUID.randomUUID().toString(),
            params = params,
            messages = currentSystemMessage + historyWithoutSystem + prompt(UUID.randomUUID().toString()) { user(userInput) }.messages
        )

        var accumulatedText = ""

        executor.executeStreaming(nextPrompt, model).collect { frame ->
            emit(frame)
            if (frame is StreamFrame.TextDelta) accumulatedText += frame.text
        }

        // 3. Persist the response
        if (accumulatedText.isNotBlank()) {
            val assistantMsg = prompt(UUID.randomUUID().toString()) { assistant(accumulatedText) }.messages.first()
            currentPrompt = nextPrompt.copy(
                messages = nextPrompt.messages + assistantMsg
            )
        }
    }

    fun sendSingleRequestStreaming(
        userInput: String,
        overrideParams: LLMParams? = null
    ): Flow<StreamFrame> = flow {
        val params = overrideParams ?: createParams()
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

    fun trimHistory(maxPairs: Int) {
        val messages = currentPrompt.messages
        val systemMessages = messages.filter { it.role == Message.Role.System }
        val nonSystemMessages = messages.filter { it.role != Message.Role.System }

        val keepCount = maxPairs * 2
        if (nonSystemMessages.size > keepCount) {
            val trimmedNonSystem = nonSystemMessages.takeLast(keepCount)
            currentPrompt = currentPrompt.copy(
                messages = systemMessages + trimmedNonSystem
            )
        }
    }

    fun getHistoryMessages(): List<Message> = currentPrompt.messages
    suspend fun getAvailableModels(): List<LLModel> = executor.models()

    private fun createParams(): LLMParams {
        return OpenRouterParams(
            temperature = temperature,
            maxTokens = maxTokens,
            stop = stopSequences.takeIf { it.isNotEmpty() }
        )
    }
}
