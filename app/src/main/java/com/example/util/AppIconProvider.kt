package com.example.util

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.annotation.DrawableRes
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.example.R
import java.util.concurrent.ConcurrentHashMap

object AppIconProvider {

    private val iconCache = ConcurrentHashMap<String, ImageBitmap>()
    private val appLabelCache = ConcurrentHashMap<String, String>()

    /**
     * Attempts to load the real application icon via Android PackageManager.
     * Caches the result in memory.
     * Returns null if package is not installed or icon cannot be loaded.
     */
    fun getAppIcon(context: Context, packageName: String): ImageBitmap? {
        if (packageName.isBlank()) return null
        iconCache[packageName]?.let { return it }

        return try {
            val pm = context.packageManager
            val drawable = pm.getApplicationIcon(packageName)
            val bitmap = drawableToBitmap(drawable)
            val imageBitmap = bitmap.asImageBitmap()
            iconCache[packageName] = imageBitmap
            imageBitmap
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Resolves human-readable app label via PackageManager or clean Arabic defaults.
     */
    fun getAppLabel(context: Context, packageName: String, isClonedApp: Boolean = false): String {
        val cached = appLabelCache[packageName]
        val baseName = if (cached != null) {
            cached
        } else {
            val resolved = try {
                val pm = context.packageManager
                val appInfo = pm.getApplicationInfo(packageName, 0)
                pm.getApplicationLabel(appInfo).toString()
            } catch (e: Exception) {
                resolveFallbackName(packageName)
            }
            appLabelCache[packageName] = resolved
            resolved
        }

        return if (isClonedApp) "$baseName (مستنسخ)" else baseName
    }

    /**
     * Fallback vector drawable resource when app icon cannot be loaded from PackageManager.
     */
    @DrawableRes
    fun getFallbackIconRes(packageName: String): Int {
        val lower = packageName.lowercase()
        return when {
            lower == "com.whatsapp.w4b" -> R.drawable.ic_brand_whatsapp_business
            lower.contains("whatsapp") -> R.drawable.ic_brand_whatsapp
            lower.contains("telegram") -> R.drawable.ic_brand_telegram
            lower.contains("orca") || lower.contains("messenger") -> R.drawable.ic_brand_messenger
            lower.contains("signal") || lower.contains("securesms") -> R.drawable.ic_brand_signal
            lower.contains("mms") || lower.contains("messaging") -> R.drawable.ic_brand_sms
            else -> R.drawable.ic_brand_generic
        }
    }

    private fun resolveFallbackName(packageName: String): String {
        val lower = packageName.lowercase()
        return when {
            lower == "com.whatsapp.w4b" -> "واتساب للأعمال"
            lower.contains("whatsapp") -> "واتساب"
            lower.contains("telegram") -> "تيليجرام"
            lower.contains("orca") || lower.contains("messenger") -> "ماسنجر"
            lower.contains("signal") || lower.contains("securesms") -> "سيجنال"
            lower.contains("mms") || lower.contains("messaging") -> "رسائل SMS"
            lower.contains("instagram") -> "إنستغرام"
            else -> "محادثة"
        }
    }

    private fun drawableToBitmap(drawable: Drawable): Bitmap {
        if (drawable is BitmapDrawable && drawable.bitmap != null) {
            return drawable.bitmap
        }
        val width = if (drawable.intrinsicWidth > 0) drawable.intrinsicWidth else 96
        val height = if (drawable.intrinsicHeight > 0) drawable.intrinsicHeight else 96
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }
}
