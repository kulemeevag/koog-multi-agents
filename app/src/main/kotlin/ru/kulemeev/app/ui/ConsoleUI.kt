package ru.kulemeev.app.ui

import org.jline.reader.EndOfFileException
import org.jline.reader.LineReaderBuilder
import org.jline.reader.UserInterruptException
import org.jline.terminal.TerminalBuilder
import kotlin.time.Duration

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
        println("${CYAN}/model$RESET          - View current model ID")
        println("${CYAN}/model list$RESET     - List available models")
        println("${CYAN}/model <id>$RESET      - Set model ID")
        println("${CYAN}/temp <value>$RESET   - Set temperature (0.0 to 1.0)")
        println("${CYAN}/system <text>$RESET  - Set system prompt")
        println("${CYAN}/max_tokens <v>$RESET  - Set max tokens")
        println("${CYAN}/history <n>$RESET    - Set max history pairs")
        println("${CYAN}/resume$RESET         - List and resume previous sessions")
        println("${CYAN}/resume <id>$RESET    - Resume specific session")
        println("${CYAN}/stats$RESET          - View session token statistics")
        println("${CYAN}/config$RESET         - View all current settings")
        println("${CYAN}/clear$RESET          - Start a fresh session")
        println("${CYAN}/exit$RESET or ${CYAN}/quit$RESET - Exit")
        println("${CYAN}/help$RESET           - Show this help message")
    }

    fun displaySessionList(sessions: List<String>) {
        println("${BOLD}${YELLOW}=== Available Sessions ===$RESET")
        if (sessions.isEmpty()) {
            println("No saved sessions found.")
        } else {
            sessions.forEachIndexed { index, id ->
                println("${CYAN}[$index] $id$RESET")
            }
        }
        println("${BOLD}${YELLOW}==========================$RESET")
    }

    fun displayFullHistory(messages: List<ai.koog.prompt.message.Message>) {
        println("\n${BOLD}${MAGENTA}=== Session History ===$RESET")
        messages.forEach { msg ->
            val roleColor = when (msg.role) {
                ai.koog.prompt.message.Message.Role.User -> GREEN
                ai.koog.prompt.message.Message.Role.Assistant -> YELLOW
                ai.koog.prompt.message.Message.Role.System -> CYAN
                else -> MAGENTA
            }
            println("${BOLD}$roleColor${msg.role}:$RESET ${msg.content}")
        }
        println("${BOLD}${MAGENTA}=======================\n$RESET")
    }

    fun displayCurrentModel(modelId: String) {
        println("${GREEN}Current model: $modelId$RESET")
    }

    fun displayModelSet(modelId: String) {
        println("${GREEN}Model set to $modelId$RESET")
    }

    fun displayAvailableModels(modelIds: List<String>) {
        println("${BOLD}${YELLOW}=== Available Models ===$RESET")
        modelIds.forEach { id ->
            println("${CYAN}$id$RESET")
        }
        println("${BOLD}${YELLOW}========================$RESET")
    }

    fun displayCurrentConfig(
        modelId: String,
        temperature: Double,
        systemPrompt: String,
        maxTokens: Int?,
        stopSequences: List<String>,
        maxHistoryPairs: Int
    ) {
        println("${BOLD}${YELLOW}=== Current Configuration ===$RESET")
        println("${CYAN}Model ID:$RESET         $modelId")
        println("${CYAN}Temperature:$RESET      $temperature")
        println("${CYAN}Max Tokens:$RESET       ${maxTokens ?: "unlimited"}")
        println("${CYAN}Stop Sequences:$RESET   ${if (stopSequences.isEmpty()) "none" else stopSequences.joinToString(", ")}")
        println("${CYAN}Max History Pairs:$RESET $maxHistoryPairs")
        println("${CYAN}System Prompt:$RESET     $systemPrompt")
        println("${BOLD}${YELLOW}=============================$RESET")
    }

    fun displayCurrentHistoryPairs(value: Int) {
        println("${GREEN}Current history limit: $value pairs$RESET")
    }

    fun displayHistoryPairsSet(value: Int) {
        println("${GREEN}Max history pairs set to $value$RESET")
    }

    fun displayHistoryCleared() {
        println("${GREEN}Chat history has been cleared.$RESET")
    }

    fun displayParametersReset() {
        println("${GREEN}Parameters have been reset to config defaults.$RESET")
    }

    fun readComparisonPrompt(): String? {
        return try {
            lineReader.readLine("${BOLD}${CYAN}Comparison Prompt: $RESET")
        } catch (e: Exception) {
            null
        }
    }

    fun readComparisonMaxTokens(): Int? {
        return try {
            val input = lineReader.readLine("${BOLD}${CYAN}Restriction Max Tokens [default 10]: $RESET")
            if (input.isBlank()) 10 else input.toIntOrNull() ?: 10
        } catch (e: Exception) {
            10
        }
    }

    fun readComparisonStopSequences(): List<String> {
        return try {
            val input = lineReader.readLine("${BOLD}${CYAN}Restriction Stop Sequence [default '.']: $RESET")
            if (input.isBlank()) listOf(".") else input.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        } catch (e: Exception) {
            listOf(".")
        }
    }

    fun displayCurrentMaxTokens(value: Int?) {
        println("${GREEN}Current max tokens: ${value ?: "unlimited"}$RESET")
    }

    fun displayMaxTokensSet(value: Int?) {
        println("${GREEN}Max tokens set to ${value ?: "unlimited"}$RESET")
    }

    fun displayCurrentStopSequences(sequences: List<String>) {
        println("${GREEN}Current stop sequences: ${if (sequences.isEmpty()) "none" else sequences.joinToString(", ")}$RESET")
    }

    fun displayStopSequencesSet(sequences: List<String>) {
        println("${GREEN}Stop sequences updated to: ${if (sequences.isEmpty()) "none" else sequences.joinToString(", ")}$RESET")
    }

    fun displayCompareHeader(title: String) {
        println("\n${BOLD}${MAGENTA}=== $title ===$RESET")
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

    fun displayResponseStats(
        inputTokens: Int?,
        outputTokens: Int?,
        sessionInputTokens: Int,
        sessionOutputTokens: Int,
        duration: Duration?,
        historySize: Int
    ) {
        print("${CYAN}[")
        if (duration != null && duration != Duration.ZERO) {
            print("Time: $duration, ")
        }
        
        if (inputTokens != null && outputTokens != null) {
            print("Tokens (turn): ${inputTokens} in / ${outputTokens} out, ")
        }
        
        print("Tokens (session): ${sessionInputTokens} in / ${sessionOutputTokens} out")
        print(", History: $historySize messages")
        println("]$RESET")
    }

    fun displayFinishReason(reason: String?) {
        if (reason != null && reason != "stop") {
            println("${CYAN}[Terminated: $reason]$RESET")
        }
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
            System.out.flush()
        }
    }
}
