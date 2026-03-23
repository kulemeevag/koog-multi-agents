package ru.kulemeev.app.chat

import ai.koog.prompt.params.LLMParams
import ai.koog.prompt.streaming.StreamFrame
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import ru.kulemeev.app.config.ConfigLoader
import ru.kulemeev.app.ui.ConsoleUI
import java.util.concurrent.atomic.AtomicReference
import kotlin.time.measureTimedValue

interface ChatCommand {
    val name: String
    val aliases: List<String> get() = emptyList()
    val description: String
    suspend fun execute(args: String, context: ChatCommandContext): CommandResult
}

data class ChatCommandContext(
    val ui: ConsoleUI,
    val agent: ChatAgent,
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

suspend fun executeStreamingRequest(
    userInput: String,
    agent: ChatAgent,
    ui: ConsoleUI,
    currentJob: AtomicReference<Job?>,
    overrideParams: LLMParams? = null,
    isComparison: Boolean = false
): LLMResponse = coroutineScope {
    println()

    val (response, duration) = measureTimedValue {
        var accumulatedText = ""
        var accumulatedReason: String? = null
        var inputTokens: Int? = null
        var outputTokens: Int? = null

        val scope = this
        val streamJob = scope.launch {
            try {
                val flow = if (isComparison) {
                    agent.sendSingleRequestStreaming(userInput, overrideParams)
                } else {
                    agent.processMessageStreaming(userInput, overrideParams)
                }

                flow.collect { frame ->
                    when (frame) {
                        is StreamFrame.TextDelta -> {
                            ui.displayBotMessageChunk(frame.text)
                            accumulatedText += frame.text
                        }
                        is StreamFrame.ReasoningDelta -> ui.displayReasoning(frame.text)
                        is StreamFrame.ToolCallComplete -> ui.displayToolCall(frame.name)
                        is StreamFrame.End -> {
                            accumulatedReason = frame.finishReason
                            inputTokens = frame.metaInfo.inputTokensCount
                            outputTokens = frame.metaInfo.outputTokensCount
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

        LLMResponse(accumulatedText, accumulatedReason, inputTokens, outputTokens)
    }

    ui.displayResponseEnd()
    ui.displayResponseStats(response.inputTokens, response.outputTokens, duration, agent.getHistoryMessages().size)
    ui.displayFinishReason(response.finishReason)

    response
}
