package com.example.util

import android.content.Context
import android.net.Uri
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
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
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

val Context.wallpaperDataStore: DataStore<Preferences> by preferencesDataStore(name = "wallpaper_preferences")

enum class WallpaperType(val id: String, val title: String) {
    DEFAULT("DEFAULT", "الخلفية الافتراضية"),
    PRESET("PRESET", "خلفية جاهزة من التطبيق"),
    CUSTOM_PHOTO("CUSTOM_PHOTO", "صورة شخصية من المعرض");

    companion object {
        fun fromId(id: String): WallpaperType = entries.find { it.id == id } ?: DEFAULT
    }
}

enum class WallpaperScale(val id: String, val title: String) {
    FILL_CROP("FILL_CROP", "ملء الشاشة وتوسيط (Fill Crop)"),
    FIT("FIT", "احتواء كامل الصورة (Fit)"),
    CENTER("CENTER", "الحجم الطبيعي وسط الشاشة (Center)");

    companion object {
        fun fromId(id: String): WallpaperScale = entries.find { it.id == id } ?: FILL_CROP
    }
}

data class PresetWallpaper(
    val id: String,
    val title: String,
    val description: String,
    val previewGradientColors: List<Long>
) {
    val nameAr: String get() = title
    val gradientColors: List<androidx.compose.ui.graphics.Color> get() = previewGradientColors.map { androidx.compose.ui.graphics.Color(it) }
}

object PresetWallpapers {
    val list = listOf(
        PresetWallpaper("mesh_dark", "شبكة التمويه الداكنة", "نمط هندسي داكن وعصري", listOf(0xFF0F172A, 0xFF020617)),
        PresetWallpaper("emerald_glow", "توهج الزمرد", "تدرج هادئ بألوان الأخضر الداكن", listOf(0xFF064E3B, 0xFF022C22)),
        PresetWallpaper("midnight_blue", "سماء منتصف الليل", "أزرق عميق يمنح راحة للعين", listOf(0xFF1E1B4B, 0xFF0F172A)),
        PresetWallpaper("amethyst_purple", "حجر الجمشت", "بنفسجي داكن ملكي وأنيق", listOf(0xFF3B0764, 0xFF1E0533)),
        PresetWallpaper("minimal_carbon", "ألياف الكربون المينيمال", "رمادي داكن ناعم فائق البساطة", listOf(0xFF1E293B, 0xFF0F172A))
    )

    fun find(id: String): PresetWallpaper? = list.find { it.id == id }
}

data class WallpaperConfig(
    val type: WallpaperType = WallpaperType.DEFAULT,
    val presetId: String = "mesh_dark",
    val customImagePath: String? = null,
    val dimmingOpacity: Float = 0.40f, // Default 40%
    val isBlurEnabled: Boolean = false,
    val scaleMode: WallpaperScale = WallpaperScale.FILL_CROP
) {
    val wallpaperType: WallpaperType get() = type
    val dimAlpha: Float get() = dimmingOpacity
    val isBlurred: Boolean get() = isBlurEnabled
}

class ChatWallpaperManager private constructor(private val context: Context) {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    val globalWallpaperFlow: Flow<WallpaperConfig> = context.wallpaperDataStore.data.map { prefs ->
        WallpaperConfig(
            type = WallpaperType.fromId(prefs[KEY_GLOBAL_TYPE] ?: WallpaperType.DEFAULT.id),
            presetId = prefs[KEY_GLOBAL_PRESET] ?: "mesh_dark",
            customImagePath = prefs[KEY_GLOBAL_IMAGE_PATH],
            dimmingOpacity = prefs[KEY_GLOBAL_DIMMING] ?: 0.40f,
            isBlurEnabled = prefs[KEY_GLOBAL_BLUR] ?: false,
            scaleMode = WallpaperScale.fromId(prefs[KEY_GLOBAL_SCALE] ?: WallpaperScale.FILL_CROP.id)
        )
    }

    val globalConfigFlow: Flow<WallpaperConfig> get() = globalWallpaperFlow
    val presetWallpapers: List<PresetWallpaper> get() = PresetWallpapers.list

    fun setDefaultWallpaper() = setGlobalDefault()
    fun setPresetWallpaper(presetId: String) = setGlobalPreset(presetId)
    suspend fun setCustomPhotoWallpaper(uri: Uri): Boolean = saveCustomPhotoFromUri(uri) != null
    fun setDimAlpha(alpha: Float) = updateGlobalDimming(alpha)
    fun setBlurred(blurred: Boolean) = updateGlobalBlur(blurred)

    val globalWallpaperState: StateFlow<WallpaperConfig> = globalWallpaperFlow.stateIn(
        scope = scope,
        started = SharingStarted.Eagerly,
        initialValue = WallpaperConfig()
    )

    fun setGlobalDefault() {
        scope.launch {
            context.wallpaperDataStore.edit { prefs ->
                prefs[KEY_GLOBAL_TYPE] = WallpaperType.DEFAULT.id
            }
        }
    }

    fun setGlobalPreset(presetId: String, dimming: Float = 0.40f, blur: Boolean = false) {
        scope.launch {
            context.wallpaperDataStore.edit { prefs ->
                prefs[KEY_GLOBAL_TYPE] = WallpaperType.PRESET.id
                prefs[KEY_GLOBAL_PRESET] = presetId
                prefs[KEY_GLOBAL_DIMMING] = dimming
                prefs[KEY_GLOBAL_BLUR] = blur
            }
        }
    }

    suspend fun saveCustomPhotoFromUri(uri: Uri, isForConversationId: Long? = null): String? = withContext(Dispatchers.IO) {
        try {
            val wallpapersDir = File(context.filesDir, "wallpapers").apply { mkdirs() }
            val suffix = if (isForConversationId != null) "conv_${isForConversationId}" else "global"
            val targetFile = File(wallpapersDir, "bg_${suffix}_${System.currentTimeMillis()}.jpg")

            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                FileOutputStream(targetFile).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }

            val savedPath = targetFile.absolutePath

            if (isForConversationId == null) {
                context.wallpaperDataStore.edit { prefs ->
                    prefs[KEY_GLOBAL_TYPE] = WallpaperType.CUSTOM_PHOTO.id
                    prefs[KEY_GLOBAL_IMAGE_PATH] = savedPath
                    prefs[KEY_GLOBAL_DIMMING] = 0.40f // Default 40%
                }
            } else {
                context.wallpaperDataStore.edit { prefs ->
                    prefs[stringPreferencesKey("conv_${isForConversationId}_type")] = WallpaperType.CUSTOM_PHOTO.id
                    prefs[stringPreferencesKey("conv_${isForConversationId}_image_path")] = savedPath
                    prefs[floatPreferencesKey("conv_${isForConversationId}_dimming")] = 0.40f
                }
            }

            savedPath
        } catch (e: Exception) {
            null
        }
    }

    fun updateGlobalDimming(dimming: Float) {
        val safeDimming = dimming.coerceIn(0.0f, 0.80f)
        scope.launch {
            context.wallpaperDataStore.edit { it[KEY_GLOBAL_DIMMING] = safeDimming }
        }
    }

    fun updateGlobalBlur(blur: Boolean) {
        scope.launch {
            context.wallpaperDataStore.edit { it[KEY_GLOBAL_BLUR] = blur }
        }
    }

    fun updateGlobalScale(scale: WallpaperScale) {
        scope.launch {
            context.wallpaperDataStore.edit { it[KEY_GLOBAL_SCALE] = scale.id }
        }
    }

    fun getConversationWallpaperFlow(conversationId: Long): Flow<WallpaperConfig> {
        return context.wallpaperDataStore.data.map { prefs ->
            val hasCustomConv = prefs[stringPreferencesKey("conv_${conversationId}_type")] != null
            if (hasCustomConv) {
                WallpaperConfig(
                    type = WallpaperType.fromId(prefs[stringPreferencesKey("conv_${conversationId}_type")] ?: WallpaperType.DEFAULT.id),
                    presetId = prefs[stringPreferencesKey("conv_${conversationId}_preset")] ?: "mesh_dark",
                    customImagePath = prefs[stringPreferencesKey("conv_${conversationId}_image_path")],
                    dimmingOpacity = prefs[floatPreferencesKey("conv_${conversationId}_dimming")] ?: 0.40f,
                    isBlurEnabled = prefs[booleanPreferencesKey("conv_${conversationId}_blur")] ?: false,
                    scaleMode = WallpaperScale.fromId(prefs[stringPreferencesKey("conv_${conversationId}_scale")] ?: WallpaperScale.FILL_CROP.id)
                )
            } else {
                // Fallback to global config
                WallpaperConfig(
                    type = WallpaperType.fromId(prefs[KEY_GLOBAL_TYPE] ?: WallpaperType.DEFAULT.id),
                    presetId = prefs[KEY_GLOBAL_PRESET] ?: "mesh_dark",
                    customImagePath = prefs[KEY_GLOBAL_IMAGE_PATH],
                    dimmingOpacity = prefs[KEY_GLOBAL_DIMMING] ?: 0.40f,
                    isBlurEnabled = prefs[KEY_GLOBAL_BLUR] ?: false,
                    scaleMode = WallpaperScale.fromId(prefs[KEY_GLOBAL_SCALE] ?: WallpaperScale.FILL_CROP.id)
                )
            }
        }
    }

    fun removeConversationOverride(conversationId: Long) {
        scope.launch {
            context.wallpaperDataStore.edit { prefs ->
                prefs.remove(stringPreferencesKey("conv_${conversationId}_type"))
                prefs.remove(stringPreferencesKey("conv_${conversationId}_preset"))
                prefs.remove(stringPreferencesKey("conv_${conversationId}_image_path"))
                prefs.remove(floatPreferencesKey("conv_${conversationId}_dimming"))
                prefs.remove(booleanPreferencesKey("conv_${conversationId}_blur"))
                prefs.remove(stringPreferencesKey("conv_${conversationId}_scale"))
            }
        }
    }

    companion object {
        private val KEY_GLOBAL_TYPE = stringPreferencesKey("wp_global_type")
        private val KEY_GLOBAL_PRESET = stringPreferencesKey("wp_global_preset")
        private val KEY_GLOBAL_IMAGE_PATH = stringPreferencesKey("wp_global_image_path")
        private val KEY_GLOBAL_DIMMING = floatPreferencesKey("wp_global_dimming")
        private val KEY_GLOBAL_BLUR = booleanPreferencesKey("wp_global_blur")
        private val KEY_GLOBAL_SCALE = stringPreferencesKey("wp_global_scale")

        @Volatile
        private var instance: ChatWallpaperManager? = null

        fun get(context: Context): ChatWallpaperManager {
            return instance ?: synchronized(this) {
                instance ?: ChatWallpaperManager(context.applicationContext).also { instance = it }
            }
        }
    }
}
