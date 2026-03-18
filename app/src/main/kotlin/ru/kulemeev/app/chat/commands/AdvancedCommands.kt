package ru.kulemeev.app.chat.commands

import ai.koog.prompt.executor.clients.openrouter.OpenRouterParams
import ai.koog.prompt.llm.LLMCapability
import ai.koog.prompt.llm.LLMProvider
import ai.koog.prompt.llm.LLModel
import ru.kulemeev.app.ChatMessage
import ru.kulemeev.app.chat.ChatCommand
import ru.kulemeev.app.chat.ChatCommandContext
import ru.kulemeev.app.chat.CommandResult
import ru.kulemeev.app.chat.executeStreamingRequestInternal

class ResetCommand : ChatCommand {
    override val name = "reset"
    override val description = "Reset parameters to config defaults"
    override suspend fun execute(args: String, context: ChatCommandContext): CommandResult {
        val freshConfig = context.configLoader.loadConfig()
        context.llmService.model = LLModel(
            provider = LLMProvider.OpenRouter,
            id = freshConfig.modelId,
            capabilities = listOf(
                LLMCapability.Temperature,
                LLMCapability.Completion,
            )
        )
        context.llmService.temperature = freshConfig.temperature
        context.llmService.systemPrompt = freshConfig.systemPrompt
        context.llmService.maxTokens = freshConfig.maxTokens
        context.llmService.stopSequences = freshConfig.stopSequences
        context.onHistoryPairsChange(freshConfig.maxHistoryPairs)
        context.ui.displayParametersReset()
        return CommandResult.Handled
    }
}

class CompareCommand : ChatCommand {
    override val name = "compare"
    override val description = "Interactive comparison mode"
    override suspend fun execute(args: String, context: ChatCommandContext): CommandResult {
        val promptText = args.ifBlank { context.ui.readComparisonPrompt() }
        if (promptText.isNullOrBlank()) {
            context.ui.displayError("Comparison cancelled: no prompt provided.")
            return CommandResult.Handled
        }

        val restMaxTokens = context.ui.readComparisonMaxTokens()
        val restStop = context.ui.readComparisonStopSequences()

        val messages = context.history.getAll() + ChatMessage.User(promptText)

        context.ui.displayCompareHeader("CURRENT PARAMS")
        executeStreamingRequestInternal(messages, context.llmService, context.ui, context.currentJob, null)

        context.ui.displayCompareHeader("RESTRICTED (maxTokens=$restMaxTokens, stop=$restStop)")
        val restrictedParams = OpenRouterParams(
            temperature = context.llmService.temperature,
            maxTokens = restMaxTokens,
            stop = restStop
        )
        executeStreamingRequestInternal(messages, context.llmService, context.ui, context.currentJob, restrictedParams)

        return CommandResult.Handled
    }
}
