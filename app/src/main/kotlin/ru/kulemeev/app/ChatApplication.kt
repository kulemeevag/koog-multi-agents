package ru.kulemeev.app

import ai.koog.prompt.executor.clients.openrouter.OpenRouterLLMClient
import ai.koog.prompt.llm.LLMCapability
import ai.koog.prompt.llm.LLMProvider
import ai.koog.prompt.llm.LLModel
import io.ktor.client.*
import io.ktor.client.plugins.logging.*
import kotlinx.coroutines.Job
import ru.kulemeev.app.chat.*
import ru.kulemeev.app.chat.commands.*
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
    private var isRunning = true

    private val commandManager = CommandManager(
        listOf(
            ExitCommand(),
            HelpCommand(),
            ModelCommand(),
            ClearCommand(),
            ConfigCommand(),
            TempCommand(),
            SystemPromptCommand(),
            MaxTokensCommand(),
            StopSequencesCommand(),
            HistoryLimitCommand(),
            CompareCommand(),
            ResetCommand()
        )
    )

    suspend fun run(
        apiKey: String,
        logLevel: LogLevel = LogLevel.NONE
    ) {
        System.setProperty("org.jline.utils.Log.level", "ERROR")

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
                level = logLevel
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

        val context = ChatCommandContext(
            ui = ui,
            llmService = llmService,
            history = history,
            maxHistoryPairs = maxHistoryPairs,
            configLoader = configLoader,
            currentJob = currentJob,
            onExit = { isRunning = false },
            onHistoryPairsChange = { maxHistoryPairs = it },
            onModelChange = { newModelId ->
                llmService.model = createModel(newModelId)
            }
        )

        while (isRunning) {
            val userInput = ui.readUserInput()

            if (userInput == null) {
                ui.displayGoodbyeMessage()
                break
            }

            if (userInput.isBlank()) continue

            val cmdResult = commandManager.handleCommand(userInput, context)
            if (cmdResult is CommandResult.Exit) break
            if (cmdResult is CommandResult.Handled) continue

            // Normal flow:
            history.trim(maxHistoryPairs)
            val userMsg = ChatMessage.User(userInput)
            history.add(userMsg)

            val response = executeStreamingRequestInternal(history.getAll(), llmService, ui, currentJob, null)

            if (response.text.isNotBlank()) {
                history.add(ChatMessage.Assistant(response.text, response.finishReason))
            }
        }
    }
}
