package ru.kulemeev.app.chat

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import ru.kulemeev.app.config.ConfigLoader
import ru.kulemeev.app.llm.LLMService
import ru.kulemeev.app.ui.ConsoleUI
import java.util.concurrent.atomic.AtomicReference

interface ChatCommand {
    val name: String
    val aliases: List<String> get() = emptyList()
    val description: String
    suspend fun execute(args: String, context: ChatCommandContext): CommandResult
}

data class ChatCommandContext(
    val ui: ConsoleUI,
    val llmService: LLMService,
    val history: ChatHistory,
    var maxHistoryPairs: Int,
    val configLoader: ConfigLoader,
    val currentJob: AtomicReference<Job?>,
    val onExit: () -> Unit,
    val onHistoryPairsChange: (Int) -> Unit,
    val onModelChange: (String) -> Unit
)

sealed class CommandResult {
    data object Continue : CommandResult()
    data object Handled : CommandResult()
    data object Exit : CommandResult()
}

suspend fun executeStreamingRequestInternal(
    messages: List<ru.kulemeev.app.ChatMessage>,
    llmService: LLMService,
    ui: ConsoleUI,
    currentJob: AtomicReference<Job?>,
    overrideParams: ai.koog.prompt.params.LLMParams?
): LLMResponse = coroutineScope {
    var accumulatedText = ""
    var accumulatedReason: String? = null
    println()

    val scope = this // Explicitly use the coroutineScope
    val streamJob = scope.launch {
        try {
            llmService.streamResponse(messages, overrideParams).collect { frame ->
                when (frame) {
                    is ai.koog.prompt.streaming.StreamFrame.TextDelta -> {
                        ui.displayBotMessageChunk(frame.text)
                        accumulatedText += frame.text
                    }
                    is ai.koog.prompt.streaming.StreamFrame.ReasoningDelta -> ui.displayReasoning(frame.text)
                    is ai.koog.prompt.streaming.StreamFrame.ToolCallComplete -> ui.displayToolCall(frame.name)
                    is ai.koog.prompt.streaming.StreamFrame.End -> {
                        accumulatedReason = frame.finishReason
                    }
                    else -> {}
                }
            }
        } catch (_: CancellationException) {
            ui.displayGenerationCancelled()
        } catch (e: Exception) {
            ui.displayError("Streaming error: ${e.message}")
        }
    }

    currentJob.set(streamJob)
    streamJob.join()
    currentJob.set(null)

    ui.displayResponseEnd()
    ui.displayFinishReason(accumulatedReason)

    LLMResponse(accumulatedText, accumulatedReason)
}

