package ru.kulemeev.app.chat.strategies

import ai.koog.prompt.message.Message

/**
 * Implementation of koog's FromLastNMessages strategy.
 * Simply retains the last N messages and preserves system context.
 */
internal class TruncateHistoryStrategy(private val lastN: Int) : HistoryCompressionStrategy() {
    
    override suspend fun compress(
        originalMessages: List<Message>,
        compressor: suspend (List<Message>) -> String?
    ): List<Message> {
        val nonSystemMessages = originalMessages.filter { it.role != Message.Role.System }
        
        if (nonSystemMessages.size <= lastN) return originalMessages
        
        // Take last N messages
        val truncatedMessages = nonSystemMessages.takeLast(lastN)
        
        // Use compose logic to keep system messages and the very first user message
        return composeMessageHistory(originalMessages, truncatedMessages)
    }
}
