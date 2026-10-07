package com.livetranslate.app.presentation.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val LiveTranslateColorScheme = darkColorScheme(
    primary = ElectricBlue,
    onPrimary = Midnight,
    secondary = SoftMint,
    onSecondary = Midnight,
    background = Midnight,
    onBackground = FrostWhite,
    surface = DeepOcean,
    onSurface = FrostWhite,
    surfaceVariant = DeepOcean,
    onSurfaceVariant = Mist,
    outline = OverlayBorder,
)

@Composable
fun LiveTranslateTheme(
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = LiveTranslateColorScheme,
        typography = Typography,
        content = content,
    )
}
