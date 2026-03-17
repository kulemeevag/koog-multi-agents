package ru.kulemeev.app

import ai.koog.prompt.executor.ollama.client.OllamaClient
import ai.koog.prompt.streaming.StreamFrame
import kotlinx.coroutines.runBlocking
import ru.kulemeev.app.chat.ChatHistory
import ru.kulemeev.app.llm.LLMService
import ru.kulemeev.app.ui.ConsoleUI

import io.ktor.client.*
import io.ktor.client.plugins.logging.*

class ChatApplication {
    fun run() = runBlocking {
        val ui = ConsoleUI()
        ui.displayWelcomeMessage()

        val temperature = ui.getTemperature()
        val systemPrompt =
            "Ты — полезный AI-ассистент. Отвечай кратко и по делу, максимум одно предложение. Отвечай только на том языке, котором тебя спросили"

        val customHttpClient = HttpClient {
            install(Logging) {
                logger = Logger.DEFAULT
                level = LogLevel.HEADERS
            }
        }
        val client = OllamaClient(baseClient = customHttpClient)
        val llmService = LLMService(client, systemPrompt, temperature)
        val history = ChatHistory()

        while (true) {
            val userInput = ui.readUserInput()
            if (userInput.isBlank()) continue
            if (userInput == "exit" || userInput == "quit") {
                ui.displayGoodbyeMessage()
                break
            }

            history.add(Message.User(userInput))

            val response = StringBuilder()
            llmService.streamResponse(history.getAll()).collect { frame ->
                when (frame) {
                    is StreamFrame.TextDelta -> {
                        ui.displayBotMessageChunk(frame.text)
                        response.append(frame.text)
                    }

                    is StreamFrame.ReasoningDelta -> ui.displayReasoning(frame.text)
                    is StreamFrame.ToolCallComplete -> ui.displayToolCall(frame.name)
                    is StreamFrame.End -> {}
                    else -> {}
                }
            }

            if (response.isNotBlank()) {
                history.add(Message.Assistant(response.toString()))
            }
        }
    }
}
