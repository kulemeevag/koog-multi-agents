package ru.kulemeev.app

import ai.koog.prompt.executor.clients.openrouter.OpenRouterLLMClient
import ai.koog.prompt.llm.LLMCapability
import ai.koog.prompt.llm.LLMProvider
import ai.koog.prompt.llm.LLModel
import ai.koog.prompt.streaming.StreamFrame
import io.ktor.client.*
import io.ktor.client.plugins.logging.*
import kotlinx.coroutines.*
import ru.kulemeev.app.chat.ChatHistory
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
    fun run() = runBlocking {
        System.setProperty("org.jline.utils.Log.level", "ERROR")
        val apiKey = System.getenv("OPENROUTER_API_KEY") ?: throw IllegalStateException("OPENROUTER_API_KEY is not set")

        val config = configLoader.loadConfig()
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

        val temperature = config.temperature
        val systemPrompt = config.systemPrompt

        val customHttpClient = HttpClient {
            install(Logging) {
                logger = Logger.DEFAULT
                level = LogLevel.INFO
            }
        }

        val client = OpenRouterLLMClient(apiKey, baseClient = customHttpClient)
        val llmModel = LLModel(
            provider = LLMProvider.OpenRouter,
            id = config.modelId,
            capabilities = listOf(
                LLMCapability.Temperature,
                LLMCapability.Completion,
            )
        )

        val llmService = LLMService(client, llmModel, systemPrompt, temperature)
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
                        return@runBlocking
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
                    else -> {
                        ui.displayError("Unknown command: $command. Type /help for assistance.")
                        continue
                    }
                }
            }

            history.add(ChatMessage.User(userInput))

            val response = StringBuilder()
            println() // Newline before starting response

            // Run streaming in a separate job to make it cancellable via Signal handler
            val streamJob = launch {
                try {
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
                } catch (e: CancellationException) {
                    ui.displayGenerationCancelled()
                }
            }

            currentJob.set(streamJob)
            streamJob.join()
            currentJob.set(null)

            ui.displayResponseEnd()

            if (response.isNotBlank()) {
                history.add(ChatMessage.Assistant(response.toString()))
            }
        }
    }
}
