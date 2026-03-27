package ru.kulemeev.app.chat.strategies

import ai.koog.prompt.dsl.prompt
import ai.koog.prompt.message.Message
import java.util.*

/**
 * Enhanced implementation that uses two-step confirmation (Assistant + User)
 * to prevent model looping. This is the recommended pattern in koog.
 */
internal class WholeHistorySummaryStrategy : HistoryCompressionStrategy() {
    
    override suspend fun compress(
        originalMessages: List<Message>,
        compressor: suspend (List<Message>) -> String?
    ): List<Message> {
        // Lower threshold for testing: compress if we have at least 2 non-system messages
        val nonSystemCount = originalMessages.count { it.role != Message.Role.System }
        if (nonSystemCount < 2) return originalMessages

        val summaryText = compressor(originalMessages) ?: return originalMessages

        // Create Assistant summary
        val assistantSummary = prompt(UUID.randomUUID().toString()) {
            assistant("I have summarized our previous conversation to save context space. Here is the summary:\n\n$summaryText")
        }.messages.first()

        // Create User confirmation (The "Anchor" that prevents loops)
        val userConfirmation = prompt(UUID.randomUUID().toString()) {
            user("Thank you. This summary accurately reflects our discussion. Let's continue based on this context.")
        }.messages.first()

        // Use compose logic to build new message list
        return composeMessageHistory(originalMessages, listOf(assistantSummary, userConfirmation))
    }
}
