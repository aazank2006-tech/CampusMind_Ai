package com.example.data.model

data class ModelOption(
    val id: String,
    val displayName: String,
    val badge: String,
    val description: String
)

object ModelOptions {
    val GEMINI_FLASH = ModelOption(
        id = "gemini-3.5-flash",
        displayName = "Gemini 3.5 Flash",
        badge = "⭐ Best Overall",
        description = "Balanced intelligence, fast speed, and strong multimodal capabilities"
    )

    val GEMINI_PRO = ModelOption(
        id = "gemini-3.1-pro-preview",
        displayName = "Gemini 3.1 Pro",
        badge = "💻 Best for Coding",
        description = "Advanced reasoning, in-depth code synthesis, and complex STEM topics"
    )

    val GEMINI_FLASH_LITE = ModelOption(
        id = "gemini-3.1-flash-lite-preview",
        displayName = "Gemini 3.1 Flash Lite",
        badge = "⚡ Ultra Fast",
        description = "Low latency, quick answers for rapid study sessions"
    )

    val ALL = listOf(
        GEMINI_FLASH,
        GEMINI_PRO,
        GEMINI_FLASH_LITE
    )
}
