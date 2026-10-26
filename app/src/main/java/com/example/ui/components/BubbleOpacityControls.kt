package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.Opacity
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.ui.theme.VaultTheme
import com.example.util.PresetWallpapers
import com.example.util.WallpaperConfig
import com.example.util.WallpaperScale
import com.example.util.WallpaperType
import java.io.File

/**
 * Reusable control panel and real-time live preview for Message Bubble Opacity.
 * Allows independent incoming vs outgoing opacity controls or linked mode.
 */
@Composable
fun BubbleOpacityControls(
    wallpaperConfig: WallpaperConfig,
    onIncomingOpacityChange: (Float) -> Unit,
    onOutgoingOpacityChange: (Float) -> Unit,
    onLinkedChange: (Boolean) -> Unit,
    onResetToDefault: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = VaultTheme.colors

    Column(modifier = modifier.fillMaxWidth()) {
        // Section Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Opacity,
                contentDescription = null,
                tint = colors.accent,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = "شفافية فقاعات الرسائل",
                    color = colors.textPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "التحكم بشفافية خلفية الرسائل لإبراز جمال صورة الخلفية",
                    color = colors.textMuted,
                    fontSize = 10.5.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Live Real-Time Interactive Preview
        BubbleOpacityLivePreview(
            wallpaperConfig = wallpaperConfig,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Link Toggle: Independent vs Synchronized Incoming/Outgoing
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(colors.surfaceElevated)
                .padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = if (wallpaperConfig.isLinkedBubbleOpacity) Icons.Default.Link else Icons.Default.LinkOff,
                    contentDescription = null,
                    tint = if (wallpaperConfig.isLinkedBubbleOpacity) colors.accent else colors.textMuted,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "ربط شفافية الوارد والصادر معاً",
                        color = colors.textPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = if (wallpaperConfig.isLinkedBubbleOpacity) "القيمتان متطابقتان معاً" else "تحكم مستقل بالرسائل المستقبلة والصادرة",
                        color = colors.textMuted,
                        fontSize = 10.sp
                    )
                }
            }

            Switch(
                checked = wallpaperConfig.isLinkedBubbleOpacity,
                onCheckedChange = onLinkedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.Black,
                    checkedTrackColor = colors.accent,
                    uncheckedThumbColor = colors.textMuted,
                    uncheckedTrackColor = colors.surface
                )
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (wallpaperConfig.isLinkedBubbleOpacity) {
            // Single Unified Slider
            val percent = (wallpaperConfig.incomingBubbleOpacity * 100).toInt()
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "شفافية الفقاعات (الوارد والصادر): $percent%",
                    color = colors.textPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = if (wallpaperConfig.type == WallpaperType.CUSTOM_PHOTO || wallpaperConfig.type == WallpaperType.PRESET) "الافتراضي لخلفية الصورة: 65%" else "الافتراضي: 100%",
                    color = colors.textMuted,
                    fontSize = 10.sp
                )
            }

            Slider(
                value = wallpaperConfig.incomingBubbleOpacity,
                onValueChange = onIncomingOpacityChange,
                valueRange = 0.20f..1.00f,
                steps = 15,
                colors = SliderDefaults.colors(
                    thumbColor = colors.accent,
                    activeTrackColor = colors.accent,
                    inactiveTrackColor = colors.surfaceElevated
                ),
                modifier = Modifier.fillMaxWidth()
            )
        } else {
            // Independent Sliders: Incoming (المستقبلة) & Outgoing (الصادرة)
            val inPercent = (wallpaperConfig.incomingBubbleOpacity * 100).toInt()
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "شفافية الرسائل المستقبلة (الواردة): $inPercent%",
                    color = colors.textPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "20% - 100%",
                    color = colors.textMuted,
                    fontSize = 10.sp
                )
            }

            Slider(
                value = wallpaperConfig.incomingBubbleOpacity,
                onValueChange = onIncomingOpacityChange,
                valueRange = 0.20f..1.00f,
                steps = 15,
                colors = SliderDefaults.colors(
                    thumbColor = colors.accent,
                    activeTrackColor = colors.accent,
                    inactiveTrackColor = colors.surfaceElevated
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(6.dp))

            val outPercent = (wallpaperConfig.outgoingBubbleOpacity * 100).toInt()
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "شفافية الرسائل الصادرة: $outPercent%",
                    color = colors.textPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "20% - 100%",
                    color = colors.textMuted,
                    fontSize = 10.sp
                )
            }

            Slider(
                value = wallpaperConfig.outgoingBubbleOpacity,
                onValueChange = onOutgoingOpacityChange,
                valueRange = 0.20f..1.00f,
                steps = 15,
                colors = SliderDefaults.colors(
                    thumbColor = colors.secondary,
                    activeTrackColor = colors.secondary,
                    inactiveTrackColor = colors.surfaceElevated
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Reset to Default button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(
                onClick = onResetToDefault
            ) {
                Icon(
                    imageVector = Icons.Default.RestartAlt,
                    contentDescription = null,
                    tint = colors.accent,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "إعادة الشفافية للوضع الافتراضي",
                    color = colors.accent,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

/**
 * High-fidelity real-time preview card displaying sample bubbles over the exact active wallpaper.
 */
@Composable
fun BubbleOpacityLivePreview(
    wallpaperConfig: WallpaperConfig,
    modifier: Modifier = Modifier
) {
    val colors = VaultTheme.colors

    Card(
        modifier = modifier
            .height(160.dp)
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, colors.border, RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Layer 1: Simulated active wallpaper
            when (wallpaperConfig.type) {
                WallpaperType.CUSTOM_PHOTO -> {
                    wallpaperConfig.customImagePath?.let { path ->
                        val file = File(path)
                        if (file.exists()) {
                            val scale = when (wallpaperConfig.scaleMode) {
                                WallpaperScale.FILL_CROP -> ContentScale.Crop
                                WallpaperScale.FIT -> ContentScale.Fit
                                WallpaperScale.CENTER -> ContentScale.Inside
                            }
                            AsyncImage(
                                model = file,
                                contentDescription = null,
                                contentScale = scale,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .then(if (wallpaperConfig.isBlurEnabled) Modifier.blur(12.dp) else Modifier)
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black.copy(alpha = wallpaperConfig.dimmingOpacity))
                            )
                        } else {
                            FallbackPreviewPattern()
                        }
                    } ?: FallbackPreviewPattern()
                }

                WallpaperType.PRESET -> {
                    val preset = PresetWallpapers.find(wallpaperConfig.presetId) ?: PresetWallpapers.list.first()
                    val gradColors = preset.previewGradientColors.map { Color(it) }
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Brush.verticalGradient(gradColors))
                            .then(if (wallpaperConfig.isBlurEnabled) Modifier.blur(12.dp) else Modifier)
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = wallpaperConfig.dimmingOpacity))
                    )
                }

                WallpaperType.DEFAULT -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(colors.background)
                    )
                }
            }

            // Layer 2: Sample Bubbles with Live Opacity
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(10.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Incoming Sample Bubble (aligned to start / RTL right)
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 14.dp, bottomStart = 14.dp, bottomEnd = 14.dp))
                        .background(colors.incomingBubble.copy(alpha = wallpaperConfig.incomingBubbleOpacity))
                        .border(
                            width = 1.dp,
                            color = colors.incomingBubbleBorder.copy(alpha = wallpaperConfig.incomingBubbleOpacity.coerceAtLeast(0.4f)),
                            shape = RoundedCornerShape(topStart = 4.dp, topEnd = 14.dp, bottomStart = 14.dp, bottomEnd = 14.dp)
                        )
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Column {
                        Text(
                            text = "أحمد خالد (رسالة واردة)",
                            color = colors.primary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "السلام عليكم، كيف حالك اليوم؟",
                            color = colors.textPrimary,
                            fontSize = 12.sp,
                            style = TextStyle(
                                shadow = if (wallpaperConfig.incomingBubbleOpacity < 0.85f) {
                                    Shadow(color = if (colors.isDark) Color.Black.copy(alpha = 0.8f) else Color.White.copy(alpha = 0.9f), blurRadius = 4f)
                                } else null
                            )
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "10:30 ص",
                            color = colors.textMuted,
                            fontSize = 9.sp,
                            modifier = Modifier.align(Alignment.End)
                        )
                    }
                }

                // Outgoing Sample Bubble (aligned to end / RTL left)
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.82f)
                        .align(Alignment.End)
                        .clip(RoundedCornerShape(topStart = 14.dp, topEnd = 4.dp, bottomStart = 14.dp, bottomEnd = 14.dp))
                        .background(colors.outgoingBubble.copy(alpha = wallpaperConfig.outgoingBubbleOpacity))
                        .border(
                            width = 1.dp,
                            color = colors.outgoingBubbleBorder.copy(alpha = wallpaperConfig.outgoingBubbleOpacity.coerceAtLeast(0.4f)),
                            shape = RoundedCornerShape(topStart = 14.dp, topEnd = 4.dp, bottomStart = 14.dp, bottomEnd = 14.dp)
                        )
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Column {
                        Text(
                            text = "أنا (رسالة صادرة)",
                            color = colors.secondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "وعليكم السلام، كل شيء ممتاز والحمد لله",
                            color = colors.textPrimary,
                            fontSize = 12.sp,
                            style = TextStyle(
                                shadow = if (wallpaperConfig.outgoingBubbleOpacity < 0.85f) {
                                    Shadow(color = if (colors.isDark) Color.Black.copy(alpha = 0.8f) else Color.White.copy(alpha = 0.9f), blurRadius = 4f)
                                } else null
                            )
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "10:31 ص ✓✓",
                            color = colors.textMuted,
                            fontSize = 9.sp,
                            modifier = Modifier.align(Alignment.End)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FallbackPreviewPattern() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    listOf(
                        Color(0xFF0F172A),
                        Color(0xFF064E3B),
                        Color(0xFF1E1B4B)
                    )
                )
            )
    )
}
