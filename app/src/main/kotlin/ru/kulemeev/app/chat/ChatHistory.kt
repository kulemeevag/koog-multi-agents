package ru.kulemeev.app.chat

import ru.kulemeev.app.ChatMessage

class ChatHistory {
    private val history = mutableListOf<ChatMessage>()

    fun add(chatMessage: ChatMessage) {
        history.add(chatMessage)
    }

    fun addAll(messages: List<ChatMessage>) {
        history.addAll(messages)
    }

    fun replace(messages: List<ChatMessage>) {
        history.clear()
        history.addAll(messages)
    }

    fun getAll(): List<ChatMessage> {
        return history.toList()
    }

    fun clear() {
        history.clear()
    }

    /**
     * Trims the history to the specified maximum number of message pairs.
     * Always ensures the history starts with a User message and ends with an Assistant message (or nothing if empty).
     */
    fun trim(maxPairs: Int) {
        val maxMessages = maxPairs * 2
        
        // If maxPairs is 0, just clear it
        if (maxPairs <= 0) {
            history.clear()
            return
        }

        while (history.size > maxMessages) {
            history.removeAt(0)
        }
        
        // After trimming by size, ensure the first message is a User message.
        // If it's an Assistant message, remove it to maintain pair integrity.
        while (history.isNotEmpty() && history[0] !is ChatMessage.User) {
            history.removeAt(0)
        }
    }
}
