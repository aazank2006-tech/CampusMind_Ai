package com.example.data.repository

import com.example.domain.model.Chat
import com.example.domain.model.Message
import com.example.domain.model.Role
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

sealed interface StreamEvent {
    data class Thinking(val chunk: String) : StreamEvent
    data class Token(val text: String) : StreamEvent
    data class ToolCall(val toolName: String, val arguments: String) : StreamEvent
    data object Done : StreamEvent
    data class Error(val message: String) : StreamEvent
}

interface ChatRepository {
    fun getChats(): Flow<List<Chat>>
    fun getMessages(chatId: String): Flow<List<Message>>
    suspend fun createNewChat(initialTitle: String = "New conversation"): Chat
    suspend fun pinChat(chatId: String, pinned: Boolean)
    suspend fun renameChat(chatId: String, newTitle: String)
    suspend fun deleteChat(chatId: String): Chat?
    suspend fun restoreChat(chat: Chat)
    suspend fun addMessage(message: Message)
    suspend fun updateMessage(message: Message)
    suspend fun deleteMessagesAfter(chatId: String, messageId: String)
    suspend fun clearAllData()
    fun streamChatReply(chatId: String, userPrompt: String, isPro: Boolean): Flow<StreamEvent>
}

class FakeChatRepository : ChatRepository {

    private val chatsFlow = MutableStateFlow<List<Chat>>(
        listOf(
            Chat(
                id = "chat_pinned_1",
                title = "CS 201: Sorting & Complexity",
                isPinned = true,
                createdAt = System.currentTimeMillis() - 86400000 * 2,
                updatedAt = System.currentTimeMillis() - 86400000 * 2
            ),
            Chat(
                id = "chat_pinned_2",
                title = "Research Paper Thesis Draft",
                isPinned = true,
                createdAt = System.currentTimeMillis() - 86400000 * 4,
                updatedAt = System.currentTimeMillis() - 86400000 * 4
            ),
            Chat(
                id = "chat_recent_1",
                title = "Binary Search Tree in Kotlin",
                isPinned = false,
                createdAt = System.currentTimeMillis() - 3600000 * 3,
                updatedAt = System.currentTimeMillis() - 3600000 * 3
            ),
            Chat(
                id = "chat_recent_2",
                title = "Finals Revision Week Schedule",
                isPinned = false,
                createdAt = System.currentTimeMillis() - 3600000 * 12,
                updatedAt = System.currentTimeMillis() - 3600000 * 12
            ),
            Chat(
                id = "chat_recent_3",
                title = "C++ Pointer Arithmetic & Lifetime",
                isPinned = false,
                createdAt = System.currentTimeMillis() - 86400000,
                updatedAt = System.currentTimeMillis() - 86400000
            )
        )
    )

    private val messagesMap = MutableStateFlow<Map<String, List<Message>>>(emptyMap())

    override fun getChats(): Flow<List<Chat>> = chatsFlow.asStateFlow()

    override fun getMessages(chatId: String): Flow<List<Message>> {
        return messagesMap.map { it[chatId] ?: emptyList() }
    }

    override suspend fun createNewChat(initialTitle: String): Chat {
        val newChat = Chat(title = initialTitle)
        chatsFlow.value = listOf(newChat) + chatsFlow.value
        return newChat
    }

    override suspend fun pinChat(chatId: String, pinned: Boolean) {
        chatsFlow.value = chatsFlow.value.map {
            if (it.id == chatId) it.copy(isPinned = pinned, updatedAt = System.currentTimeMillis()) else it
        }
    }

    override suspend fun renameChat(chatId: String, newTitle: String) {
        chatsFlow.value = chatsFlow.value.map {
            if (it.id == chatId) it.copy(title = newTitle.trim(), updatedAt = System.currentTimeMillis()) else it
        }
    }

    override suspend fun deleteChat(chatId: String): Chat? {
        val chatToDelete = chatsFlow.value.find { it.id == chatId }
        chatsFlow.value = chatsFlow.value.filterNot { it.id == chatId }
        return chatToDelete
    }

    override suspend fun restoreChat(chat: Chat) {
        chatsFlow.value = (listOf(chat) + chatsFlow.value).sortedByDescending { it.updatedAt }
    }

    override suspend fun addMessage(message: Message) {
        val current = messagesMap.value.toMutableMap()
        val list = (current[message.chatId] ?: emptyList()) + message
        current[message.chatId] = list
        messagesMap.value = current
    }

    override suspend fun updateMessage(message: Message) {
        val current = messagesMap.value.toMutableMap()
        val list = current[message.chatId] ?: return
        current[message.chatId] = list.map { if (it.id == message.id) message else it }
        messagesMap.value = current
    }

    override suspend fun deleteMessagesAfter(chatId: String, messageId: String) {
        val current = messagesMap.value.toMutableMap()
        val list = current[chatId] ?: return
        val targetIndex = list.indexOfFirst { it.id == messageId }
        if (targetIndex != -1) {
            current[chatId] = list.take(targetIndex + 1)
            messagesMap.value = current
        }
    }

    override suspend fun clearAllData() {
        chatsFlow.value = emptyList()
        messagesMap.value = emptyMap()
    }

    override fun streamChatReply(chatId: String, userPrompt: String, isPro: Boolean): Flow<StreamEvent> = flow {
        // Step 1: Emit thinking thoughts (especially prominent in Pro mode)
        if (isPro) {
            val thinkingSteps = listOf(
                "Analyzing prompt: \"$userPrompt\"...",
                " Identifying core concepts and algorithmic requirements...",
                " Formulating comparison matrix and asymptotic bounds...",
                " Generating verified implementation with edge-case handling."
            )
            for (step in thinkingSteps) {
                delay(120)
                emit(StreamEvent.Thinking(step))
            }
            delay(150)
        }

        // Step 2: Canned rich Markdown reply tailored to student academic workflow
        val replyMarkdown = buildString {
            append("## Understanding ")
            append(userPrompt.take(35))
            append("\n\n")
            append("Here is a comprehensive breakdown to help you master this concept for your coursework:\n\n")
            append("### Key Takeaways\n")
            append("- **Efficiency**: Choose algorithms that scale gracefully with input size N.\n")
            append("- **Memory Tradeoff**: In-place algorithms save memory at the cost of recursive stack overhead.\n")
            append("- **Edge Cases**: Always handle empty inputs, single elements, and duplicated keys.\n\n")
            append("### Algorithm Complexity Comparison\n\n")
            append("| Algorithm | Best Case | Average Case | Worst Case | Space |\n")
            append("| :--- | :--- | :--- | :--- | :--- |\n")
            append("| Quick Sort | O(n log n) | O(n log n) | O(n²) | O(log n) |\n")
            append("| Merge Sort | O(n log n) | O(n log n) | O(n log n) | O(n) |\n")
            append("| Heap Sort  | O(n log n) | O(n log n) | O(n log n) | O(1) |\n")
            append("| Tim Sort   | O(n)       | O(n log n) | O(n log n) | O(n) |\n\n")
            append("### Practical Implementation\n\n")
            append("Here is an optimized, clean implementation with inline docstrings:\n\n")
            append("```python\n")
            append("def partition(arr: list[int], low: int, high: int) -> int:\n")
            append("    \"\"\"Lomuto partition scheme with pivot at high index.\"\"\"\n")
            append("    pivot = arr[high]\n")
            append("    i = low - 1\n")
            append("    for j in range(low, high):\n")
            append("        if arr[j] <= pivot:\n")
            append("            i += 1\n")
            append("            arr[i], arr[j] = arr[j], arr[i]\n")
            append("    arr[i + 1], arr[high] = arr[high], arr[i + 1]\n")
            append("    return i + 1\n\n")
            append("def quick_sort(arr: list[int], low: int = 0, high: int | None = None) -> None:\n")
            append("    if high is None:\n")
            append("        high = len(arr) - 1\n")
            append("    if low < high:\n")
            append("        pi = partition(arr, low, high)\n")
            append("        quick_sort(arr, low, pi - 1)\n")
            append("        quick_sort(arr, pi + 1, high)\n\n")
            append("# Quick sanity test\n")
            append("numbers = [64, 34, 25, 12, 22, 11, 90]\n")
            append("quick_sort(numbers)\n")
            append("print(\"Sorted array:\", numbers)\n")
            append("```\n\n")
            append("> **Study Tip**: When writing exam explanations, always cite both time complexity and auxiliary space to secure full marks.\n")
        }

        // Step 3: Stream tokens in chunks every 35ms (satisfying performance requirement)
        val tokenChunks = replyMarkdown.chunked(6)
        for (chunk in tokenChunks) {
            delay(35)
            emit(StreamEvent.Token(chunk))
        }

        emit(StreamEvent.Done)
    }
}
