package ru.kulemeev.app.chat.commands

import ai.koog.prompt.llm.LLMCapability
import ai.koog.prompt.llm.LLMProvider
import ai.koog.prompt.llm.LLModel
import ru.kulemeev.app.chat.ChatCommand
import ru.kulemeev.app.chat.ChatCommandContext
import ru.kulemeev.app.chat.CommandResult

class ModelCommand : ChatCommand {
    override val name = "model"
    override val description = "View, list or set model ID"
    override suspend fun execute(args: String, context: ChatCommandContext): CommandResult {
        when {
            args.isBlank() -> {
                context.ui.displayCurrentModel(context.agent.model.id)
            }
            args.trim().lowercase() == "list" -> {
                try {
                    val modelIds = context.agent.getAvailableModels().map { it.id }
                    context.ui.displayAvailableModels(modelIds)
                } catch (e: Exception) {
                    context.ui.displayError("Failed to fetch models: ${e.message}")
                }
            }
            else -> {
                val newModelId = args.trim()
                context.agent.model = LLModel(
                    provider = LLMProvider.OpenRouter,
                    id = newModelId,
                    capabilities = listOf(
                        LLMCapability.Temperature,
                        LLMCapability.Completion,
                    )
                )
                context.ui.displayModelSet(newModelId)
            }
        }
        return CommandResult.Handled
    }
}

class TempCommand : ChatCommand {
    override val name = "temp"
    override val description = "View or set temperature (0.0 to 1.0)"
    override suspend fun execute(args: String, context: ChatCommandContext): CommandResult {
        if (args.isBlank()) {
            context.ui.displayCurrentTemperature(context.agent.temperature)
        } else {
            val newTemp = args.toDoubleOrNull()
            if (newTemp != null && newTemp in 0.0..1.0) {
                context.agent.temperature = newTemp
                context.ui.displayTemperatureSet(newTemp)
            } else {
                context.ui.displayError("Invalid temperature. Please provide a value between 0.0 and 1.0")
            }
        }
        return CommandResult.Handled
    }
}

class SystemPromptCommand : ChatCommand {
    override val name = "system"
    override val description = "View or set system prompt"
    override suspend fun execute(args: String, context: ChatCommandContext): CommandResult {
        if (args.isBlank()) {
            context.ui.displayCurrentSystemPrompt(context.agent.systemPrompt)
        } else {
            context.agent.systemPrompt = args
            context.ui.displaySystemPromptSet(args)
        }
        return CommandResult.Handled
    }
}

class MaxTokensCommand : ChatCommand {
    override val name = "max_tokens"
    override val description = "View or set max tokens (null for unlimited)"
    override suspend fun execute(args: String, context: ChatCommandContext): CommandResult {
        if (args.isBlank()) {
            context.ui.displayCurrentMaxTokens(context.agent.maxTokens)
        } else {
            val newMax = if (args.lowercase() == "null") null else args.toIntOrNull()
            if (newMax == null || newMax > 0) {
                context.agent.maxTokens = newMax
                context.ui.displayMaxTokensSet(newMax)
            } else {
                context.ui.displayError("Invalid max tokens. Use a positive integer or 'null'")
            }
        }
        return CommandResult.Handled
    }
}

class StopSequencesCommand : ChatCommand {
    override val name = "stop"
    override val description = "View or set stop sequences (comma separated)"
    override suspend fun execute(args: String, context: ChatCommandContext): CommandResult {
        if (args.isBlank()) {
            context.ui.displayCurrentStopSequences(context.agent.stopSequences)
        } else {
            val newStop = args.split(",").map { it.trim() }.filter { it.isNotEmpty() }
            context.agent.stopSequences = newStop
            context.ui.displayStopSequencesSet(newStop)
        }
        return CommandResult.Handled
    }
}

class HistoryLimitCommand : ChatCommand {
    override val name = "history"
    override val description = "View or set max history pairs"
    override suspend fun execute(args: String, context: ChatCommandContext): CommandResult {
        if (args.isBlank()) {
            context.ui.displayCurrentHistoryPairs(context.agent.maxHistoryPairs)
        } else {
            val newLimit = args.toIntOrNull()
            if (newLimit != null && newLimit >= 0) {
                context.agent.maxHistoryPairs = newLimit
                context.ui.displayHistoryPairsSet(newLimit)
            } else {
                context.ui.displayError("Invalid history limit. Please provide a non-negative integer.")
            }
        }
        return CommandResult.Handled
    }
}
