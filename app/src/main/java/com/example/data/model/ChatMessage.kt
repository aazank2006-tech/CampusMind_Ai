package com.example.data.model

import java.util.UUID

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val role: String, // "user" or "assistant"
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val modelName: String = "",
    val personaTitle: String = "",
    val isError: Boolean = false
) {
    val isUser: Boolean get() = role == "user"
}
