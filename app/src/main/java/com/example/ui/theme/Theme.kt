package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext

val LocalCustomColors = staticCompositionLocalOf {
    ThemeColorFactory.createCustomColors(ColorPalette.EMERALD, isDark = true)
}

object VaultTheme {
    val colors: CustomThemeColors
        @Composable
        @ReadOnlyComposable
        get() = LocalCustomColors.current
}

@Composable
fun MessageVaultTheme(
    themeConfig: ThemeConfig? = null,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val systemInDark = isSystemInDarkTheme()

    val effectiveConfig = themeConfig ?: ThemeConfig()
    val isDark = when (effectiveConfig.mode) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.SYSTEM -> systemInDark
    }

    val colorScheme = ThemeColorFactory.createColorScheme(effectiveConfig.palette, isDark)
    val customColors = ThemeColorFactory.createCustomColors(effectiveConfig.palette, isDark)

    CompositionLocalProvider(
        LocalCustomColors provides customColors
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
