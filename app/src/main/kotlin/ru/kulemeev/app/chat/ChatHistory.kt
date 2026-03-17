package ru.kulemeev.app.chat

import ru.kulemeev.app.ChatMessage

class ChatHistory {
    private val history = mutableListOf<ChatMessage>()

    fun add(chatMessage: ChatMessage) {
        history.add(chatMessage)
    }

    fun getAll(): List<ChatMessage> {
        return history.toList()
    }
}
