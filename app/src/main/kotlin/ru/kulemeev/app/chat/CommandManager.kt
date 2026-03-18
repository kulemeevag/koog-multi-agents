package ru.kulemeev.app.chat

class CommandManager(private val commands: List<ChatCommand>) {
    private val commandMap: Map<String, ChatCommand> = mutableMapOf<String, ChatCommand>().apply {
        commands.forEach { command ->
            put(command.name.lowercase(), command)
            command.aliases.forEach { alias ->
                put(alias.lowercase(), command)
            }
        }
    }

    fun getAllCommands(): List<ChatCommand> = commands

    suspend fun handleCommand(userInput: String, context: ChatCommandContext): CommandResult {
        if (!userInput.startsWith("/")) return CommandResult.Continue

        val parts = userInput.substring(1).split(" ", limit = 2)
        val commandName = parts[0].lowercase()
        val args = if (parts.size > 1) parts[1] else ""

        val command = commandMap[commandName] ?: return CommandResult.Continue

        return try {
            command.execute(args, context)
            CommandResult.Handled
        } catch (e: Exception) {
            context.ui.displayError("Command execution failed: ${e.message}")
            CommandResult.Handled
        }
    }
}
