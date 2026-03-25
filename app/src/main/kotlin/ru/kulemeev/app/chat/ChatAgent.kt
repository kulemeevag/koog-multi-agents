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
import java.text.SimpleDateFormat
import java.util.*

/**
 * ChatAgent encapsulates logic, history, and multi-session persistence.
 */
class ChatAgent(
    private val executor: LLMClient,
    var model: LLModel,
    var systemPrompt: String,
    var temperature: Double = 0.7,
    var maxTokens: Int? = null,
    var stopSequences: List<String> = emptyList(),
    var maxHistoryPairs: Int = 10,
    private val storage: HistoryStorage = JsonlHistoryStorage()
) {
    private var currentPrompt: Prompt
    var currentSessionId: String
        private set

    init {
        // Start with a CLEAN session by default (like ClaudeCode/Gemini)
        currentSessionId = generateNewSessionId()
        currentPrompt = createInitialPrompt()
        // Save initial system prompt to the new session file
        currentPrompt.messages.forEach { storage.appendMessage(currentSessionId, it) }
    }

    private fun generateNewSessionId(): String {
        return SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(Date())
    }

    private fun createInitialPrompt(): Prompt {
        return prompt(UUID.randomUUID().toString()) {
            system(systemPrompt)
        }
    }

    /**
     * Resumes a previous session from storage.
     */
    fun resumeSession(sessionId: String) {
        val savedMessages = storage.loadHistory(sessionId)
        if (savedMessages.isNotEmpty()) {
            currentSessionId = sessionId
            // Reconstruct prompt from saved messages
            currentPrompt = prompt(UUID.randomUUID().toString()) {}.copy(messages = savedMessages)
        }
    }

    /**
     * Main entry point for chat.
     */
    fun processMessageStreaming(
        userInput: String,
        overrideParams: LLMParams? = null
    ): Flow<StreamFrame> = flow {
        // 1. Proactive maintenance: trim before adding current turn
        trimHistory(maxHistoryPairs - 1)

        val params = overrideParams ?: createParams()
        
        // Inject latest system prompt
        val historyWithoutSystem = currentPrompt.messages.filter { it.role != Message.Role.System }
        val currentSystemMessage = prompt(UUID.randomUUID().toString()) { system(systemPrompt) }.messages
        
        // User message
        val userMessages = prompt(UUID.randomUUID().toString()) { user(userInput) }.messages
        userMessages.forEach { storage.appendMessage(currentSessionId, it) }

        val nextPrompt = currentPrompt.copy(
            id = UUID.randomUUID().toString(),
            params = params,
            messages = currentSystemMessage + historyWithoutSystem + userMessages
        )

        var accumulatedText = ""
        executor.executeStreaming(nextPrompt, model).collect { frame ->
            emit(frame)
            if (frame is StreamFrame.TextDelta) accumulatedText += frame.text
        }

        if (accumulatedText.isNotBlank()) {
            val assistantMsg = prompt(UUID.randomUUID().toString()) { assistant(accumulatedText) }.messages.first()
            storage.appendMessage(currentSessionId, assistantMsg)
            currentPrompt = nextPrompt.copy(messages = nextPrompt.messages + assistantMsg)
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
        currentSessionId = generateNewSessionId()
        currentPrompt = createInitialPrompt()
        currentPrompt.messages.forEach { storage.appendMessage(currentSessionId, it) }
    }

    fun trimHistory(maxPairs: Int) {
        val messages = currentPrompt.messages
        val systemMessages = messages.filter { it.role == Message.Role.System }
        val nonSystemMessages = messages.filter { it.role != Message.Role.System }
        
        val keepCount = maxPairs * 2
        if (nonSystemMessages.size > keepCount) {
            val trimmedNonSystem = nonSystemMessages.takeLast(keepCount)
            currentPrompt = currentPrompt.copy(messages = systemMessages + trimmedNonSystem)
            storage.saveFullHistory(currentSessionId, currentPrompt.messages)
        }
    }

    fun getHistoryMessages(): List<Message> = currentPrompt.messages
    fun listSessions(): List<String> = storage.listSessions()
    suspend fun getAvailableModels(): List<LLModel> = executor.models()

    private fun createParams(): LLMParams {
        return OpenRouterParams(
            temperature = temperature,
            maxTokens = maxTokens,
            stop = stopSequences.takeIf { it.isNotEmpty() }
        )
    }
}
