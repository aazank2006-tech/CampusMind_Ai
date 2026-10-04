package com.example.data.local.entities

data class ChatEntity(
    val id: String,
    val title: String,
    val isPinned: Boolean,
    val createdAt: Long,
    val updatedAt: Long
)

data class MessageEntity(
    val id: String,
    val chatId: String,
    val role: String, // "USER" or "ASSISTANT"
    val content: String,
    val thinkingContent: String?,
    val status: String, // "PENDING", "STREAMING", "COMPLETE", "ERROR", "STOPPED"
    val createdAt: Long
)

data class AttachmentEntity(
    val id: String,
    val messageId: String,
    val name: String,
    val mimeType: String,
    val uri: String?,
    val sizeBytes: Long
)
