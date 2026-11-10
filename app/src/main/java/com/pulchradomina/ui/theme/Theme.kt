package com.pulchradomina.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val GothicDarkColorScheme = darkColorScheme(
    primary = VioletAccent,
    onPrimary = Color.White,
    primaryContainer = AbyssalPurple,
    onPrimaryContainer = VioletLight,
    secondary = VioletLight,
    onSecondary = ObsidianDark,
    secondaryContainer = SurfaceCard,
    onSecondaryContainer = SilverMist,
    tertiary = RoseDark,
    onTertiary = Color.White,
    background = ObsidianDark,
    onBackground = PaleText,
    surface = AbyssalPurple,
    onSurface = PaleText,
    surfaceVariant = SurfaceGothic,
    onSurfaceVariant = MutedText,
    outline = BorderGothic
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Mantém a identidade gótica personalizada do jogo em todos os dispositivos
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = GothicDarkColorScheme,
        typography = Typography,
        content = content
    )
}
