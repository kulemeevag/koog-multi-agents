package ru.kulemeev.app.ui

import java.util.*

class ConsoleUI {
    private val scanner = Scanner(System.`in`)

    private val RESET = "\u001B[0m"
    private val GREEN = "\u001B[32m"
    private val YELLOW = "\u001B[33m"
    private val CYAN = "\u001B[36m"
    private val MAGENTA = "\u001B[35m"

    fun displayWelcomeMessage() {
        println("${GREEN}Welcome to the Koog LLM Chat!$RESET")
        println("${GREEN}Type 'exit' or 'quit' to end the session.$RESET")
    }

    fun getTemperature(): Double {
        println("${CYAN}Enter temperature (default: 0.7):$RESET")
        print("> ")
        return scanner.nextLine().takeIf { it.isNotBlank() }?.toDoubleOrNull() ?: 0.7
    }

    fun readUserInput(): String {
        println()
        print("> ")
        return scanner.nextLine()
    }

    fun displayBotMessageChunk(chunk: String) {
        print("$YELLOW$chunk$RESET")
    }

    fun displayGoodbyeMessage() {
        println("${GREEN}Exiting the chatbot. Goodbye!$RESET")
    }

    fun displayToolCall(toolName: String) {
        println("${MAGENTA}Tool call: $toolName$RESET")
    }

    fun displayReasoning(reasoning: String?) {
        if (reasoning != null) {
            print("${CYAN}[Reasoning] $reasoning$RESET")
        }
    }
}
