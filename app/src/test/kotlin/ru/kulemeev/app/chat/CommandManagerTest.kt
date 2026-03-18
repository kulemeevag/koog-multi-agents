package ru.kulemeev.app.chat

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class CommandManagerTest {

    @Test
    fun `handleCommand should resolve exact command name`(): Unit = runBlocking {
        val mockCommand = mock<ChatCommand>()
        whenever(mockCommand.name).thenReturn("test")

        val manager = CommandManager(listOf(mockCommand))
        val context = mock<ChatCommandContext>()

        manager.handleCommand("/test args", context)

        verify(mockCommand).execute("args", context)
    }

    @Test
    fun `handleCommand should resolve alias`(): Unit = runBlocking {
        val mockCommand = mock<ChatCommand>()
        whenever(mockCommand.name).thenReturn("test")
        whenever(mockCommand.aliases).thenReturn(listOf("t"))

        val manager = CommandManager(listOf(mockCommand))
        val context = mock<ChatCommandContext>()

        manager.handleCommand("/t args", context)

        verify(mockCommand).execute("args", context)
    }

    @Test
    fun `handleCommand should return Continue for non-slash input`() = runBlocking {
        val manager = CommandManager(emptyList())
        val context = mock<ChatCommandContext>()

        val result = manager.handleCommand("not a command", context)

        assertEquals(CommandResult.Continue, result)
    }
}
