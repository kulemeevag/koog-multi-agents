package ru.kulemeev.app.chat

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import ru.kulemeev.app.ChatMessage

class ChatHistoryTest {

    @Test
    fun `trim should keep exactly maxPairs * 2 messages`() {
        val history = ChatHistory()
        for (i in 1..10) {
            history.add(ChatMessage.User("User $i"))
            history.add(ChatMessage.Assistant("Assistant $i"))
        }
        
        // Total 20 messages (10 pairs)
        assertEquals(20, history.getAll().size)
        
        history.trim(5)
        // Should keep 10 messages (5 pairs)
        assertEquals(10, history.getAll().size)
    }

    @Test
    fun `trim should ensure history starts with a User message`() {
        val history = ChatHistory()
        history.add(ChatMessage.User("U1"))
        history.add(ChatMessage.Assistant("A1"))
        history.add(ChatMessage.User("U2"))
        history.add(ChatMessage.Assistant("A2"))
        
        // We have 2 pairs. Trim to 1 pair.
        history.trim(1)
        
        val all = history.getAll()
        assertEquals(2, all.size)
        assertTrue(all[0] is ChatMessage.User, "First message should be User, but was ${all[0]::class.simpleName}")
        assertEquals("U2", (all[0] as ChatMessage.User).content)
    }

    @Test
    fun `trim should handle odd number of messages correctly and always start with User`() {
        val history = ChatHistory()
        history.add(ChatMessage.User("U1"))
        history.add(ChatMessage.Assistant("A1"))
        history.add(ChatMessage.User("U2")) // 3 messages total
        
        // Trim to 1 pair (max 2 messages)
        history.trim(1)
        
        val all = history.getAll()
        // [U1, A1, U2] -> trim to 2 -> [A1, U2] -> remove non-user -> [U2]
        assertEquals(1, all.size)
        assertTrue(all[0] is ChatMessage.User, "First message should be User, but was ${all[0]::class.simpleName}")
        assertEquals("U2", (all[0] as ChatMessage.User).content)
    }

    @Test
    fun `trim should work when first message becomes Assistant after trim`() {
        val history = ChatHistory()
        history.add(ChatMessage.User("U1"))
        history.add(ChatMessage.Assistant("A1"))
        history.add(ChatMessage.User("U2"))
        history.add(ChatMessage.Assistant("A2"))
        history.add(ChatMessage.User("U3")) // 5 messages
        
        // Trim to 2 pairs (max 4 messages). 
        // 5 > 4 -> remove U1. 
        // Remaining: [A1, U2, A2, U3]. 
        // Size is 4. Even. First is Assistant. This is the bug.
        history.trim(2)
        
        val all = history.getAll()
        assertTrue(all[0] is ChatMessage.User, "After trim, first message must be User, but was ${all[0]::class.simpleName}")
    }

    @Test
    fun `trim with 0 pairs should clear history`() {
        val history = ChatHistory()
        history.add(ChatMessage.User("U1"))
        history.trim(0)
        assertEquals(0, history.getAll().size)
    }
}
