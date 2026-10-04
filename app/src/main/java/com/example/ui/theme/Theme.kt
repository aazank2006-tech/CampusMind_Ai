package com.example.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightGeminiColorScheme = lightColorScheme(
    primary = GeminiLightPrimary,
    onPrimary = GeminiLightOnPrimary,
    primaryContainer = GeminiLightPrimaryContainer,
    onPrimaryContainer = GeminiLightOnPrimaryContainer,
    background = GeminiLightBackground,
    onBackground = GeminiLightOnSurface,
    surface = GeminiLightSurface,
    onSurface = GeminiLightOnSurface,
    surfaceVariant = GeminiLightSurfaceContainer,
    onSurfaceVariant = GeminiLightOnSurfaceVariant,
    surfaceContainer = GeminiLightSurfaceContainer,
    surfaceContainerHigh = GeminiLightSurfaceContainerHigh,
    surfaceContainerHighest = GeminiLightSurfaceContainerHighest,
    outline = GeminiLightOutline,
    outlineVariant = GeminiLightOutlineVariant
)

private val DarkGeminiColorScheme = darkColorScheme(
    primary = GeminiDarkPrimary,
    onPrimary = GeminiDarkOnPrimary,
    primaryContainer = GeminiDarkPrimaryContainer,
    onPrimaryContainer = GeminiDarkOnPrimaryContainer,
    background = GeminiDarkBackground,
    onBackground = GeminiDarkOnSurface,
    surface = GeminiDarkSurface,
    onSurface = GeminiDarkOnSurface,
    surfaceVariant = GeminiDarkSurfaceContainer,
    onSurfaceVariant = GeminiDarkOnSurfaceVariant,
    surfaceContainer = GeminiDarkSurfaceContainer,
    surfaceContainerHigh = GeminiDarkSurfaceContainerHigh,
    surfaceContainerHighest = GeminiDarkSurfaceContainerHighest,
    outline = GeminiDarkOutline,
    outlineVariant = GeminiDarkOutlineVariant
)

@Composable
fun CampusMindTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkGeminiColorScheme
        else -> LightGeminiColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
                WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        shapes = GeminiShapes,
        content = content
    )
}
