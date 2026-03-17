package ru.kulemeev.app

sealed class ChatMessage {
    data class User(val content: String) : ChatMessage()
    data class Assistant(val content: String) : ChatMessage()
}
