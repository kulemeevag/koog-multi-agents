package ru.kulemeev.app

sealed class Message {
    data class User(val content: String) : Message()
    data class Assistant(val content: String) : Message()
}
