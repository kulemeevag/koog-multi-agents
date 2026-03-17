package ru.kulemeev.app.ui

import org.jline.reader.EndOfFileException
import org.jline.reader.LineReader
import org.jline.reader.LineReaderBuilder
import org.jline.reader.UserInterruptException
import org.jline.terminal.TerminalBuilder

class ConsoleUI {
    private val terminal = TerminalBuilder.builder()
        .system(true)
        .dumb(true) // Allow falling back to dumb terminal without warnings
        .build()

    private val lineReader = LineReaderBuilder.builder()
        .terminal(terminal)
        .build()

    private val RESET = "\u001B[0m"
    private val GREEN = "\u001B[32m"
    private val YELLOW = "\u001B[33m"
    private val CYAN = "\u001B[36m"
    private val MAGENTA = "\u001B[35m"
    private val BOLD = "\u001B[1m"

    fun displayWelcomeMessage() {
        println("${BOLD}${GREEN}Welcome to the Koog LLM Chat!$RESET")
        println("${GREEN}Type /help to see available slash commands.$RESET")
    }

    fun displayHelp() {
        println("${YELLOW}Available slash commands:$RESET")
        println("${CYAN}/temp$RESET          - View current temperature")
        println("${CYAN}/temp <value>$RESET  - Set temperature (0.0 to 1.0)")
        println("${CYAN}/system$RESET        - View current system prompt")
        println("${CYAN}/system <text>$RESET - Set system prompt")
        println("${CYAN}/exit$RESET or ${CYAN}/quit$RESET - Exit the application")
        println("${CYAN}/help$RESET          - Show this help message")
    }

    fun displayCurrentTemperature(value: Double) {
        println("${GREEN}Current temperature: $value$RESET")
    }

    fun displayCurrentSystemPrompt(prompt: String) {
        println("${GREEN}Current system prompt: $prompt$RESET")
    }

    fun displayTemperatureSet(value: Double) {
        println("${GREEN}Temperature set to $value$RESET")
    }

    fun displaySystemPromptSet(prompt: String) {
        println("${GREEN}System prompt updated to: $prompt$RESET")
    }

    fun displayError(message: String) {
        println("${MAGENTA}Error: $message$RESET")
    }

    fun displayGenerationCancelled() {
        println("\n${MAGENTA}[Generation stopped by user]$RESET")
    }

    /**
     * Reads input. Returns null if interrupted (Ctrl+C) or EOF (Ctrl+D).
     */
    fun readUserInput(): String? {
        return try {
            if (terminal.type == "dumb") {
                // In IDE "Run" window, we need to be more explicit
                print("> ")
                System.out.flush()
                lineReader.readLine("")
            } else {
                lineReader.readLine("> ")
            }
        } catch (e: UserInterruptException) {
            null // Signal Ctrl+C
        } catch (e: EndOfFileException) {
            null // Signal Ctrl+D or EOF
        }
    }

    fun displayBotMessageChunk(chunk: String) {
        print("$YELLOW$chunk$RESET")
        System.out.flush() // Ensure chunk is visible immediately
    }

    fun displayResponseEnd() {
        println()
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
