package com.example.data.repository

import com.example.data.local.dao.ChatDao
import com.example.data.local.entities.ChatEntity
import com.example.data.local.entities.MessageEntity
import com.example.domain.model.Chat
import com.example.domain.model.Message
import com.example.domain.model.MessageStatus
import com.example.domain.model.Role
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class OfflineFirstChatRepository(
    private val chatDao: ChatDao,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) : ChatRepository {

    init {
        scope.launch {
            seedInitialDataIfNeeded()
        }
    }

    private suspend fun seedInitialDataIfNeeded() {
        val existing = chatDao.getAllChats().first()
        if (existing.isEmpty()) {
            val starterChats = listOf(
                ChatEntity(
                    id = "chat_pinned_1",
                    title = "CS 201: Sorting & Complexity",
                    isPinned = true,
                    createdAt = System.currentTimeMillis() - 86400000 * 2,
                    updatedAt = System.currentTimeMillis() - 86400000 * 2
                ),
                ChatEntity(
                    id = "chat_pinned_2",
                    title = "Research Paper Thesis Draft",
                    isPinned = true,
                    createdAt = System.currentTimeMillis() - 86400000 * 4,
                    updatedAt = System.currentTimeMillis() - 86400000 * 4
                ),
                ChatEntity(
                    id = "chat_recent_1",
                    title = "Binary Search Tree in Kotlin",
                    isPinned = false,
                    createdAt = System.currentTimeMillis() - 3600000 * 3,
                    updatedAt = System.currentTimeMillis() - 3600000 * 3
                ),
                ChatEntity(
                    id = "chat_recent_2",
                    title = "Finals Revision Week Schedule",
                    isPinned = false,
                    createdAt = System.currentTimeMillis() - 3600000 * 12,
                    updatedAt = System.currentTimeMillis() - 3600000 * 12
                ),
                ChatEntity(
                    id = "chat_recent_3",
                    title = "C++ Pointer Arithmetic & Lifetime",
                    isPinned = false,
                    createdAt = System.currentTimeMillis() - 86400000,
                    updatedAt = System.currentTimeMillis() - 86400000
                )
            )
            for (chat in starterChats) {
                chatDao.insertChat(chat)
            }

            // Seed sample message in chat_recent_1
            chatDao.insertMessage(
                MessageEntity(
                    id = "msg_seed_1",
                    chatId = "chat_recent_1",
                    role = "USER",
                    content = "How do I implement in-order traversal for a BST in Kotlin?",
                    thinkingContent = null,
                    status = "COMPLETE",
                    createdAt = System.currentTimeMillis() - 3600000 * 3
                )
            )
            chatDao.insertMessage(
                MessageEntity(
                    id = "msg_seed_2",
                    chatId = "chat_recent_1",
                    role = "ASSISTANT",
                    content = "In-order traversal visits left subtree, root, then right subtree in ascending order.\n\n```kotlin\nclass TreeNode(var value: Int) {\n    var left: TreeNode? = null\n    var right: TreeNode? = null\n}\n\nfun inOrder(node: TreeNode?) {\n    if (node == null) return\n    inOrder(node.left)\n    print(node.value.toString() + \" \")\n    inOrder(node.right)\n}\n```",
                    thinkingContent = "Recursive formulation with base case checking null node.",
                    status = "COMPLETE",
                    createdAt = System.currentTimeMillis() - 3600000 * 3 + 1000
                )
            )
        }
    }

    override fun getChats(): Flow<List<Chat>> {
        return chatDao.getAllChats().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getMessages(chatId: String): Flow<List<Message>> {
        return chatDao.getMessagesForChat(chatId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun createNewChat(initialTitle: String): Chat {
        val newChat = Chat(title = initialTitle)
        chatDao.insertChat(newChat.toEntity())
        return newChat
    }

    override suspend fun pinChat(chatId: String, pinned: Boolean) {
        chatDao.setChatPinned(chatId, pinned)
    }

    override suspend fun renameChat(chatId: String, newTitle: String) {
        chatDao.renameChat(chatId, newTitle.trim())
    }

    override suspend fun deleteChat(chatId: String): Chat? {
        val existing = chatDao.getChatById(chatId) ?: return null
        chatDao.deleteChat(chatId)
        return existing.toDomain()
    }

    override suspend fun restoreChat(chat: Chat) {
        chatDao.insertChat(chat.toEntity())
    }

    override suspend fun addMessage(message: Message) {
        chatDao.insertMessage(message.toEntity())
    }

    override suspend fun updateMessage(message: Message) {
        chatDao.updateMessage(message.toEntity())
    }

    override suspend fun deleteMessagesAfter(chatId: String, messageId: String) {
        chatDao.deleteMessagesAfter(chatId, messageId)
    }

    override suspend fun clearAllData() {
        chatDao.clearAllData()
    }

    override fun streamChatReply(chatId: String, userPrompt: String, isPro: Boolean): Flow<StreamEvent> = flow {
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

        val tokenChunks = replyMarkdown.chunked(6)
        for (chunk in tokenChunks) {
            delay(35)
            emit(StreamEvent.Token(chunk))
        }

        emit(StreamEvent.Done)
    }

    private fun ChatEntity.toDomain() = Chat(
        id = id,
        title = title,
        isPinned = isPinned,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    private fun Chat.toEntity() = ChatEntity(
        id = id,
        title = title,
        isPinned = isPinned,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    private fun MessageEntity.toDomain() = Message(
        id = id,
        chatId = chatId,
        role = if (role == "USER") Role.USER else Role.ASSISTANT,
        content = content,
        thinkingContent = thinkingContent,
        status = when (status) {
            "STREAMING" -> MessageStatus.STREAMING
            "STOPPED" -> MessageStatus.STOPPED
            "ERROR" -> MessageStatus.ERROR
            "PENDING" -> MessageStatus.PENDING
            else -> MessageStatus.COMPLETE
        },
        createdAt = createdAt
    )

    private fun Message.toEntity() = MessageEntity(
        id = id,
        chatId = chatId,
        role = role.name,
        content = content,
        thinkingContent = thinkingContent,
        status = status.name,
        createdAt = createdAt
    )
}
