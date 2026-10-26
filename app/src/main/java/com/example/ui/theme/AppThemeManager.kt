package com.example.ui.theme

import android.content.Context
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

val Context.themeDataStore: DataStore<Preferences> by preferencesDataStore(name = "theme_preferences")

enum class ThemeMode(val id: String, val title: String) {
    DARK("DARK", "الوضع الداكن (Dark Mode)"),
    LIGHT("LIGHT", "الوضع الفاتح (Light Mode)"),
    SYSTEM("SYSTEM", "تلقائي حسب إعدادات الهاتف (System Default)");

    companion object {
        fun fromId(id: String): ThemeMode = entries.find { it.id == id } ?: DARK
    }
}

enum class ColorPalette(
    val id: String,
    val title: String,
    val primaryColor: Color,
    val secondaryColor: Color,
    val darkSurfaceColor: Color,
    val lightSurfaceColor: Color
) {
    EMERALD(
        id = "EMERALD",
        title = "الأخضر الزمردي (الافتراضي)",
        primaryColor = Color(0xFF00E676),
        secondaryColor = Color(0xFF00B0FF),
        darkSurfaceColor = Color(0xFF0E1A11),
        lightSurfaceColor = Color(0xFFF0FDF4)
    ),
    ROYAL_BLUE(
        id = "ROYAL_BLUE",
        title = "الأزرق الملكي",
        primaryColor = Color(0xFF2979FF),
        secondaryColor = Color(0xFF00E5FF),
        darkSurfaceColor = Color(0xFF0B1424),
        lightSurfaceColor = Color(0xFFF0F6FF)
    ),
    ROYAL_PURPLE(
        id = "ROYAL_PURPLE",
        title = "البنفسجي الملكي",
        primaryColor = Color(0xFFB388FF),
        secondaryColor = Color(0xFFFF80AB),
        darkSurfaceColor = Color(0xFF170C24),
        lightSurfaceColor = Color(0xFFFAF5FF)
    ),
    BLACK_GOLD(
        id = "BLACK_GOLD",
        title = "الأسود والذهبي",
        primaryColor = Color(0xFFFFD700),
        secondaryColor = Color(0xFFFFB300),
        darkSurfaceColor = Color(0xFF18150B),
        lightSurfaceColor = Color(0xFFFFFDF0)
    ),
    CYAN_TURQUOISE(
        id = "CYAN_TURQUOISE",
        title = "التركواز البحري",
        primaryColor = Color(0xFF00E5FF),
        secondaryColor = Color(0xFF1DE9B6),
        darkSurfaceColor = Color(0xFF07191C),
        lightSurfaceColor = Color(0xFFF0FDFA)
    ),
    CRIMSON_RED(
        id = "CRIMSON_RED",
        title = "الأحمر القرمزي",
        primaryColor = Color(0xFFFF5252),
        secondaryColor = Color(0xFFFF7043),
        darkSurfaceColor = Color(0xFF1C0A0C),
        lightSurfaceColor = Color(0xFFFFF1F2)
    ),
    SLATE_GRAY(
        id = "SLATE_GRAY",
        title = "الرمادي الأنيق",
        primaryColor = Color(0xFF90A4AE),
        secondaryColor = Color(0xFFB0BEC5),
        darkSurfaceColor = Color(0xFF14171A),
        lightSurfaceColor = Color(0xFFF8FAFC)
    );

    val displayNameAr: String get() = title
    val displayNameEn: String get() = when (this) {
        EMERALD -> "Royal Emerald (Default)"
        ROYAL_BLUE -> "Royal Blue"
        ROYAL_PURPLE -> "Imperial Purple"
        BLACK_GOLD -> "Black & Gold"
        CYAN_TURQUOISE -> "Cyan Turquoise"
        CRIMSON_RED -> "Crimson Red"
        SLATE_GRAY -> "Elegant Slate"
    }
    val accentColor: Color get() = primaryColor

    companion object {
        fun fromId(id: String): ColorPalette = entries.find { it.id == id } ?: EMERALD
    }
}

data class ThemeConfig(
    val mode: ThemeMode = ThemeMode.DARK,
    val palette: ColorPalette = ColorPalette.EMERALD
) {
    val themeMode: ThemeMode get() = mode
    val colorPalette: ColorPalette get() = palette
}

data class CustomThemeColors(
    val isDark: Boolean,
    val primary: Color,
    val primaryContainer: Color,
    val secondary: Color,
    val background: Color,
    val surface: Color,
    val surfaceElevated: Color,
    val border: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val incomingBubble: Color,
    val incomingBubbleBorder: Color,
    val deletedBubble: Color,
    val deletedBubbleBorder: Color,
    val outgoingBubble: Color = primaryContainer,
    val outgoingBubbleBorder: Color = border
) {
    val accent: Color get() = primary
    val cardBorder: Color get() = border
    val cardBackground: Color get() = surface
    val topBarBackground: Color get() = surface
}

object ThemeColorFactory {

    fun createColorScheme(palette: ColorPalette, isDark: Boolean): ColorScheme {
        return if (isDark) {
            darkColorScheme(
                primary = palette.primaryColor,
                onPrimary = Color.Black,
                primaryContainer = getDarkContainer(palette),
                onPrimaryContainer = palette.primaryColor,
                secondary = palette.secondaryColor,
                onSecondary = Color.Black,
                background = getDarkBackground(palette),
                onBackground = Color(0xFFF0F4F2),
                surface = palette.darkSurfaceColor,
                onSurface = Color(0xFFF0F4F2),
                surfaceVariant = getDarkElevated(palette),
                onSurfaceVariant = Color(0xFFA0B3A5),
                outline = getDarkBorder(palette),
                error = Color(0xFFFF453A),
                onError = Color.White
            )
        } else {
            lightColorScheme(
                primary = getLightPrimary(palette),
                onPrimary = Color.White,
                primaryContainer = getLightContainer(palette),
                onPrimaryContainer = getLightPrimary(palette),
                secondary = palette.secondaryColor,
                onSecondary = Color.Black,
                background = Color(0xFFF8FAFC),
                onBackground = Color(0xFF0F172A),
                surface = Color.White,
                onSurface = Color(0xFF0F172A),
                surfaceVariant = palette.lightSurfaceColor,
                onSurfaceVariant = Color(0xFF475569),
                outline = Color(0xFFCBD5E1),
                error = Color(0xFFDC2626),
                onError = Color.White
            )
        }
    }

    fun createCustomColors(palette: ColorPalette, isDark: Boolean): CustomThemeColors {
        return if (isDark) {
            CustomThemeColors(
                isDark = true,
                primary = palette.primaryColor,
                primaryContainer = getDarkContainer(palette),
                secondary = palette.secondaryColor,
                background = getDarkBackground(palette),
                surface = palette.darkSurfaceColor,
                surfaceElevated = getDarkElevated(palette),
                border = getDarkBorder(palette),
                textPrimary = Color(0xFFF3FFF6),
                textSecondary = Color(0xFFA5BBA9),
                textMuted = Color(0xFF6B8270),
                incomingBubble = palette.darkSurfaceColor,
                incomingBubbleBorder = getDarkBorder(palette),
                deletedBubble = Color(0xFF260D10),
                deletedBubbleBorder = Color(0xFFFF453A).copy(alpha = 0.5f),
                outgoingBubble = getDarkOutgoingBubble(palette),
                outgoingBubbleBorder = palette.primaryColor.copy(alpha = 0.4f)
            )
        } else {
            CustomThemeColors(
                isDark = false,
                primary = getLightPrimary(palette),
                primaryContainer = getLightContainer(palette),
                secondary = palette.secondaryColor,
                background = Color(0xFFF1F5F9),
                surface = Color.White,
                surfaceElevated = palette.lightSurfaceColor,
                border = Color(0xFFE2E8F0),
                textPrimary = Color(0xFF0F172A),
                textSecondary = Color(0xFF334155),
                textMuted = Color(0xFF64748B),
                incomingBubble = Color.White,
                incomingBubbleBorder = Color(0xFFE2E8F0),
                deletedBubble = Color(0xFFFEF2F2),
                deletedBubbleBorder = Color(0xFFFCA5A5),
                outgoingBubble = getLightOutgoingBubble(palette),
                outgoingBubbleBorder = palette.primaryColor.copy(alpha = 0.35f)
            )
        }
    }

    private fun getDarkOutgoingBubble(palette: ColorPalette): Color = when (palette) {
        ColorPalette.EMERALD -> Color(0xFF0F3822)
        ColorPalette.ROYAL_BLUE -> Color(0xFF10284A)
        ColorPalette.ROYAL_PURPLE -> Color(0xFF2E1245)
        ColorPalette.BLACK_GOLD -> Color(0xFF33290D)
        ColorPalette.CYAN_TURQUOISE -> Color(0xFF0A2D33)
        ColorPalette.CRIMSON_RED -> Color(0xFF3A1015)
        ColorPalette.SLATE_GRAY -> Color(0xFF222830)
    }

    private fun getLightOutgoingBubble(palette: ColorPalette): Color = when (palette) {
        ColorPalette.EMERALD -> Color(0xFFE6F9EE)
        ColorPalette.ROYAL_BLUE -> Color(0xFFEBF3FF)
        ColorPalette.ROYAL_PURPLE -> Color(0xFFF5ECFF)
        ColorPalette.BLACK_GOLD -> Color(0xFFFFFBEA)
        ColorPalette.CYAN_TURQUOISE -> Color(0xFFE6FAF9)
        ColorPalette.CRIMSON_RED -> Color(0xFFFFECEF)
        ColorPalette.SLATE_GRAY -> Color(0xFFF1F5F9)
    }

    private fun getDarkBackground(palette: ColorPalette): Color = when (palette) {
        ColorPalette.EMERALD -> Color(0xFF080F0A)
        ColorPalette.ROYAL_BLUE -> Color(0xFF060B14)
        ColorPalette.ROYAL_PURPLE -> Color(0xFF0D0614)
        ColorPalette.BLACK_GOLD -> Color(0xFF0D0C07)
        ColorPalette.CYAN_TURQUOISE -> Color(0xFF040E10)
        ColorPalette.CRIMSON_RED -> Color(0xFF100507)
        ColorPalette.SLATE_GRAY -> Color(0xFF0B0D0E)
    }

    private fun getDarkContainer(palette: ColorPalette): Color = when (palette) {
        ColorPalette.EMERALD -> Color(0xFF003819)
        ColorPalette.ROYAL_BLUE -> Color(0xFF0D254C)
        ColorPalette.ROYAL_PURPLE -> Color(0xFF2E134D)
        ColorPalette.BLACK_GOLD -> Color(0xFF3B2E05)
        ColorPalette.CYAN_TURQUOISE -> Color(0xFF042F36)
        ColorPalette.CRIMSON_RED -> Color(0xFF3D090E)
        ColorPalette.SLATE_GRAY -> Color(0xFF20262B)
    }

    private fun getDarkElevated(palette: ColorPalette): Color = when (palette) {
        ColorPalette.EMERALD -> Color(0xFF1C3322)
        ColorPalette.ROYAL_BLUE -> Color(0xFF152238)
        ColorPalette.ROYAL_PURPLE -> Color(0xFF26143A)
        ColorPalette.BLACK_GOLD -> Color(0xFF262112)
        ColorPalette.CYAN_TURQUOISE -> Color(0xFF0E282E)
        ColorPalette.CRIMSON_RED -> Color(0xFF2B1014)
        ColorPalette.SLATE_GRAY -> Color(0xFF1F2428)
    }

    private fun getDarkBorder(palette: ColorPalette): Color = when (palette) {
        ColorPalette.EMERALD -> Color(0xFF23442C)
        ColorPalette.ROYAL_BLUE -> Color(0xFF1E3A5F)
        ColorPalette.ROYAL_PURPLE -> Color(0xFF3D215E)
        ColorPalette.BLACK_GOLD -> Color(0xFF4A3E14)
        ColorPalette.CYAN_TURQUOISE -> Color(0xFF13424A)
        ColorPalette.CRIMSON_RED -> Color(0xFF4A181D)
        ColorPalette.SLATE_GRAY -> Color(0xFF2E363D)
    }

    private fun getLightPrimary(palette: ColorPalette): Color = when (palette) {
        ColorPalette.EMERALD -> Color(0xFF059669)
        ColorPalette.ROYAL_BLUE -> Color(0xFF1D4ED8)
        ColorPalette.ROYAL_PURPLE -> Color(0xFF7C3AED)
        ColorPalette.BLACK_GOLD -> Color(0xFFB45309)
        ColorPalette.CYAN_TURQUOISE -> Color(0xFF0891B2)
        ColorPalette.CRIMSON_RED -> Color(0xFFDC2626)
        ColorPalette.SLATE_GRAY -> Color(0xFF475569)
    }

    private fun getLightContainer(palette: ColorPalette): Color = when (palette) {
        ColorPalette.EMERALD -> Color(0xFFD1FAE5)
        ColorPalette.ROYAL_BLUE -> Color(0xFFDBEAFE)
        ColorPalette.ROYAL_PURPLE -> Color(0xFFEDE9FE)
        ColorPalette.BLACK_GOLD -> Color(0xFFFEF3C7)
        ColorPalette.CYAN_TURQUOISE -> Color(0xFFCFFAFE)
        ColorPalette.CRIMSON_RED -> Color(0xFFFFE4E6)
        ColorPalette.SLATE_GRAY -> Color(0xFFE2E8F0)
    }
}

class AppThemeManager private constructor(private val context: Context) {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    val themeConfigFlow: Flow<ThemeConfig> = context.themeDataStore.data.map { prefs ->
        ThemeConfig(
            mode = ThemeMode.fromId(prefs[KEY_THEME_MODE] ?: ThemeMode.DARK.id),
            palette = ColorPalette.fromId(prefs[KEY_COLOR_PALETTE] ?: ColorPalette.EMERALD.id)
        )
    }

    val themeConfigState: StateFlow<ThemeConfig> = themeConfigFlow.stateIn(
        scope = scope,
        started = SharingStarted.Eagerly,
        initialValue = ThemeConfig()
    )

    fun setThemeMode(mode: ThemeMode) {
        scope.launch {
            context.themeDataStore.edit { it[KEY_THEME_MODE] = mode.id }
        }
    }

    fun setColorPalette(palette: ColorPalette) {
        scope.launch {
            context.themeDataStore.edit { it[KEY_COLOR_PALETTE] = palette.id }
        }
    }

    fun resetToDefault() {
        scope.launch {
            context.themeDataStore.edit { prefs ->
                prefs[KEY_THEME_MODE] = ThemeMode.DARK.id
                prefs[KEY_COLOR_PALETTE] = ColorPalette.EMERALD.id
            }
        }
    }

    fun resetToDefaults() = resetToDefault()

    companion object {
        private val KEY_THEME_MODE = stringPreferencesKey("app_theme_mode")
        private val KEY_COLOR_PALETTE = stringPreferencesKey("app_color_palette")

        @Volatile
        private var instance: AppThemeManager? = null

        fun get(context: Context): AppThemeManager {
            return instance ?: synchronized(this) {
                instance ?: AppThemeManager(context.applicationContext).also { instance = it }
            }
        }
    }
}
