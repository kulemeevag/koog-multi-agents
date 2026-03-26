package ru.kulemeev.app.chat.commands

import ru.kulemeev.app.chat.ChatCommand
import ru.kulemeev.app.chat.ChatCommandContext
import ru.kulemeev.app.chat.CommandResult

class ExitCommand : ChatCommand {
    override val name = "exit"
    override val aliases = listOf("quit")
    override val description = "Exit the application"
    override suspend fun execute(args: String, context: ChatCommandContext): CommandResult {
        context.ui.displayGoodbyeMessage()
        context.onExit()
        return CommandResult.Exit
    }
}

class HelpCommand : ChatCommand {
    override val name = "help"
    override val description = "Show this help message"
    override suspend fun execute(args: String, context: ChatCommandContext): CommandResult {
        context.ui.displayHelp()
        return CommandResult.Handled
    }
}

class ClearCommand : ChatCommand {
    override val name = "clear"
    override val description = "Clear chat history"
    override suspend fun execute(args: String, context: ChatCommandContext): CommandResult {
        context.agent.clearHistory()
        context.ui.displayHistoryCleared()
        return CommandResult.Handled
    }
}

class ConfigCommand : ChatCommand {
    override val name = "config"
    override val description = "View all current settings"
    override suspend fun execute(args: String, context: ChatCommandContext): CommandResult {
        context.ui.displayCurrentConfig(
            modelId = context.agent.model.id,
            temperature = context.agent.temperature,
            systemPrompt = context.agent.systemPrompt,
            maxTokens = context.agent.maxTokens,
            stopSequences = context.agent.stopSequences,
            maxHistoryPairs = context.agent.maxHistoryPairs
        )
        return CommandResult.Handled
    }
}

class StatsCommand : ChatCommand {
    override val name = "stats"
    override val description = "View token statistics for the current session"
    override suspend fun execute(args: String, context: ChatCommandContext): CommandResult {
        context.ui.displayResponseStats(
            null, null,
            context.agent.sessionInputTokens,
            context.agent.sessionOutputTokens,
            null,
            context.agent.getHistoryMessages().size
        )
        return CommandResult.Handled
    }
}
