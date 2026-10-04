package com.example.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// Light Gemini-like Material You Palette
val GeminiLightBackground = Color(0xFFFFFFFF)
val GeminiLightSurface = Color(0xFFFFFFFF)
val GeminiLightSurfaceContainer = Color(0xFFF0F4F9)
val GeminiLightSurfaceContainerHigh = Color(0xFFE9EEF6)
val GeminiLightSurfaceContainerHighest = Color(0xFFE1E8F2)
val GeminiLightOnSurface = Color(0xFF1F1F1F)
val GeminiLightOnSurfaceVariant = Color(0xFF5F6368)
val GeminiLightPrimary = Color(0xFF0B57D0)
val GeminiLightOnPrimary = Color(0xFFFFFFFF)
val GeminiLightPrimaryContainer = Color(0xFFD3E3FD)
val GeminiLightOnPrimaryContainer = Color(0xFF041E49)
val GeminiLightOutline = Color(0xFFC4C7C5)
val GeminiLightOutlineVariant = Color(0xFFE1E3E1)

// Dark Gemini-like Material You Palette
val GeminiDarkBackground = Color(0xFF131314)
val GeminiDarkSurface = Color(0xFF131314)
val GeminiDarkSurfaceContainer = Color(0xFF1E1F20)
val GeminiDarkSurfaceContainerHigh = Color(0xFF28292A)
val GeminiDarkSurfaceContainerHighest = Color(0xFF333537)
val GeminiDarkOnSurface = Color(0xFFE3E3E3)
val GeminiDarkOnSurfaceVariant = Color(0xFF9AA0A6)
val GeminiDarkPrimary = Color(0xFFA8C7FA)
val GeminiDarkOnPrimary = Color(0xFF042B67)
val GeminiDarkPrimaryContainer = Color(0xFF0842A0)
val GeminiDarkOnPrimaryContainer = Color(0xFFD3E3FD)
val GeminiDarkOutline = Color(0xFF444746)
val GeminiDarkOutlineVariant = Color(0xFF2E3133)

// Signature Gemini Gradient: Blue -> Purple -> Coral/Pink
val GeminiGreetingGradient = Brush.linearGradient(
    colors = listOf(
        Color(0xFF4285F4),
        Color(0xFF9B72CB),
        Color(0xFFD96570)
    )
)

// Accent Waveform & Orb Gradients
val LiveOrbGradient = Brush.radialGradient(
    colors = listOf(
        Color(0xFF4285F4),
        Color(0xFF9B72CB),
        Color(0x00131314)
    )
)
