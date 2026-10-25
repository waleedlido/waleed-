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
import com.example.ui.theme.DeepObsidian
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.SurfaceElevatedDark
import com.example.util.AppIconProvider

/**
 * Standardized Application Source Icon (24-28dp) using PackageManager
 * with high-fidelity vector brand fallbacks and HONOR App Twin clone badge.
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
        modifier = modifier.size(if (isClonedApp && showCloneBadge) size + 6.dp else size),
        contentAlignment = Alignment.Center
    ) {
        // App Icon (PackageManager or Fallback Vector)
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
                    modifier = Modifier
                        .size(size)
                        .clip(CircleShape)
                )
            } else {
                Image(
                    painter = painterResource(id = fallbackRes),
                    contentDescription = appLabel,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .size(size)
                        .clip(CircleShape)
                )
            }
        }

        // Small "مستنسخ" badge for HONOR MagicOS App Twin
        if (isClonedApp && showCloneBadge) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = 4.dp, y = 4.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFF00363A))
                    .border(0.8.dp, CyanAccent, RoundedCornerShape(4.dp))
                    .padding(horizontal = 3.dp, vertical = 0.5.dp)
            ) {
                Text(
                    text = "مستنسخ",
                    color = CyanAccent,
                    fontSize = 7.5.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/**
 * Source badge with real icon, app name label, and App Twin clone indication.
 */
@Composable
fun AppSourceBadge(
    packageName: String,
    isClonedApp: Boolean,
    modifier: Modifier = Modifier,
    iconSize: Dp = 20.dp,
    showLabel: Boolean = true
) {
    val context = LocalContext.current
    val label = remember(packageName, isClonedApp) {
        AppIconProvider.getAppLabel(context, packageName, isClonedApp)
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (isClonedApp) Color(0xFF00272B) else DarkEmerald.copy(alpha = 0.5f))
            .border(
                width = 1.dp,
                color = if (isClonedApp) CyanAccent.copy(alpha = 0.6f) else NeonGreen.copy(alpha = 0.3f),
                shape = RoundedCornerShape(8.dp)
            )
            .padding(horizontal = 6.dp, vertical = 3.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            AppSourceIcon(
                packageName = packageName,
                isClonedApp = isClonedApp,
                size = iconSize,
                showCloneBadge = false
            )
            if (showLabel) {
                Spacer(modifier = Modifier.width(5.dp))
                Text(
                    text = label,
                    color = if (isClonedApp) CyanAccent else NeonGreen,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/**
 * Avatar with contact initial and badged Real Application Source icon at corner
 */
@Composable
fun AppAvatarIcon(
    packageName: String,
    chatTitle: String,
    isClonedApp: Boolean,
    modifier: Modifier = Modifier
) {
    val initialLetter = chatTitle.take(1).uppercase().ifBlank { "؟" }

    Box(
        modifier = modifier.size(50.dp),
        contentAlignment = Alignment.Center
    ) {
        // Main Avatar Circle
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(CircleShape)
                .background(Color(0xFF092015))
                .border(
                    width = 1.2.dp,
                    color = if (isClonedApp) CyanAccent else NeonGreen.copy(alpha = 0.5f),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = initialLetter,
                color = if (isClonedApp) CyanAccent else NeonGreen,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // Real app icon badged at bottom-end corner
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = 2.dp, y = 2.dp)
        ) {
            AppSourceIcon(
                packageName = packageName,
                isClonedApp = isClonedApp,
                size = 22.dp,
                showCloneBadge = true
            )
        }
    }
}
