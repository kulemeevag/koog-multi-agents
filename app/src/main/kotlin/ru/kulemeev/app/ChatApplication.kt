package ru.kulemeev.app

import ai.koog.prompt.executor.clients.openrouter.OpenRouterLLMClient
import ai.koog.prompt.executor.clients.openrouter.OpenRouterParams
import ai.koog.prompt.llm.LLMCapability
import ai.koog.prompt.llm.LLMProvider
import ai.koog.prompt.llm.LLModel
import ai.koog.prompt.params.LLMParams
import ai.koog.prompt.streaming.StreamFrame
import io.ktor.client.*
import io.ktor.client.plugins.logging.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import ru.kulemeev.app.chat.ChatHistory
import ru.kulemeev.app.chat.LLMResponse
import ru.kulemeev.app.config.ConfigLoader
import ru.kulemeev.app.config.JsonFileConfigLoader
import ru.kulemeev.app.llm.LLMService
import ru.kulemeev.app.ui.ConsoleUI
import sun.misc.Signal
import java.util.concurrent.atomic.AtomicReference
import kotlin.system.exitProcess

class ChatApplication(
    private val configLoader: ConfigLoader = JsonFileConfigLoader()
) {
    private var maxHistoryPairs: Int = 10

    suspend fun run() {
        System.setProperty("org.jline.utils.Log.level", "ERROR")
        val apiKey = System.getenv("OPENROUTER_API_KEY") ?: throw IllegalStateException("OPENROUTER_API_KEY is not set")

        val config = configLoader.loadConfig()
        maxHistoryPairs = config.maxHistoryPairs
        val ui = ConsoleUI()

        val currentJob = AtomicReference<Job?>(null)

        // Handle Ctrl+C (SIGINT)
        Signal.handle(Signal("INT")) {
            val job = currentJob.get()
            if (job != null && job.isActive) {
                job.cancel()
            } else {
                println()
                ui.displayGoodbyeMessage()
                exitProcess(0)
            }
        }

        ui.displayWelcomeMessage()

        val customHttpClient = HttpClient {
            install(Logging) {
                logger = Logger.DEFAULT
                level = LogLevel.INFO
            }
        }

        val client = OpenRouterLLMClient(apiKey, baseClient = customHttpClient)
        
        fun createModel(modelId: String) = LLModel(
            provider = LLMProvider.OpenRouter,
            id = modelId,
            capabilities = listOf(
                LLMCapability.Temperature,
                LLMCapability.Completion,
            )
        )

        val llmService = LLMService(
            client = client,
            model = createModel(config.modelId),
            systemPrompt = config.systemPrompt,
            temperature = config.temperature,
            maxTokens = config.maxTokens,
            stopSequences = config.stopSequences
        )
        val history = ChatHistory()

        while (true) {
            val userInput = ui.readUserInput()

            // JLine returns null on Ctrl+C at prompt
            if (userInput == null) {
                ui.displayGoodbyeMessage()
                break
            }

            if (userInput.isBlank()) continue

            if (userInput.startsWith("/")) {
                val parts = userInput.split(" ", limit = 2)
                val command = parts[0].lowercase()
                val args = if (parts.size > 1) parts[1] else ""

                when (command) {
                    "/exit", "/quit" -> {
                        ui.displayGoodbyeMessage()
                        return
                    }

                    "/help" -> {
                        ui.displayHelp()
                        continue
                    }

                    "/temp" -> {
                        if (args.isBlank()) {
                            ui.displayCurrentTemperature(llmService.temperature)
                        } else {
                            val newTemp = args.toDoubleOrNull()
                            if (newTemp != null && newTemp in 0.0..1.0) {
                                llmService.temperature = newTemp
                                ui.displayTemperatureSet(newTemp)
                            } else {
                                ui.displayError("Invalid temperature. Please provide a value between 0.0 and 1.0")
                            }
                        }
                        continue
                    }

                    "/system" -> {
                        if (args.isBlank()) {
                            ui.displayCurrentSystemPrompt(llmService.systemPrompt)
                        } else {
                            llmService.systemPrompt = args
                            ui.displaySystemPromptSet(args)
                        }
                        continue
                    }

                    "/max_tokens" -> {
                        if (args.isBlank()) {
                            ui.displayCurrentMaxTokens(llmService.maxTokens)
                        } else {
                            val newMax = if (args.lowercase() == "null") null else args.toIntOrNull()
                            if (newMax == null || newMax > 0) {
                                llmService.maxTokens = newMax
                                ui.displayMaxTokensSet(newMax)
                            } else {
                                ui.displayError("Invalid max tokens. Use a positive integer or 'null'")
                            }
                        }
                        continue
                    }

                    "/stop" -> {
                        if (args.isBlank()) {
                            ui.displayCurrentStopSequences(llmService.stopSequences)
                        } else {
                            val newStop = args.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                            llmService.stopSequences = newStop
                            ui.displayStopSequencesSet(newStop)
                        }
                        continue
                    }

                    "/history" -> {
                        if (args.isBlank()) {
                            ui.displayCurrentHistoryPairs(maxHistoryPairs)
                        } else {
                            val newLimit = args.toIntOrNull()
                            if (newLimit != null && newLimit >= 0) {
                                maxHistoryPairs = newLimit
                                ui.displayHistoryPairsSet(newLimit)
                            } else {
                                ui.displayError("Invalid history limit. Please provide a non-negative integer.")
                            }
                        }
                        continue
                    }

                    "/compare" -> {
                        val promptText = args.ifBlank { ui.readComparisonPrompt() }
                        if (promptText.isNullOrBlank()) {
                            ui.displayError("Comparison cancelled: no prompt provided.")
                        } else {
                            val restMaxTokens = ui.readComparisonMaxTokens()
                            val restStop = ui.readComparisonStopSequences()

                            val messages = history.getAll() + ChatMessage.User(promptText)

                            ui.displayCompareHeader("CURRENT PARAMS")
                            executeStreamingRequest(messages, llmService, ui, currentJob, null)

                            ui.displayCompareHeader("RESTRICTED (maxTokens=$restMaxTokens, stop=$restStop)")
                            val restrictedParams = OpenRouterParams(
                                temperature = llmService.temperature,
                                maxTokens = restMaxTokens,
                                stop = restStop
                            )
                            executeStreamingRequest(messages, llmService, ui, currentJob, restrictedParams)
                        }
                        continue
                    }

                    "/config" -> {
                        ui.displayCurrentConfig(
                            modelId = llmService.model.id,
                            temperature = llmService.temperature,
                            systemPrompt = llmService.systemPrompt,
                            maxTokens = llmService.maxTokens,
                            stopSequences = llmService.stopSequences,
                            maxHistoryPairs = maxHistoryPairs
                        )
                        continue
                    }

                    "/clear" -> {
                        history.clear()
                        ui.displayHistoryCleared()
                        continue
                    }

                    "/reset" -> {
                        val freshConfig = configLoader.loadConfig()
                        llmService.model = createModel(freshConfig.modelId)
                        llmService.temperature = freshConfig.temperature
                        llmService.systemPrompt = freshConfig.systemPrompt
                        llmService.maxTokens = freshConfig.maxTokens
                        llmService.stopSequences = freshConfig.stopSequences
                        maxHistoryPairs = freshConfig.maxHistoryPairs
                        ui.displayParametersReset()
                        continue
                    }

                    else -> {
                        ui.displayError("Unknown command: $command. Type /help for assistance.")
                        continue
                    }
                }
            }

            // Normal flow:
            // 1. Trim history to max pairs (ensures we have room for a new pair)
            history.trim(maxHistoryPairs)
            
            // 2. Add new user message (now history has 2*N + 1 messages)
            val userMsg = ChatMessage.User(userInput)
            history.add(userMsg)
            
            // 3. Execute request with full context including the new message
            val response = executeStreamingRequest(history.getAll(), llmService, ui, currentJob, null)

            // 4. Add assistant response if it's not blank (completes the pair)
            if (response.text.isNotBlank()) {
                history.add(ChatMessage.Assistant(response.text, response.finishReason))
            }
        }
    }

    private suspend fun executeStreamingRequest(
        messages: List<ChatMessage>,
        llmService: LLMService,
        ui: ConsoleUI,
        currentJob: AtomicReference<Job?>,
        overrideParams: LLMParams?
    ): LLMResponse = coroutineScope {
        var accumulatedText = ""
        var accumulatedReason: String? = null
        println() // Newline before starting response

        // Launch in the provided scope so it can be tracked
        val streamJob = launch {
            try {
                llmService.streamResponse(messages, overrideParams).collect { frame ->
                    when (frame) {
                        is StreamFrame.TextDelta -> {
                            ui.displayBotMessageChunk(frame.text)
                            accumulatedText += frame.text
                        }

                        is StreamFrame.ReasoningDelta -> ui.displayReasoning(frame.text)
                        is StreamFrame.ToolCallComplete -> ui.displayToolCall(frame.name)
                        is StreamFrame.End -> {
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
}
