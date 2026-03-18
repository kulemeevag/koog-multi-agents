package ru.kulemeev.app.chat.commands

import ru.kulemeev.app.chat.ChatCommand
import ru.kulemeev.app.chat.ChatCommandContext
import ru.kulemeev.app.chat.CommandResult

class TempCommand : ChatCommand {
    override val name = "temp"
    override val description = "View or set temperature (0.0 to 1.0)"
    override suspend fun execute(args: String, context: ChatCommandContext): CommandResult {
        if (args.isBlank()) {
            context.ui.displayCurrentTemperature(context.llmService.temperature)
        } else {
            val newTemp = args.toDoubleOrNull()
            if (newTemp != null && newTemp in 0.0..1.0) {
                context.llmService.temperature = newTemp
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
            context.ui.displayCurrentSystemPrompt(context.llmService.systemPrompt)
        } else {
            context.llmService.systemPrompt = args
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
            context.ui.displayCurrentMaxTokens(context.llmService.maxTokens)
        } else {
            val newMax = if (args.lowercase() == "null") null else args.toIntOrNull()
            if (newMax == null || newMax > 0) {
                context.llmService.maxTokens = newMax
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
            context.ui.displayCurrentStopSequences(context.llmService.stopSequences)
        } else {
            val newStop = args.split(",").map { it.trim() }.filter { it.isNotEmpty() }
            context.llmService.stopSequences = newStop
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
            context.ui.displayCurrentHistoryPairs(context.maxHistoryPairs)
        } else {
            val newLimit = args.toIntOrNull()
            if (newLimit != null && newLimit >= 0) {
                context.onHistoryPairsChange(newLimit)
                context.ui.displayHistoryPairsSet(newLimit)
            } else {
                context.ui.displayError("Invalid history limit. Please provide a non-negative integer.")
            }
        }
        return CommandResult.Handled
    }
}
