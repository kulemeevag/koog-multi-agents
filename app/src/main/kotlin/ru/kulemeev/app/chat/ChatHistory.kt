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
     * Always ensures an even number of messages remains (full pairs).
     */
    fun trim(maxPairs: Int) {
        val maxMessages = maxPairs * 2
        
        while (history.size > maxMessages) {
            history.removeAt(0)
        }
        
        // After trimming, ensure we have an even number of messages
        // so that we always start with a User message and end with an Assistant message
        if (history.size % 2 != 0) {
            history.removeAt(0)
        }
    }
}
