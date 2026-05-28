package com.checkarr.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Blue = Color(0xFF3B82F6)
val Indigo = Color(0xFF6366F1)
val Purple = Color(0xFFA855F7)
val Pink = Color(0xFFEC4899)
val Red = Color(0xFFEF4444)
val Orange = Color(0xFFF97316)
val Yellow = Color(0xFFEAB308)
val Green = Color(0xFF22C55E)
val Mint = Color(0xFF14B8A6)
val Cyan = Color(0xFF06B6D4)

fun accentColor(name: String): Color = when (name) {
    "blue" -> Blue
    "indigo" -> Indigo
    "purple" -> Purple
    "pink" -> Pink
    "red" -> Red
    "orange" -> Orange
    "yellow" -> Yellow
    "green" -> Green
    "mint" -> Mint
    "cyan" -> Cyan
    else -> Blue
}

private val DarkColorScheme = darkColorScheme(
    primary = Blue,
    secondary = Blue,
    tertiary = Indigo,
    background = Color(0xFF0D0D0D),
    surface = Color(0xFF1C1C1E),
    surfaceVariant = Color(0xFF2C2C2E),
    onBackground = Color.White,
    onSurface = Color.White,
    onSurfaceVariant = Color(0xFF8E8E93),
    outline = Color(0xFF3A3A3C),
    error = Color(0xFFFF453A)
)

private val LightColorScheme = lightColorScheme(
    primary = Blue,
    secondary = Blue,
    tertiary = Indigo,
    background = Color(0xFFF2F2F7),
    surface = Color.White,
    surfaceVariant = Color(0xFFEEEEF0),
    onBackground = Color(0xFF1C1C1E),
    onSurface = Color(0xFF1C1C1E),
    onSurfaceVariant = Color(0xFF6C6C70),
    outline = Color(0xFFC6C6C8),
    error = Color(0xFFFF3B30)
)

@Composable
fun RuddarrTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    accentColorName: String = "blue",
    content: @Composable () -> Unit
) {
    val baseScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val accent = accentColor(accentColorName)
    val colorScheme = baseScheme.copy(
        primary = accent,
        secondary = accent,
        onPrimary = Color.White,
        onSecondary = Color.White
    )

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography(),
        content = content
    )
}
