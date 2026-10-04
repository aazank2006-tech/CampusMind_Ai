package com.example.domain.model

import java.util.UUID

enum class Role {
    USER,
    ASSISTANT
}

enum class MessageStatus {
    PENDING,
    STREAMING,
    COMPLETE,
    ERROR,
    STOPPED
}

data class Attachment(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val mimeType: String,
    val uri: String? = null,
    val sizeBytes: Long = 0L
)

data class Chat(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val isPinned: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

data class Message(
    val id: String = UUID.randomUUID().toString(),
    val chatId: String,
    val role: Role,
    val content: String,
    val thinkingContent: String? = null,
    val isThinkingExpanded: Boolean = false,
    val attachments: List<Attachment> = emptyList(),
    val status: MessageStatus = MessageStatus.COMPLETE,
    val createdAt: Long = System.currentTimeMillis()
)

data class AiModel(
    val id: String,
    val displayName: String,
    val description: String,
    val badge: String,
    val isPro: Boolean = false
)

object AiModels {
    val FAST = AiModel(
        id = "campusmind-fast",
        displayName = "CampusMind Fast",
        description = "Quick, responsive answers for everyday tasks and study questions",
        badge = "Fast",
        isPro = false
    )

    val PRO = AiModel(
        id = "campusmind-pro",
        displayName = "CampusMind Pro",
        description = "Advanced reasoning, complex coding, and deep academic synthesis",
        badge = "Pro",
        isPro = true
    )

    val ALL = listOf(FAST, PRO)
}

data class SuggestionPrompt(
    val id: String,
    val title: String,
    val prompt: String,
    val iconEmoji: String
)

object DefaultSuggestions {
    val ALL = listOf(
        SuggestionPrompt(
            id = "write",
            title = "Help me write",
            prompt = "Draft a persuasive introduction for a research paper on machine learning in education.",
            iconEmoji = "✍️"
        ),
        SuggestionPrompt(
            id = "plan",
            title = "Plan a study schedule",
            prompt = "Create a balanced 5-day revision schedule for 3 upcoming university final exams.",
            iconEmoji = "📅"
        ),
        SuggestionPrompt(
            id = "explain",
            title = "Explain a concept",
            prompt = "Explain Dijkstra's shortest path algorithm simply, with step-by-step intuition.",
            iconEmoji = "💡"
        ),
        SuggestionPrompt(
            id = "brainstorm",
            title = "Brainstorm ideas",
            prompt = "Brainstorm 5 innovative capstone project ideas combining mobile apps and on-device AI.",
            iconEmoji = "🚀"
        )
    )
}
