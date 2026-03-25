package ru.kulemeev.app.chat

import ai.koog.prompt.message.Message
import kotlinx.serialization.json.Json
import java.io.File

/**
 * JSONL implementation using standard kotlinx.serialization.
 */
internal class JsonlHistoryStorage(
    private val sessionsDir: String = "sessions"
) : HistoryStorage {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    init {
        File(sessionsDir).mkdirs()
    }

    private fun getFile(sessionId: String) = File(sessionsDir, "$sessionId.jsonl")

    override fun appendMessage(sessionId: String, message: Message) {
        try {
            val jsonLine = json.encodeToString(message)
            getFile(sessionId).appendText(jsonLine + "\n")
        } catch (e: Exception) {
            System.err.println("Failed to append message: ${e.message}")
        }
    }

    override fun loadHistory(sessionId: String): List<Message> {
        val file = getFile(sessionId)
        if (!file.exists()) return emptyList()

        return try {
            file.readLines()
                .filter { it.isNotBlank() }
                .map { json.decodeFromString<Message>(it) }
        } catch (e: Exception) {
            System.err.println("Failed to load history for $sessionId: ${e.message}")
            emptyList()
        }
    }

    override fun saveFullHistory(sessionId: String, messages: List<Message>) {
        try {
            val content = messages.joinToString("\n") { json.encodeToString(it) }
            getFile(sessionId).writeText(if (content.isEmpty()) "" else content + "\n")
        } catch (e: Exception) {
            System.err.println("Failed to save full history: ${e.message}")
        }
    }

    override fun listSessions(): List<String> {
        return File(sessionsDir).listFiles { f -> f.extension == "jsonl" }
            ?.map { it.nameWithoutExtension }
            ?.sortedDescending() ?: emptyList()
    }

    override fun deleteSession(sessionId: String) {
        getFile(sessionId).delete()
    }
}
