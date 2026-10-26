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
    val scaleMode: WallpaperScale = WallpaperScale.FILL_CROP,
    // Message bubble opacity (20% to 100%, default 65% when custom wallpaper is chosen)
    val incomingBubbleOpacity: Float = 1.0f,
    val outgoingBubbleOpacity: Float = 1.0f,
    val isLinkedBubbleOpacity: Boolean = true
) {
    val wallpaperType: WallpaperType get() = type
    val dimAlpha: Float get() = dimmingOpacity
    val isBlurred: Boolean get() = isBlurEnabled
    val incomingOpacity: Float get() = incomingBubbleOpacity
    val outgoingOpacity: Float get() = outgoingBubbleOpacity
    val isLinked: Boolean get() = isLinkedBubbleOpacity
}

class ChatWallpaperManager private constructor(private val context: Context) {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    val globalWallpaperFlow: Flow<WallpaperConfig> = context.wallpaperDataStore.data.map { prefs ->
        val type = WallpaperType.fromId(prefs[KEY_GLOBAL_TYPE] ?: WallpaperType.DEFAULT.id)
        val hasCustomPhotoOrPreset = (type == WallpaperType.CUSTOM_PHOTO || type == WallpaperType.PRESET)
        val defaultOpacity = if (hasCustomPhotoOrPreset) 0.65f else 1.0f

        val incoming = prefs[KEY_GLOBAL_INCOMING_OPACITY] ?: defaultOpacity
        val outgoing = prefs[KEY_GLOBAL_OUTGOING_OPACITY] ?: defaultOpacity
        val linked = prefs[KEY_GLOBAL_LINKED_OPACITY] ?: true

        WallpaperConfig(
            type = type,
            presetId = prefs[KEY_GLOBAL_PRESET] ?: "mesh_dark",
            customImagePath = prefs[KEY_GLOBAL_IMAGE_PATH],
            dimmingOpacity = prefs[KEY_GLOBAL_DIMMING] ?: 0.40f,
            isBlurEnabled = prefs[KEY_GLOBAL_BLUR] ?: false,
            scaleMode = WallpaperScale.fromId(prefs[KEY_GLOBAL_SCALE] ?: WallpaperScale.FILL_CROP.id),
            incomingBubbleOpacity = incoming.coerceIn(0.20f, 1.00f),
            outgoingBubbleOpacity = outgoing.coerceIn(0.20f, 1.00f),
            isLinkedBubbleOpacity = linked
        )
    }

    val globalConfigFlow: Flow<WallpaperConfig> get() = globalWallpaperFlow
    val presetWallpapers: List<PresetWallpaper> get() = PresetWallpapers.list

    fun setDefaultWallpaper(forConversationId: Long? = null) {
        if (forConversationId == null) {
            setGlobalDefault()
        } else {
            removeConversationOverride(forConversationId)
        }
    }

    fun setPresetWallpaper(presetId: String, forConversationId: Long? = null) {
        if (forConversationId == null) {
            setGlobalPreset(presetId)
        } else {
            scope.launch {
                context.wallpaperDataStore.edit { prefs ->
                    prefs[stringPreferencesKey("conv_${forConversationId}_type")] = WallpaperType.PRESET.id
                    prefs[stringPreferencesKey("conv_${forConversationId}_preset")] = presetId
                    if (prefs[floatPreferencesKey("conv_${forConversationId}_dimming")] == null) {
                        prefs[floatPreferencesKey("conv_${forConversationId}_dimming")] = 0.40f
                    }
                    if (prefs[floatPreferencesKey("conv_${forConversationId}_incoming_opacity")] == null) {
                        prefs[floatPreferencesKey("conv_${forConversationId}_incoming_opacity")] = 0.65f
                    }
                    if (prefs[floatPreferencesKey("conv_${forConversationId}_outgoing_opacity")] == null) {
                        prefs[floatPreferencesKey("conv_${forConversationId}_outgoing_opacity")] = 0.65f
                    }
                }
            }
        }
    }

    suspend fun setCustomPhotoWallpaper(uri: Uri, forConversationId: Long? = null): Boolean =
        saveCustomPhotoFromUri(uri, forConversationId) != null

    fun setDimAlpha(alpha: Float, forConversationId: Long? = null) {
        val safeDimming = alpha.coerceIn(0.0f, 0.80f)
        if (forConversationId == null) {
            updateGlobalDimming(safeDimming)
        } else {
            scope.launch {
                context.wallpaperDataStore.edit { prefs ->
                    prefs[floatPreferencesKey("conv_${forConversationId}_dimming")] = safeDimming
                }
            }
        }
    }

    fun setBlurred(blurred: Boolean, forConversationId: Long? = null) {
        if (forConversationId == null) {
            updateGlobalBlur(blurred)
        } else {
            scope.launch {
                context.wallpaperDataStore.edit { prefs ->
                    prefs[booleanPreferencesKey("conv_${forConversationId}_blur")] = blurred
                }
            }
        }
    }

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
                if (prefs[KEY_GLOBAL_INCOMING_OPACITY] == null) {
                    prefs[KEY_GLOBAL_INCOMING_OPACITY] = 0.65f
                }
                if (prefs[KEY_GLOBAL_OUTGOING_OPACITY] == null) {
                    prefs[KEY_GLOBAL_OUTGOING_OPACITY] = 0.65f
                }
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
                    prefs[KEY_GLOBAL_DIMMING] = 0.40f // Default 40% dimming
                    // Default 65% bubble opacity when choosing custom photo
                    if (prefs[KEY_GLOBAL_INCOMING_OPACITY] == null) {
                        prefs[KEY_GLOBAL_INCOMING_OPACITY] = 0.65f
                    }
                    if (prefs[KEY_GLOBAL_OUTGOING_OPACITY] == null) {
                        prefs[KEY_GLOBAL_OUTGOING_OPACITY] = 0.65f
                    }
                }
            } else {
                context.wallpaperDataStore.edit { prefs ->
                    prefs[stringPreferencesKey("conv_${isForConversationId}_type")] = WallpaperType.CUSTOM_PHOTO.id
                    prefs[stringPreferencesKey("conv_${isForConversationId}_image_path")] = savedPath
                    prefs[floatPreferencesKey("conv_${isForConversationId}_dimming")] = 0.40f
                    if (prefs[floatPreferencesKey("conv_${isForConversationId}_incoming_opacity")] == null) {
                        prefs[floatPreferencesKey("conv_${isForConversationId}_incoming_opacity")] = 0.65f
                    }
                    if (prefs[floatPreferencesKey("conv_${isForConversationId}_outgoing_opacity")] == null) {
                        prefs[floatPreferencesKey("conv_${isForConversationId}_outgoing_opacity")] = 0.65f
                    }
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

    fun setIncomingBubbleOpacity(opacity: Float, forConversationId: Long? = null) {
        val safe = opacity.coerceIn(0.20f, 1.00f)
        scope.launch {
            context.wallpaperDataStore.edit { prefs ->
                val linkedKey = if (forConversationId == null) KEY_GLOBAL_LINKED_OPACITY else booleanPreferencesKey("conv_${forConversationId}_linked_opacity")
                val isLinked = prefs[linkedKey] ?: true

                if (forConversationId == null) {
                    prefs[KEY_GLOBAL_INCOMING_OPACITY] = safe
                    if (isLinked) {
                        prefs[KEY_GLOBAL_OUTGOING_OPACITY] = safe
                    }
                } else {
                    prefs[floatPreferencesKey("conv_${forConversationId}_incoming_opacity")] = safe
                    if (isLinked) {
                        prefs[floatPreferencesKey("conv_${forConversationId}_outgoing_opacity")] = safe
                    }
                }
            }
        }
    }

    fun setOutgoingBubbleOpacity(opacity: Float, forConversationId: Long? = null) {
        val safe = opacity.coerceIn(0.20f, 1.00f)
        scope.launch {
            context.wallpaperDataStore.edit { prefs ->
                val linkedKey = if (forConversationId == null) KEY_GLOBAL_LINKED_OPACITY else booleanPreferencesKey("conv_${forConversationId}_linked_opacity")
                val isLinked = prefs[linkedKey] ?: true

                if (forConversationId == null) {
                    prefs[KEY_GLOBAL_OUTGOING_OPACITY] = safe
                    if (isLinked) {
                        prefs[KEY_GLOBAL_INCOMING_OPACITY] = safe
                    }
                } else {
                    prefs[floatPreferencesKey("conv_${forConversationId}_outgoing_opacity")] = safe
                    if (isLinked) {
                        prefs[floatPreferencesKey("conv_${forConversationId}_incoming_opacity")] = safe
                    }
                }
            }
        }
    }

    fun setLinkedBubbleOpacity(isLinked: Boolean, forConversationId: Long? = null) {
        scope.launch {
            context.wallpaperDataStore.edit { prefs ->
                if (forConversationId == null) {
                    prefs[KEY_GLOBAL_LINKED_OPACITY] = isLinked
                    if (isLinked) {
                        val current = prefs[KEY_GLOBAL_INCOMING_OPACITY] ?: 0.65f
                        prefs[KEY_GLOBAL_OUTGOING_OPACITY] = current
                    }
                } else {
                    prefs[booleanPreferencesKey("conv_${forConversationId}_linked_opacity")] = isLinked
                    if (isLinked) {
                        val current = prefs[floatPreferencesKey("conv_${forConversationId}_incoming_opacity")]
                            ?: prefs[KEY_GLOBAL_INCOMING_OPACITY] ?: 0.65f
                        prefs[floatPreferencesKey("conv_${forConversationId}_outgoing_opacity")] = current
                    }
                }
            }
        }
    }

    fun resetBubbleOpacityToDefault(forConversationId: Long? = null) {
        scope.launch {
            context.wallpaperDataStore.edit { prefs ->
                val type = if (forConversationId == null) {
                    WallpaperType.fromId(prefs[KEY_GLOBAL_TYPE] ?: WallpaperType.DEFAULT.id)
                } else {
                    WallpaperType.fromId(prefs[stringPreferencesKey("conv_${forConversationId}_type")] ?: prefs[KEY_GLOBAL_TYPE] ?: WallpaperType.DEFAULT.id)
                }
                val defaultTarget = if (type == WallpaperType.CUSTOM_PHOTO || type == WallpaperType.PRESET) 0.65f else 1.0f

                if (forConversationId == null) {
                    prefs[KEY_GLOBAL_INCOMING_OPACITY] = defaultTarget
                    prefs[KEY_GLOBAL_OUTGOING_OPACITY] = defaultTarget
                    prefs[KEY_GLOBAL_LINKED_OPACITY] = true
                } else {
                    prefs[floatPreferencesKey("conv_${forConversationId}_incoming_opacity")] = defaultTarget
                    prefs[floatPreferencesKey("conv_${forConversationId}_outgoing_opacity")] = defaultTarget
                    prefs[booleanPreferencesKey("conv_${forConversationId}_linked_opacity")] = true
                }
            }
        }
    }

    fun getConversationWallpaperFlow(conversationId: Long): Flow<WallpaperConfig> {
        return context.wallpaperDataStore.data.map { prefs ->
            val hasCustomConv = prefs[stringPreferencesKey("conv_${conversationId}_type")] != null
            if (hasCustomConv) {
                val type = WallpaperType.fromId(prefs[stringPreferencesKey("conv_${conversationId}_type")] ?: WallpaperType.DEFAULT.id)
                val hasPhoto = (type == WallpaperType.CUSTOM_PHOTO || type == WallpaperType.PRESET)
                val defaultOpacity = if (hasPhoto) 0.65f else 1.0f

                val inc = prefs[floatPreferencesKey("conv_${conversationId}_incoming_opacity")] ?: prefs[KEY_GLOBAL_INCOMING_OPACITY] ?: defaultOpacity
                val out = prefs[floatPreferencesKey("conv_${conversationId}_outgoing_opacity")] ?: prefs[KEY_GLOBAL_OUTGOING_OPACITY] ?: defaultOpacity
                val linked = prefs[booleanPreferencesKey("conv_${conversationId}_linked_opacity")] ?: prefs[KEY_GLOBAL_LINKED_OPACITY] ?: true

                WallpaperConfig(
                    type = type,
                    presetId = prefs[stringPreferencesKey("conv_${conversationId}_preset")] ?: "mesh_dark",
                    customImagePath = prefs[stringPreferencesKey("conv_${conversationId}_image_path")],
                    dimmingOpacity = prefs[floatPreferencesKey("conv_${conversationId}_dimming")] ?: 0.40f,
                    isBlurEnabled = prefs[booleanPreferencesKey("conv_${conversationId}_blur")] ?: false,
                    scaleMode = WallpaperScale.fromId(prefs[stringPreferencesKey("conv_${conversationId}_scale")] ?: WallpaperScale.FILL_CROP.id),
                    incomingBubbleOpacity = inc.coerceIn(0.20f, 1.00f),
                    outgoingBubbleOpacity = out.coerceIn(0.20f, 1.00f),
                    isLinkedBubbleOpacity = linked
                )
            } else {
                // Fallback to global config
                val type = WallpaperType.fromId(prefs[KEY_GLOBAL_TYPE] ?: WallpaperType.DEFAULT.id)
                val hasPhoto = (type == WallpaperType.CUSTOM_PHOTO || type == WallpaperType.PRESET)
                val defaultOpacity = if (hasPhoto) 0.65f else 1.0f

                val inc = prefs[KEY_GLOBAL_INCOMING_OPACITY] ?: defaultOpacity
                val out = prefs[KEY_GLOBAL_OUTGOING_OPACITY] ?: defaultOpacity
                val linked = prefs[KEY_GLOBAL_LINKED_OPACITY] ?: true

                WallpaperConfig(
                    type = type,
                    presetId = prefs[KEY_GLOBAL_PRESET] ?: "mesh_dark",
                    customImagePath = prefs[KEY_GLOBAL_IMAGE_PATH],
                    dimmingOpacity = prefs[KEY_GLOBAL_DIMMING] ?: 0.40f,
                    isBlurEnabled = prefs[KEY_GLOBAL_BLUR] ?: false,
                    scaleMode = WallpaperScale.fromId(prefs[KEY_GLOBAL_SCALE] ?: WallpaperScale.FILL_CROP.id),
                    incomingBubbleOpacity = inc.coerceIn(0.20f, 1.00f),
                    outgoingBubbleOpacity = out.coerceIn(0.20f, 1.00f),
                    isLinkedBubbleOpacity = linked
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
                prefs.remove(floatPreferencesKey("conv_${conversationId}_incoming_opacity"))
                prefs.remove(floatPreferencesKey("conv_${conversationId}_outgoing_opacity"))
                prefs.remove(booleanPreferencesKey("conv_${conversationId}_linked_opacity"))
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

        private val KEY_GLOBAL_INCOMING_OPACITY = floatPreferencesKey("wp_global_incoming_opacity")
        private val KEY_GLOBAL_OUTGOING_OPACITY = floatPreferencesKey("wp_global_outgoing_opacity")
        private val KEY_GLOBAL_LINKED_OPACITY = booleanPreferencesKey("wp_global_linked_opacity")

        @Volatile
        private var instance: ChatWallpaperManager? = null

        fun get(context: Context): ChatWallpaperManager {
            return instance ?: synchronized(this) {
                instance ?: ChatWallpaperManager(context.applicationContext).also { instance = it }
            }
        }
    }
}
