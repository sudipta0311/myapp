package com.explainmymoney.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

val DeepBlue = Color(0xFF1E3A5F)
val AccentGreen = Color(0xFF4ECDC4)
val SoftGreen = Color(0xFF7ED9B5)
val PaleBlue = Color(0xFFE8F4FD)
val CardWhite = Color(0xFFFAFBFC)
val TextPrimary = Color(0xFF1A1F36)
val TextSecondary = Color(0xFF6B7280)
val Success = Color(0xFF10B981)
val Warning = Color(0xFFF59E0B)
val Error = Color(0xFFEF4444)

private val LightColorScheme = lightColorScheme(
    primary = DeepBlue,
    onPrimary = Color.White,
    primaryContainer = PaleBlue,
    onPrimaryContainer = DeepBlue,
    secondary = AccentGreen,
    onSecondary = Color.White,
    secondaryContainer = SoftGreen.copy(alpha = 0.2f),
    onSecondaryContainer = DeepBlue,
    tertiary = SoftGreen,
    onTertiary = Color.White,
    background = Color(0xFFF8FAFC),
    onBackground = TextPrimary,
    surface = CardWhite,
    onSurface = TextPrimary,
    surfaceVariant = Color(0xFFF1F5F9),
    onSurfaceVariant = TextSecondary,
    error = Error,
    onError = Color.White
)

private val DarkColorScheme = darkColorScheme(
    primary = AccentGreen,
    onPrimary = DeepBlue,
    primaryContainer = DeepBlue,
    onPrimaryContainer = AccentGreen,
    secondary = SoftGreen,
    onSecondary = DeepBlue,
    secondaryContainer = DeepBlue.copy(alpha = 0.6f),
    onSecondaryContainer = SoftGreen,
    tertiary = AccentGreen,
    onTertiary = DeepBlue,
    background = Color(0xFF0F172A),
    onBackground = Color(0xFFF1F5F9),
    surface = Color(0xFF1E293B),
    onSurface = Color(0xFFF1F5F9),
    surfaceVariant = Color(0xFF334155),
    onSurfaceVariant = Color(0xFF94A3B8),
    error = Error,
    onError = Color.White
)

@Composable
fun ExplainMyMoneyTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
