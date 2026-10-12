package com.livetranslate.app.presentation.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private fun colorScheme(theme: AppTheme) = darkColorScheme(
    primary = theme.primary,
    onPrimary = Color(0xFF162126),
    secondary = theme.secondary,
    onSecondary = Color(0xFF162126),
    background = theme.backgroundBottom,
    onBackground = FrostWhite,
    surface = theme.backgroundBottom,
    onSurface = FrostWhite,
    surfaceVariant = theme.backgroundTop,
    onSurfaceVariant = Mist,
    outline = OverlayBorder,
)

@Composable
fun LiveTranslateTheme(
    theme: AppTheme = AppTheme.MIST,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = colorScheme(theme),
        typography = Typography,
        content = content,
    )
}
