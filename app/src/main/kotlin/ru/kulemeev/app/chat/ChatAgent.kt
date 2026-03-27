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
import ru.kulemeev.app.chat.strategies.HistoryCompressionStrategy
import ru.kulemeev.app.chat.strategies.TruncateHistoryStrategy
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

    var sessionInputTokens: Int = 0
        private set
    var sessionOutputTokens: Int = 0
        private set

    init {
        // Start with a CLEAN session by default (like ClaudeCode/Gemini)
        currentSessionId = generateNewSessionId()
        currentPrompt = createInitialPrompt()
        // Save initial system prompt to the new session file
        currentPrompt.messages.forEach { storage.appendMessage(currentSessionId, it) }
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
            sessionInputTokens = 0
            sessionOutputTokens = 0
        }
    }

    /**
     * Main entry point for chat.
     */
    fun processMessageStreaming(
        userInput: String,
        overrideParams: LLMParams? = null
    ): Flow<StreamFrame> = flow {
        // Day 9: Auto-compression logic
        // If history is getting long, use LLM to summarize it before proceeding
        val currentMessages = currentPrompt.messages.filter { it.role != Message.Role.System }
        if (currentMessages.size >= 10) {
            // Only compress if we don't already have a very recent TLDR to avoid loops
            val lastMsgContent = currentMessages.lastOrNull()?.content ?: ""
            if (!lastMsgContent.contains("[CONVERSATION TL;DR]") && !lastMsgContent.contains("I have summarized")) {
                compressHistory(ru.kulemeev.app.chat.strategies.WholeHistorySummaryStrategy())
            }
        }

        // Safety net: Proactive maintenance using Truncate Strategy (FromLastNMessages in koog)
        compressHistory(TruncateHistoryStrategy(maxHistoryPairs * 2))

        val params = overrideParams ?: createParams()
        
        // Ensure strictly one system message at the start
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
            if (frame is StreamFrame.TextDelta) {
                accumulatedText += frame.text
            } else if (frame is StreamFrame.End) {
                sessionInputTokens += frame.metaInfo.inputTokensCount ?: 0
                sessionOutputTokens += frame.metaInfo.outputTokensCount ?: 0
            }
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
        executor.executeStreaming(singlePrompt, model).collect { frame ->
            emit(frame)
            if (frame is StreamFrame.End) {
                sessionInputTokens += frame.metaInfo.inputTokensCount ?: 0
                sessionOutputTokens += frame.metaInfo.outputTokensCount ?: 0
            }
        }
    }

    fun clearHistory() {
        currentSessionId = generateNewSessionId()
        currentPrompt = createInitialPrompt()
        currentPrompt.messages.forEach { storage.appendMessage(currentSessionId, it) }
        sessionInputTokens = 0
        sessionOutputTokens = 0
    }

    fun getHistoryMessages(): List<Message> = currentPrompt.messages
    fun listSessions(): List<String> = storage.listSessions()
    suspend fun getAvailableModels(): List<LLModel> = executor.models()

    /**
     * Executes context compression using the specified strategy.
     * This follows the same architecture as koog's compression.
     */
    suspend fun compressHistory(strategy: HistoryCompressionStrategy) {
        val messages = currentPrompt.messages
        
        val compressedMessages = strategy.compress(messages) { history ->
            generateSummary(history)
        }
        
        if (compressedMessages != messages) {
            currentPrompt = currentPrompt.copy(messages = compressedMessages)
            storage.saveFullHistory(currentSessionId, currentPrompt.messages)
        }
    }

    private suspend fun generateSummary(history: List<Message>): String? {
        val tldrPromptText = """
            Briefly summarize our conversation so far into a TL;DR.
            Focus on key objectives, findings, and current status.
            Format with:
            - Objectives
            - Findings
            - Status
        """.trimIndent()

        val tempPrompt = prompt(UUID.randomUUID().toString(), createParams()) {
            history.filter { it.role != Message.Role.System }.forEach { msg ->
                when (msg.role) {
                    Message.Role.User -> user(msg.content)
                    Message.Role.Assistant -> assistant(msg.content)
                    else -> {}
                }
            }
            user(tldrPromptText)
        }

        var result = ""
        try {
            executor.executeStreaming(tempPrompt, model).collect { frame ->
                if (frame is StreamFrame.TextDelta) {
                    result += frame.text
                } else if (frame is StreamFrame.End) {
                    // Accumulate cost of summarization itself
                    sessionInputTokens += frame.metaInfo.inputTokensCount ?: 0
                    sessionOutputTokens += frame.metaInfo.outputTokensCount ?: 0
                }
            }
        } catch (e: Exception) {
            return null
        }
        return result.takeIf { it.isNotBlank() }
    }

    private fun generateNewSessionId(): String {
        return SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(Date())
    }

    private fun createInitialPrompt(): Prompt {
        return prompt(UUID.randomUUID().toString()) {
            system(systemPrompt)
        }
    }

    private fun createParams(): LLMParams {
        return OpenRouterParams(
            temperature = temperature,
            maxTokens = maxTokens,
            stop = stopSequences.takeIf { it.isNotEmpty() }
        )
    }
}
