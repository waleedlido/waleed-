package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.DarkEmerald
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.SurfaceElevatedDark
import com.example.ui.theme.TelegramColor
import com.example.ui.theme.WhatsAppColor

@Composable
fun AppSourceBadge(
    packageName: String,
    isClonedApp: Boolean,
    modifier: Modifier = Modifier
) {
    val meta = resolveAppMetadata(packageName, isClonedApp)

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(meta.backgroundColor)
            .border(1.dp, meta.borderColor, RoundedCornerShape(6.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = meta.icon,
                contentDescription = null,
                tint = meta.contentColor,
                modifier = Modifier.size(11.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = meta.label,
                color = meta.contentColor,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun AppAvatarIcon(
    packageName: String,
    chatTitle: String,
    isClonedApp: Boolean,
    modifier: Modifier = Modifier
) {
    val meta = resolveAppMetadata(packageName, isClonedApp)

    Box(
        modifier = modifier
            .size(46.dp)
            .clip(CircleShape)
            .background(meta.avatarBg)
            .border(1.2.dp, if (isClonedApp) CyanAccent else meta.borderColor, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = chatTitle.take(1).uppercase(),
            color = meta.contentColor,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

data class AppMetadata(
    val label: String,
    val icon: ImageVector,
    val contentColor: Color,
    val backgroundColor: Color,
    val borderColor: Color,
    val avatarBg: Color
)

fun resolveAppMetadata(packageName: String, isClonedApp: Boolean): AppMetadata {
    val pkg = packageName.lowercase()
    return when {
        pkg.contains("whatsapp") && isClonedApp -> AppMetadata(
            label = "واتساب (App Twin)",
            icon = Icons.Default.Forum,
            contentColor = CyanAccent,
            backgroundColor = Color(0xFF00363A),
            borderColor = CyanAccent.copy(alpha = 0.5f),
            avatarBg = Color(0xFF00272B)
        )
        pkg.contains("whatsapp") -> AppMetadata(
            label = if (pkg.contains("w4b")) "واتساب أعمال" else "واتساب",
            icon = Icons.Default.Forum,
            contentColor = WhatsAppColor,
            backgroundColor = DarkEmerald,
            borderColor = NeonGreen.copy(alpha = 0.4f),
            avatarBg = Color(0xFF072111)
        )
        pkg.contains("telegram") -> AppMetadata(
            label = "تيليجرام",
            icon = Icons.Default.Send,
            contentColor = TelegramColor,
            backgroundColor = Color(0xFF0D2D42),
            borderColor = TelegramColor.copy(alpha = 0.4f),
            avatarBg = Color(0xFF0A1F2E)
        )
        pkg.contains("orca") || pkg.contains("messenger") -> AppMetadata(
            label = "ماسنجر",
            icon = Icons.Default.Chat,
            contentColor = Color(0xFF0084FF),
            backgroundColor = Color(0xFF0D2447),
            borderColor = Color(0xFF0084FF).copy(alpha = 0.4f),
            avatarBg = Color(0xFF071730)
        )
        pkg.contains("signal") || pkg.contains("securesms") -> AppMetadata(
            label = "سيجنال",
            icon = Icons.Default.Shield,
            contentColor = Color(0xFF3A76F0),
            backgroundColor = Color(0xFF132347),
            borderColor = Color(0xFF3A76F0).copy(alpha = 0.4f),
            avatarBg = Color(0xFF09142E)
        )
        pkg.contains("mms") || pkg.contains("messaging") -> AppMetadata(
            label = "رسائل SMS",
            icon = Icons.Default.Sms,
            contentColor = Color(0xFFFFB300),
            backgroundColor = Color(0xFF382A05),
            borderColor = Color(0xFFFFB300).copy(alpha = 0.4f),
            avatarBg = Color(0xFF241B03)
        )
        else -> AppMetadata(
            label = if (isClonedApp) "تطبيق (مستنسخ)" else "محادثة",
            icon = Icons.Default.Message,
            contentColor = NeonGreen,
            backgroundColor = SurfaceElevatedDark,
            borderColor = Color(0xFF23442C),
            avatarBg = SurfaceElevatedDark
        )
    }
}
