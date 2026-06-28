package com.rannuan.tv.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = Brand500,
    onPrimary = Zinc950,
    background = Zinc950,
    surface = Zinc900,
    surfaceVariant = Zinc800,
    onBackground = Zinc300,
    onSurface = Zinc300,
    outline = White10,
    outlineVariant = Zinc700,
    error = Error500,
    errorContainer = Error500.copy(alpha = 0.15f),
    tertiary = Brand400,
    tertiaryContainer = Brand400.copy(alpha = 0.12f)
)

@Composable
fun RanNuanTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
