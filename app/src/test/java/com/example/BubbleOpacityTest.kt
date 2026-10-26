package com.example

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.test.core.app.ApplicationProvider
import com.example.ui.theme.ThemeColorFactory
import com.example.ui.theme.ColorPalette
import com.example.util.ChatWallpaperManager
import com.example.util.WallpaperType
import com.example.util.wallpaperDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class BubbleOpacityTest {

    private lateinit var context: Context
    private lateinit var wallpaperManager: ChatWallpaperManager

    @Before
    fun setUp() = runBlocking {
        context = ApplicationProvider.getApplicationContext()
        context.wallpaperDataStore.edit { it.clear() }
        wallpaperManager = ChatWallpaperManager.get(context)
    }

    @Test
    fun testDefaultOpacityValues() = runBlocking {
        wallpaperManager.setDefaultWallpaper()
        val config = wallpaperManager.globalConfigFlow.first { it.wallpaperType == WallpaperType.DEFAULT }

        assertEquals(WallpaperType.DEFAULT, config.wallpaperType)
        assertEquals(1.0f, config.incomingBubbleOpacity, 0.01f)
        assertEquals(1.0f, config.outgoingBubbleOpacity, 0.01f)
        assertTrue(config.isLinkedBubbleOpacity)
    }

    @Test
    fun testPresetWallpaperDefaultsOpacityTo65Percent() = runBlocking {
        // When user switches from DEFAULT to a preset/custom wallpaper, opacity defaults to 65% (0.65f)
        wallpaperManager.setPresetWallpaper("emerald_glow")
        val config = wallpaperManager.globalConfigFlow.first { it.wallpaperType == WallpaperType.PRESET && it.incomingBubbleOpacity == 0.65f }

        assertEquals(WallpaperType.PRESET, config.wallpaperType)
        assertEquals(0.65f, config.incomingBubbleOpacity, 0.01f)
        assertEquals(0.65f, config.outgoingBubbleOpacity, 0.01f)
    }

    @Test
    fun testIndependentIncomingAndOutgoingOpacity() = runBlocking {
        // Disable linking
        wallpaperManager.setLinkedBubbleOpacity(false)
        var config = wallpaperManager.globalConfigFlow.first { !it.isLinkedBubbleOpacity }
        assertFalse(config.isLinkedBubbleOpacity)

        // Set incoming to 40% (0.40f)
        wallpaperManager.setIncomingBubbleOpacity(0.40f)
        config = wallpaperManager.globalConfigFlow.first { it.incomingBubbleOpacity == 0.40f }
        assertEquals(0.40f, config.incomingBubbleOpacity, 0.01f)

        // Set outgoing to 85% (0.85f)
        wallpaperManager.setOutgoingBubbleOpacity(0.85f)
        config = wallpaperManager.globalConfigFlow.first { it.outgoingBubbleOpacity == 0.85f }
        assertEquals(0.85f, config.outgoingBubbleOpacity, 0.01f)
        // Incoming remains 0.40f
        assertEquals(0.40f, config.incomingBubbleOpacity, 0.01f)
    }

    @Test
    fun testLinkedOpacitySynchronizesValues() = runBlocking {
        // Enable linking
        wallpaperManager.setLinkedBubbleOpacity(true)
        val linkedConfig = wallpaperManager.globalConfigFlow.first { it.isLinkedBubbleOpacity }
        assertTrue(linkedConfig.isLinkedBubbleOpacity)

        // Updating incoming opacity updates both incoming and outgoing
        wallpaperManager.setIncomingBubbleOpacity(0.55f)
        val updatedConfig = wallpaperManager.globalConfigFlow.first { it.incomingBubbleOpacity == 0.55f && it.outgoingBubbleOpacity == 0.55f }
        assertEquals(0.55f, updatedConfig.incomingBubbleOpacity, 0.01f)
        assertEquals(0.55f, updatedConfig.outgoingBubbleOpacity, 0.01f)
    }

    @Test
    fun testOpacityRangeClampedBetween20And100Percent() = runBlocking {
        // Values below 20% should be clamped to 0.20f
        wallpaperManager.setIncomingBubbleOpacity(0.05f)
        var config = wallpaperManager.globalConfigFlow.first { it.incomingBubbleOpacity <= 0.21f }
        assertEquals(0.20f, config.incomingBubbleOpacity, 0.001f)

        // Values above 100% should be clamped to 1.0f
        wallpaperManager.setIncomingBubbleOpacity(1.5f)
        config = wallpaperManager.globalConfigFlow.first { it.incomingBubbleOpacity >= 0.99f }
        assertEquals(1.0f, config.incomingBubbleOpacity, 0.001f)
    }

    @Test
    fun testResetBubbleOpacityToDefault() = runBlocking {
        wallpaperManager.setPresetWallpaper("deep_mesh")
        wallpaperManager.setIncomingBubbleOpacity(0.30f)
        wallpaperManager.setOutgoingBubbleOpacity(0.90f)

        // Reset
        wallpaperManager.resetBubbleOpacityToDefault()
        val resetConfig = wallpaperManager.globalConfigFlow.first { it.incomingBubbleOpacity == 0.65f && it.outgoingBubbleOpacity == 0.65f }
        assertEquals(0.65f, resetConfig.incomingBubbleOpacity, 0.01f)
        assertEquals(0.65f, resetConfig.outgoingBubbleOpacity, 0.01f)
        assertTrue(resetConfig.isLinkedBubbleOpacity)
    }

    @Test
    fun testThemeColorsProvideOutgoingAndIncomingBubblesForAllPalettes() {
        ColorPalette.entries.forEach { palette ->
            // Dark mode colors
            val darkColors = ThemeColorFactory.createCustomColors(palette = palette, isDark = true)
            assertEquals("outgoingBubble should not be transparent in dark mode for $palette", 1f, darkColors.outgoingBubble.alpha, 0.01f)
            assertEquals("incomingBubble should not be transparent in dark mode for $palette", 1f, darkColors.incomingBubble.alpha, 0.01f)

            // Light mode colors
            val lightColors = ThemeColorFactory.createCustomColors(palette = palette, isDark = false)
            assertEquals("outgoingBubble should not be transparent in light mode for $palette", 1f, lightColors.outgoingBubble.alpha, 0.01f)
            assertEquals("incomingBubble should not be transparent in light mode for $palette", 1f, lightColors.incomingBubble.alpha, 0.01f)
        }
    }
}
