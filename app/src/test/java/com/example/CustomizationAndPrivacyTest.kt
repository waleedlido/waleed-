package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.util.AppIconProvider
import com.example.ui.theme.AppThemeManager
import com.example.ui.theme.ColorPalette
import com.example.ui.theme.ThemeMode
import com.example.util.ChatWallpaperManager
import com.example.util.PrivacyBlurManager
import com.example.util.WallpaperType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class CustomizationAndPrivacyTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun testAppThemeManagerDefaultsAndUpdates() = runBlocking {
        val themeManager = AppThemeManager.get(context)
        val initialConfig = themeManager.themeConfigFlow.first()

        // Defaults should be DARK mode and EMERALD palette
        assertEquals(ThemeMode.DARK, initialConfig.themeMode)
        assertEquals(ColorPalette.EMERALD, initialConfig.colorPalette)

        // Change theme mode to LIGHT
        themeManager.setThemeMode(ThemeMode.LIGHT)
        var updated = themeManager.themeConfigFlow.first { it.themeMode == ThemeMode.LIGHT }
        assertEquals(ThemeMode.LIGHT, updated.themeMode)

        // Change palette to ROYAL_BLUE
        themeManager.setColorPalette(ColorPalette.ROYAL_BLUE)
        updated = themeManager.themeConfigFlow.first { it.colorPalette == ColorPalette.ROYAL_BLUE }
        assertEquals(ColorPalette.ROYAL_BLUE, updated.colorPalette)

        // Reset to defaults
        themeManager.resetToDefaults()
        val reset = themeManager.themeConfigFlow.first { it.colorPalette == ColorPalette.EMERALD && it.themeMode == ThemeMode.DARK }
        assertEquals(ThemeMode.DARK, reset.themeMode)
        assertEquals(ColorPalette.EMERALD, reset.colorPalette)
    }

    @Test
    fun testChatWallpaperManagerDefaultsAndDimming() = runBlocking {
        val wallpaperManager = ChatWallpaperManager.get(context)
        val initialConfig = wallpaperManager.globalConfigFlow.first()

        // Default should be DEFAULT wallpaper type, dim alpha 0.40f (40%), not blurred
        assertEquals(WallpaperType.DEFAULT, initialConfig.wallpaperType)
        assertEquals(0.40f, initialConfig.dimAlpha, 0.01f)
        assertFalse(initialConfig.isBlurred)

        // Select a preset gradient
        wallpaperManager.setPresetWallpaper("emerald_glow")
        var updated = wallpaperManager.globalConfigFlow.first { it.wallpaperType == WallpaperType.PRESET }
        assertEquals(WallpaperType.PRESET, updated.wallpaperType)
        assertEquals("emerald_glow", updated.presetId)

        // Test dimming adjustment
        wallpaperManager.setDimAlpha(0.65f)
        updated = wallpaperManager.globalConfigFlow.first { it.dimAlpha >= 0.64f }
        assertEquals(0.65f, updated.dimAlpha, 0.01f)

        // Test blur toggle
        wallpaperManager.setBlurred(true)
        updated = wallpaperManager.globalConfigFlow.first { it.isBlurred }
        assertTrue(updated.isBlurred)

        // Revert to default
        wallpaperManager.setDefaultWallpaper()
        val reset = wallpaperManager.globalConfigFlow.first { it.wallpaperType == WallpaperType.DEFAULT }
        assertEquals(WallpaperType.DEFAULT, reset.wallpaperType)
    }

    @Test
    fun testPrivacyBlurManagerDefaultsAndToggles() = runBlocking {
        val privacyManager = PrivacyBlurManager.get(context)
        val initialConfig = privacyManager.configFlow.first()

        // Master blur should be OFF by default
        assertFalse(initialConfig.isMasterBlurActive)
        assertFalse(initialConfig.isMasterBlurActive)

        // Sub toggles default
        assertTrue(initialConfig.blurHomePreviews)
        assertTrue(initialConfig.blurChatMessages)
        assertFalse(initialConfig.blurSenderNames)

        // Activate master blur
        privacyManager.setMasterBlur(true)
        var updated = privacyManager.configFlow.first { it.isMasterBlurActive }
        assertTrue(updated.isMasterBlurActive)

        // Toggle sub preferences
        privacyManager.setBlurSenderNames(true)
        updated = privacyManager.configFlow.first { it.blurSenderNames }
        assertTrue(updated.blurSenderNames)

        // Toggle screenshot protection
        privacyManager.setPreventScreenshots(true)
        updated = privacyManager.configFlow.first { it.preventScreenshots }
        assertTrue(updated.preventScreenshots)

        // Restore master blur off
        privacyManager.setMasterBlur(false)
        val restored = privacyManager.configFlow.first { !it.isMasterBlurActive }
        assertFalse(restored.isMasterBlurActive)
    }

    @Test
    fun testSupportedAppIdentification() {
        // WhatsApp regular
        val waLabel = AppIconProvider.getAppLabel(context, "com.whatsapp", isClonedApp = false)
        assertTrue(waLabel.contains("واتساب"))
        assertFalse(waLabel.contains("مستنسخ"))

        // WhatsApp App Twin (HONOR MagicOS)
        val waTwinLabel = AppIconProvider.getAppLabel(context, "com.whatsapp", isClonedApp = true)
        assertTrue(waTwinLabel.contains("واتساب"))
        assertTrue(waTwinLabel.contains("مستنسخ"))

        // Telegram
        val tgLabel = AppIconProvider.getAppLabel(context, "org.telegram.messenger", isClonedApp = false)
        assertTrue(tgLabel.contains("تيليجرام"))

        // Messenger
        val fbLabel = AppIconProvider.getAppLabel(context, "com.facebook.orca", isClonedApp = false)
        assertTrue(fbLabel.contains("ماسنجر"))

        // Signal
        val sigLabel = AppIconProvider.getAppLabel(context, "org.thoughtcrime.securesms", isClonedApp = false)
        assertTrue(sigLabel.contains("سيجنال"))

        // SMS
        val smsLabel = AppIconProvider.getAppLabel(context, "com.google.android.apps.messaging", isClonedApp = false)
        assertTrue(smsLabel.contains("SMS"))
    }
}
