package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val MessageVaultColorScheme = darkColorScheme(
    primary = NeonGreen,
    onPrimary = Color.Black,
    primaryContainer = DarkEmerald,
    onPrimaryContainer = BrightNeonGreen,
    secondary = CyanAccent,
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFF00363A),
    onSecondaryContainer = Color(0xFF80DEEA),
    tertiary = GoldAccent,
    onTertiary = Color.Black,
    background = DeepObsidian,
    onBackground = TextPrimaryDark,
    surface = SurfaceDark,
    onSurface = TextPrimaryDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = TextSecondaryDark,
    outline = BorderDark,
    outlineVariant = BorderHighlight,
    error = DangerRed,
    onError = Color.White,
    errorContainer = DangerRedContainer,
    onErrorContainer = Color(0xFFFFB4AB)
)

@Composable
fun MessageVaultTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = MessageVaultColorScheme,
        typography = Typography,
        content = content
    )
}
