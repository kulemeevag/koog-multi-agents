package ru.kulemeev.app.chat.strategies

import ai.koog.prompt.message.Message

/**
 * Mirror of koog's HistoryCompressionStrategy.
 * Defines how to reduce history size while preserving context.
 */
abstract class HistoryCompressionStrategy {
    /**
     * Compresses the message list.
     * @param originalMessages current history
     * @param compressor a function that can call LLM to generate summary if needed
     * @return new list of messages (compressed)
     */
    abstract suspend fun compress(
        originalMessages: List<Message>,
        compressor: suspend (List<Message>) -> String?
    ): List<Message>

    /**
     * Common logic from koog: keeps system messages and the very first user message.
     */
    protected fun composeMessageHistory(
        originalMessages: List<Message>,
        compressedMessages: List<Message>
    ): List<Message> {
        val result = mutableListOf<Message>()
        
        // 1. Preserve all system messages (persona)
        result.addAll(originalMessages.filterIsInstance<Message.System>())
        
        // 2. Preserve the first user message (initial context)
        originalMessages.firstOrNull { it is Message.User }?.let { result.add(it) }
        
        // 3. Add compressed content (TLDR or truncated list)
        result.addAll(compressedMessages)
        
        return result
    }
}
