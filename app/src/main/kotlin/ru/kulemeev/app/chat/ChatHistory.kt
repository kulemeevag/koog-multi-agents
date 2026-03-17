package ru.kulemeev.app.chat

import ru.kulemeev.app.Message

class ChatHistory {
    private val history = mutableListOf<Message>()

    fun add(message: Message) {
        history.add(message)
    }

    fun getAll(): List<Message> {
        return history.toList()
    }
}
