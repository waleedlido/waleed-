package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.DarkEmerald
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.SurfaceElevatedDark
import com.example.ui.theme.VaultTheme
import com.example.util.AppIconProvider

/**
 * Modern circular Avatar (48-56dp) displaying the real application logo via PackageManager,
 * with high-fidelity vector fallback, and a single neat «مستنسخ» badge for HONOR App Twin.
 * Completely replaces the outdated letter circle without icon duplication.
 */
@Composable
fun AppAvatarIcon(
    packageName: String,
    isClonedApp: Boolean,
    chatTitle: String = "",
    modifier: Modifier = Modifier,
    size: Dp = 50.dp
) {
    val context = LocalContext.current
    val appIcon = remember(packageName) {
        AppIconProvider.getAppIcon(context, packageName)
    }
    val fallbackRes = remember(packageName) {
        AppIconProvider.getFallbackIconRes(packageName)
    }
    val appLabel = remember(packageName, isClonedApp) {
        AppIconProvider.getAppLabel(context, packageName, isClonedApp)
    }

    val themeColors = VaultTheme.colors

    Box(
        modifier = modifier.size(if (isClonedApp) size + 4.dp else size),
        contentAlignment = Alignment.Center
    ) {
        // Main Avatar Circle: Background Container with clean border
        Box(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(if (themeColors.isDark) SurfaceElevatedDark else Color(0xFFF1F5F9))
                .border(
                    width = 1.2.dp,
                    color = if (isClonedApp) CyanAccent.copy(alpha = 0.8f) else themeColors.border,
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            val iconInnerSize = size * 0.70f
            if (appIcon != null) {
                Image(
                    bitmap = appIcon,
                    contentDescription = appLabel,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .size(iconInnerSize)
                        .clip(CircleShape)
                )
            } else {
                Image(
                    painter = painterResource(id = fallbackRes),
                    contentDescription = appLabel,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .size(iconInnerSize)
                )
            }
        }

        // Single sleek micro layers icon badge for HONOR MagicOS App Twin (No text truncation, unobtrusive)
        if (isClonedApp) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = 1.dp, y = 1.dp)
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF031620))
                    .border(0.8.dp, CyanAccent, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Layers,
                    contentDescription = "نسخة مستنسخة (App Twin)",
                    tint = CyanAccent,
                    modifier = Modifier.size(9.dp)
                )
            }
        }
    }
}

/**
 * Standardized single app icon (24-28dp) for top app bars or specific headers.
 */
@Composable
fun AppSourceIcon(
    packageName: String,
    isClonedApp: Boolean,
    modifier: Modifier = Modifier,
    size: Dp = 26.dp,
    showCloneBadge: Boolean = true
) {
    val context = LocalContext.current
    val appIcon = remember(packageName) {
        AppIconProvider.getAppIcon(context, packageName)
    }
    val fallbackRes = remember(packageName) {
        AppIconProvider.getFallbackIconRes(packageName)
    }
    val appLabel = remember(packageName, isClonedApp) {
        AppIconProvider.getAppLabel(context, packageName, isClonedApp)
    }

    Box(
        modifier = modifier.size(if (isClonedApp && showCloneBadge) size + 4.dp else size),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(SurfaceElevatedDark)
                .border(1.dp, if (isClonedApp) CyanAccent.copy(alpha = 0.8f) else Color(0x33FFFFFF), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (appIcon != null) {
                Image(
                    bitmap = appIcon,
                    contentDescription = appLabel,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.size(size * 0.75f).clip(CircleShape)
                )
            } else {
                Image(
                    painter = painterResource(id = fallbackRes),
                    contentDescription = appLabel,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.size(size * 0.75f)
                )
            }
        }

        if (isClonedApp && showCloneBadge) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = 1.dp, y = 1.dp)
                    .size(13.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF031620))
                    .border(0.7.dp, CyanAccent, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Layers,
                    contentDescription = "نسخة مستنسخة (App Twin)",
                    tint = CyanAccent,
                    modifier = Modifier.size(7.dp)
                )
            }
        }
    }
}

@Composable
fun AppSourceBadge(
    packageName: String,
    isClonedApp: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val appLabel = remember(packageName, isClonedApp) {
        AppIconProvider.getAppLabel(context, packageName, isClonedApp)
    }
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(SurfaceElevatedDark)
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (isClonedApp) {
                Icon(
                    imageVector = Icons.Default.Layers,
                    contentDescription = null,
                    tint = CyanAccent,
                    modifier = Modifier.size(10.dp)
                )
                Spacer(modifier = Modifier.width(3.dp))
            }
            Text(
                text = appLabel,
                color = CyanAccent,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

