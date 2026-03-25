package ru.kulemeev.app.chat.commands

import ai.koog.prompt.executor.clients.openrouter.OpenRouterParams
import ai.koog.prompt.llm.LLMCapability
import ai.koog.prompt.llm.LLMProvider
import ai.koog.prompt.llm.LLModel
import ru.kulemeev.app.chat.ChatCommand
import ru.kulemeev.app.chat.ChatCommandContext
import ru.kulemeev.app.chat.CommandResult
import ru.kulemeev.app.chat.executeStreamingRequest

class ResetCommand : ChatCommand {
    override val name = "reset"
    override val description = "Reset parameters to config defaults"
    override suspend fun execute(args: String, context: ChatCommandContext): CommandResult {
        val freshConfig = context.configLoader.loadConfig()
        context.agent.model = LLModel(
            provider = LLMProvider.OpenRouter,
            id = freshConfig.modelId,
            capabilities = listOf(
                LLMCapability.Temperature,
                LLMCapability.Completion,
            )
        )
        context.agent.temperature = freshConfig.temperature
        context.agent.systemPrompt = freshConfig.systemPrompt
        context.agent.maxTokens = freshConfig.maxTokens
        context.agent.stopSequences = freshConfig.stopSequences
        context.agent.maxHistoryPairs = freshConfig.maxHistoryPairs
        context.ui.displayParametersReset()
        return CommandResult.Handled
    }
}

class ResumeCommand : ChatCommand {
    override val name = "resume"
    override val description = "List and resume previous sessions"
    override suspend fun execute(args: String, context: ChatCommandContext): CommandResult {
        val sessions = context.agent.listSessions()
        
        if (args.isBlank()) {
            context.ui.displaySessionList(sessions)
            println("Usage: /resume <id> or /resume <index>")
            return CommandResult.Handled
        }

        val targetSession = if (args.toIntOrNull() != null) {
            val index = args.toInt()
            sessions.getOrNull(index)
        } else {
            args.trim()
        }

        if (targetSession != null && sessions.contains(targetSession)) {
            context.agent.resumeSession(targetSession)
            context.ui.displayFullHistory(context.agent.getHistoryMessages())
            println("Resumed session: $targetSession")
        } else {
            context.ui.displayError("Session not found: $args")
        }
        
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

        context.ui.displayCompareHeader("CURRENT PARAMS")
        // Use stateless request for comparison
        executeStreamingRequest(promptText, context.agent, context.ui, context.currentJob, isComparison = true)

        context.ui.displayCompareHeader("RESTRICTED (maxTokens=$restMaxTokens, stop=$restStop)")
        val restrictedParams = OpenRouterParams(
            temperature = context.agent.temperature,
            maxTokens = restMaxTokens,
            stop = restStop
        )
        // Use stateless request with overrides
        executeStreamingRequest(promptText, context.agent, context.ui, context.currentJob, overrideParams = restrictedParams, isComparison = true)

        return CommandResult.Handled
    }
}
