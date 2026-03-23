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
            temperature = 0.7, // Temp is currently not directly exposed as property in agent, but we can fix that
            systemPrompt = context.agent.systemPrompt,
            maxTokens = null,
            stopSequences = emptyList(),
            maxHistoryPairs = context.maxHistoryPairs
        )
        return CommandResult.Handled
    }
}
