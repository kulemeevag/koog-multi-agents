package ru.kulemeev.app.chat

import ai.koog.prompt.message.Message

/**
 * Interface for history persistence.
 */
interface HistoryStorage {
    fun appendMessage(sessionId: String, message: Message)
    fun loadHistory(sessionId: String): List<Message>
    fun saveFullHistory(sessionId: String, messages: List<Message>)
    fun listSessions(): List<String>
    fun deleteSession(sessionId: String)
}
