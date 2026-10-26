package com.example.ui.settings

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.repository.ImportResult
import com.example.data.repository.MessageVaultRepository
import com.example.data.repository.VaultStats
import com.example.ml.ModelDownloadState
import com.example.ml.TranslationManager
import com.example.security.SecurityManager
import com.example.service.NotificationVaultService
import com.example.ui.home.formatTime
import com.example.ui.permissions.HonorOptimizationCard
import com.example.ui.theme.AppThemeManager
import com.example.ui.theme.ColorPalette
import com.example.ui.theme.DangerRed
import com.example.ui.theme.ThemeMode
import com.example.ui.theme.VaultTheme
import com.example.ui.theme.WarningOrange
import com.example.ui.theme.ThemeConfig
import com.example.util.AppNotificationManager
import com.example.util.ChatWallpaperManager
import com.example.util.LockScreenPrivacy
import com.example.util.NotificationConfig
import com.example.util.NotificationPreferences
import com.example.util.PreferenceManager
import com.example.util.PrivacyBlurConfig
import com.example.util.PrivacyBlurManager
import com.example.util.WallpaperConfig
import com.example.util.WallpaperType
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader
import java.text.SimpleDateFormat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    repository: MessageVaultRepository,
    translationManager: TranslationManager,
    securityManager: SecurityManager,
    preferenceManager: PreferenceManager,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val colors = VaultTheme.colors

    // Managers
    val themeManager = remember { AppThemeManager.get(context) }
    val themeConfig by themeManager.themeConfigFlow.collectAsState(initial = ThemeConfig())

    val wallpaperManager = remember { ChatWallpaperManager.get(context) }
    val wallpaperConfig by wallpaperManager.globalConfigFlow.collectAsState(initial = WallpaperConfig())

    val privacyManager = remember { PrivacyBlurManager.get(context) }
    val privacyConfig by privacyManager.configFlow.collectAsState(initial = PrivacyBlurConfig())

    val notifPrefs = remember { NotificationPreferences.get(context) }
    val notifConfig by notifPrefs.configFlow.collectAsState(initial = NotificationConfig())

    var showSetPinDialog by remember { mutableStateOf(false) }

    // Photo picker launcher for chat wallpapers (zero broad storage permission needed)
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch {
                val success = wallpaperManager.setCustomPhotoWallpaper(uri)
                if (success) {
                    Toast.makeText(context, "تم حفظ وتعيين صورة الخلفية بنجاح ✓", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "فشل تعيين الصورة، يرجى المحاولة بصورة أخرى", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    val postNotificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            notifPrefs.setNotificationsEnabled(true)
            Toast.makeText(context, "تم تفعيل إشعارات التطبيق بنجاح ✓", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "لم يتم منح إذن الإشعارات من النظام", Toast.LENGTH_SHORT).show()
        }
    }

    // Intercept back button: dismiss dialog first if open
    BackHandler {
        if (showSetPinDialog) {
            showSetPinDialog = false
        } else {
            onBack()
        }
    }

    var stats by remember { mutableStateOf<VaultStats?>(null) }
    LaunchedEffect(Unit) {
        stats = repository.getStats()
        translationManager.checkDownloadedModels()
    }

    val isServiceConnected by NotificationVaultService.isServiceConnected.collectAsState()
    val lastReceivedEvent by NotificationVaultService.lastReceivedEvent.collectAsState()
    val lastSavedMessage by NotificationVaultService.lastSavedMessage.collectAsState()
    val lastError by NotificationVaultService.lastError.collectAsState()
    val diagnostics by NotificationVaultService.diagnostics.collectAsState()

    val modelDownloadState by translationManager.modelDownloadState.collectAsState()
    val isArabicModelDownloaded by translationManager.isArabicModelDownloaded.collectAsState()
    val isEnglishModelDownloaded by translationManager.isEnglishModelDownloaded.collectAsState()

    var isLockEnabled by remember { mutableStateOf(securityManager.isLockEnabled()) }
    var pinValue by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf<String?>(null) }

    // SAF Document Launchers for JSON Export & Import
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch {
                try {
                    val json = repository.exportDataToJson()
                    context.contentResolver.openOutputStream(uri)?.use { os ->
                        os.write(json.toByteArray())
                    }
                    Toast.makeText(context, "تم تصدير النسخة الاحتياطية بنجاح ✓", Toast.LENGTH_LONG).show()
                } catch (e: Exception) {
                    Toast.makeText(context, "فشل التصدير: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch {
                try {
                    val content = StringBuilder()
                    context.contentResolver.openInputStream(uri)?.use { inputStream ->
                        BufferedReader(InputStreamReader(inputStream)).use { reader ->
                            var line = reader.readLine()
                            while (line != null) {
                                content.append(line)
                                line = reader.readLine()
                            }
                        }
                    }
                    val result = repository.importDataFromJson(content.toString())
                    when (result) {
                        is ImportResult.Success -> {
                            Toast.makeText(
                                context,
                                "تمت استعادة ${result.conversationsRestored} محادثة و ${result.messagesRestored} رسالة بنجاح ✓",
                                Toast.LENGTH_LONG
                            ).show()
                            stats = repository.getStats()
                        }
                        is ImportResult.Error -> {
                            Toast.makeText(context, result.message, Toast.LENGTH_LONG).show()
                        }
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "خطأ أثناء القراءة: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    Scaffold(
        containerColor = colors.background,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = colors.topBarBackground,
                    titleContentColor = colors.textPrimary
                ),
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("settings_back_button")) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع", tint = colors.textPrimary)
                    }
                },
                title = {
                    Text("الإعدادات والتخصيص", fontSize = 17.sp, fontWeight = FontWeight.Bold)
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {

            // ==========================================
            // 1. المظهر والثيمات (Theme & Color Palettes)
            // ==========================================
            SettingsSectionHeader(title = "1. المظهر والثيمات (Theme & Colors)", icon = Icons.Default.Palette)

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, colors.cardBorder, RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = colors.cardBackground),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "نمط العرض (وضع الألوان)",
                        color = colors.textPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    // Theme Mode Selector
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ThemeMode.entries.forEach { mode ->
                            val isSelected = themeConfig.themeMode == mode
                            val label = when (mode) {
                                ThemeMode.DARK -> "الداكن 🌙"
                                ThemeMode.LIGHT -> "الفاتح ☀️"
                                ThemeMode.SYSTEM -> "تلقائي 📱"
                            }
                            FilterChip(
                                selected = isSelected,
                                onClick = { themeManager.setThemeMode(mode) },
                                label = { Text(label, fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                                modifier = Modifier.weight(1f),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = colors.primaryContainer,
                                    selectedLabelColor = colors.accent,
                                    containerColor = colors.surfaceElevated,
                                    labelColor = colors.textSecondary
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = isSelected,
                                    borderColor = colors.cardBorder,
                                    selectedBorderColor = colors.accent
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = colors.cardBorder)
                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "لوحة الألوان الرئيسية (7 خيارات مميزة)",
                        color = colors.textPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "تغيير الألوان فورياً في كافة أرجاء التطبيق والبطاقات:",
                        color = colors.textMuted,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(bottom = 10.dp)
                    )

                    // 7 Palettes
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        ColorPalette.entries.forEach { palette ->
                            val isSelected = themeConfig.colorPalette == palette
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isSelected) colors.primaryContainer.copy(alpha = 0.5f) else Color.Transparent)
                                    .border(
                                        width = if (isSelected) 1.5.dp else 1.dp,
                                        color = if (isSelected) colors.accent else colors.cardBorder,
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    .clickable { themeManager.setColorPalette(palette) }
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    // Visual color circle
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(CircleShape)
                                            .background(palette.accentColor)
                                            .border(1.dp, Color.White.copy(alpha = 0.3f), CircleShape)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = palette.displayNameAr,
                                            color = if (isSelected) colors.accent else colors.textPrimary,
                                            fontSize = 13.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                        )
                                        Text(
                                            text = palette.displayNameEn,
                                            color = colors.textMuted,
                                            fontSize = 10.sp
                                        )
                                    }
                                }

                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "محدد",
                                        tint = colors.accent,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Reset to default button
                    OutlinedButton(
                        onClick = {
                            themeManager.resetToDefaults()
                            Toast.makeText(context, "تمت استعادة المظهر الافتراضي (الداكن والزمردي)", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(imageVector = Icons.Default.RestartAlt, contentDescription = null, tint = colors.accent, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("استعادة المظهر الافتراضي (الزمردي الداكن)", color = colors.accent, fontSize = 12.sp)
                    }
                }
            }

            // ==========================================
            // 2. خلفيات المحادثات (Chat Wallpapers)
            // ==========================================
            SettingsSectionHeader(title = "2. خلفيات المحادثات (Chat Wallpapers)", icon = Icons.Default.Wallpaper)

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, colors.cardBorder, RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = colors.cardBackground),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "خلفية شاشة المحادثة",
                        color = colors.textPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "اختر صورة مخصصة من معرض الصور أو من التدرجات الجاهزة:",
                        color = colors.textMuted,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    // Photo Picker Button
                    Button(
                        onClick = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = colors.primaryContainer, contentColor = colors.accent),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(imageVector = Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (wallpaperConfig.wallpaperType == WallpaperType.CUSTOM_PHOTO) "تغيير الصورة المخصصة من المعرض" else "اختيار صورة من معرض الهاتف",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Preset Gradients Row
                    Text(
                        text = "أو اختر تدرجاً جاهزاً:",
                        color = colors.textSecondary,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Default (No photo / no gradient)
                        item {
                            val isDefault = wallpaperConfig.wallpaperType == WallpaperType.DEFAULT
                            Box(
                                modifier = Modifier
                                    .size(width = 80.dp, height = 55.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(colors.background)
                                    .border(
                                        width = if (isDefault) 2.dp else 1.dp,
                                        color = if (isDefault) colors.accent else colors.cardBorder,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .clickable { wallpaperManager.setDefaultWallpaper() }
                                    .padding(4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("الافتراضي", color = colors.textSecondary, fontSize = 11.sp)
                            }
                        }

                        // Preset list
                        items(wallpaperManager.presetWallpapers) { preset ->
                            val isSelected = wallpaperConfig.wallpaperType == WallpaperType.PRESET && wallpaperConfig.presetId == preset.id
                            Box(
                                modifier = Modifier
                                    .size(width = 80.dp, height = 55.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Brush.verticalGradient(preset.gradientColors))
                                    .border(
                                        width = if (isSelected) 2.dp else 1.dp,
                                        color = if (isSelected) colors.accent else Color.Transparent,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .clickable { wallpaperManager.setPresetWallpaper(preset.id) }
                                    .padding(4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = preset.nameAr,
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = colors.cardBorder)
                    Spacer(modifier = Modifier.height(12.dp))

                    // Dimming slider (0% to 80%, default 40%)
                    val currentDimPercent = (wallpaperConfig.dimAlpha * 100).toInt()
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "مستوى التعتيم (Dimming): $currentDimPercent%",
                            color = colors.textPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "افتراضي: 40%",
                            color = colors.textMuted,
                            fontSize = 10.sp
                        )
                    }

                    Slider(
                        value = wallpaperConfig.dimAlpha,
                        onValueChange = { wallpaperManager.setDimAlpha(it) },
                        valueRange = 0.0f..0.80f,
                        steps = 7,
                        colors = SliderDefaults.colors(
                            thumbColor = colors.accent,
                            activeTrackColor = colors.accent,
                            inactiveTrackColor = colors.surfaceElevated
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // Blur toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("تمويه الخلفية (Blur)", color = colors.textPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            Text("تطبيق ضبابية خفيفة على الخلفية لتسهيل قراءة نصوص الرسائل", color = colors.textMuted, fontSize = 10.sp)
                        }
                        Switch(
                            checked = wallpaperConfig.isBlurred,
                            onCheckedChange = { wallpaperManager.setBlurred(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.Black,
                                checkedTrackColor = colors.accent,
                                uncheckedThumbColor = colors.textMuted,
                                uncheckedTrackColor = colors.surfaceElevated
                            )
                        )
                    }

                    if (wallpaperConfig.wallpaperType != WallpaperType.DEFAULT) {
                        Spacer(modifier = Modifier.height(10.dp))
                        TextButton(
                            onClick = { wallpaperManager.setDefaultWallpaper() },
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Icon(imageVector = Icons.Default.Delete, contentDescription = null, tint = DangerRed, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("إلغاء وتفريغ الخلفية", color = DangerRed, fontSize = 11.sp)
                        }
                    }
                }
            }

            // ==========================================
            // 3. الخصوصية والتمويه (Privacy & Blurring)
            // ==========================================
            SettingsSectionHeader(title = "3. الخصوصية والتمويه (Privacy & Security)", icon = Icons.Default.Security)

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, colors.cardBorder, RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = colors.cardBackground),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Master Blur Switch
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (privacyConfig.isMasterBlurActive) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = null,
                                tint = if (privacyConfig.isMasterBlurActive) colors.accent else colors.textMuted,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    "نظام تمويه الرسائل والخصوصية",
                                    color = colors.textPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    if (privacyConfig.isMasterBlurActive) "مفعّل (يخفي النصوص داخل التطبيق)" else "معطّل (الافتراضي)",
                                    color = if (privacyConfig.isMasterBlurActive) colors.accent else colors.textMuted,
                                    fontSize = 11.sp
                                )
                            }
                        }
                        Switch(
                            checked = privacyConfig.isMasterBlurActive,
                            onCheckedChange = { privacyManager.setMasterBlur(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.Black,
                                checkedTrackColor = colors.accent,
                                uncheckedThumbColor = colors.textMuted,
                                uncheckedTrackColor = colors.surfaceElevated
                            )
                        )
                    }

                    // Assurance Box: Pure visual masking, never modifies Room DB
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(colors.primaryContainer.copy(alpha = 0.5f))
                            .border(1.dp, colors.accent.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                            .padding(10.dp)
                    ) {
                        Text(
                            text = "ملاحظة أمان: التمويه هو طبقة بصرية على الواجهة لمنع المتطفلين بجوارك من قراءة الرسائل. نصوص الرسائل تبقى محفوظة بدقة ومكتملة داخل قاعدة البيانات المحلية دون أي حذف أو تشويه.",
                            color = colors.textPrimary,
                            fontSize = 11.sp,
                            lineHeight = 15.sp
                        )
                    }

                    if (privacyConfig.isMasterBlurActive) {
                        Spacer(modifier = Modifier.height(14.dp))
                        HorizontalDivider(color = colors.cardBorder)
                        Spacer(modifier = Modifier.height(10.dp))

                        // Sub-switch: Home preview
                        NotificationSubToggleRow(
                            title = "تمويه معاينة الرسائل بالرئيسية",
                            subtitle = "حجب آخر رسالة في بطاقات المحادثات",
                            checked = privacyConfig.blurHomePreviews,
                            onCheckedChange = { privacyManager.setBlurHomePreviews(it) }
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Sub-switch: Chat message text
                        NotificationSubToggleRow(
                            title = "تمويه فقاعات الرسائل بالداخل",
                            subtitle = "إخفاء نص الرسالة مع إمكانية النقر على العين لكشفها مؤقتاً",
                            checked = privacyConfig.blurChatMessages,
                            onCheckedChange = { privacyManager.setBlurChatMessages(it) }
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Sub-switch: Sender name
                        NotificationSubToggleRow(
                            title = "تمويه أسماء المرسلين",
                            subtitle = "إخفاء هوية واسم المرسل في الشاشة",
                            checked = privacyConfig.blurSenderNames,
                            onCheckedChange = { privacyManager.setBlurSenderNames(it) }
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = colors.cardBorder)
                    Spacer(modifier = Modifier.height(12.dp))

                    // FLAG_SECURE toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "حماية لقطة الشاشة والتطبيقات الحديثة",
                                color = colors.textPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "تفعيل FLAG_SECURE لمنع أخذ لقطات شاشة وحجب المحتوى في شاشة المهام المتعددة (Recent Apps)",
                                color = colors.textMuted,
                                fontSize = 11.sp
                            )
                        }
                        Switch(
                            checked = privacyConfig.preventScreenshots,
                            onCheckedChange = { privacyManager.setPreventScreenshots(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.Black,
                                checkedTrackColor = colors.accent,
                                uncheckedThumbColor = colors.textMuted,
                                uncheckedTrackColor = colors.surfaceElevated
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = colors.cardBorder)
                    Spacer(modifier = Modifier.height(12.dp))

                    // Biometric & PIN lock
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Fingerprint, contentDescription = null, tint = colors.accent)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("القفل بالبصمة / الوجه", color = colors.textPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                Text("طلب المصادقة الحيوية عند فتح التطبيق", color = colors.textMuted, fontSize = 11.sp)
                            }
                        }
                        Switch(
                            checked = isLockEnabled,
                            onCheckedChange = { checked ->
                                if (checked && !securityManager.hasPinSet()) {
                                    showSetPinDialog = true
                                } else {
                                    isLockEnabled = checked
                                    securityManager.setLockEnabled(checked)
                                }
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.Black,
                                checkedTrackColor = colors.accent,
                                uncheckedThumbColor = colors.textMuted,
                                uncheckedTrackColor = colors.surfaceElevated
                            )
                        )
                    }

                    if (isLockEnabled) {
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = { showSetPinDialog = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Lock, contentDescription = null, tint = colors.accent, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (securityManager.hasPinSet()) "تغيير رمز PIN الاحتياطي" else "تعيين رمز PIN الاحتياطي",
                                color = colors.accent,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }

            // ==========================================
            // 4. إشعارات التطبيق وشاشة القفل (v1.0.4)
            // ==========================================
            SettingsSectionHeader(title = "4. إشعارات التطبيق وشاشة القفل (Notification System)", icon = Icons.Default.Notifications)

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, colors.cardBorder, RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = colors.cardBackground),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Master Switch
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (notifConfig.areNotificationsEnabled) Icons.Default.NotificationsActive else Icons.Default.NotificationsOff,
                                contentDescription = null,
                                tint = if (notifConfig.areNotificationsEnabled) colors.accent else colors.textMuted,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    "إشعارات التطبيق",
                                    color = colors.textPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    if (notifConfig.areNotificationsEnabled) "مفعلة (شريط الحالة وشاشة القفل)" else "معطلة (افتراضياً)",
                                    color = if (notifConfig.areNotificationsEnabled) colors.accent else colors.textMuted,
                                    fontSize = 11.sp
                                )
                            }
                        }
                        Switch(
                            checked = notifConfig.areNotificationsEnabled,
                            onCheckedChange = { checked ->
                                if (checked) {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        val hasPerm = ContextCompat.checkSelfPermission(
                                            context,
                                            Manifest.permission.POST_NOTIFICATIONS
                                        ) == PackageManager.PERMISSION_GRANTED
                                        if (hasPerm) {
                                            notifPrefs.setNotificationsEnabled(true)
                                        } else {
                                            postNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                        }
                                    } else {
                                        notifPrefs.setNotificationsEnabled(true)
                                    }
                                } else {
                                    notifPrefs.setNotificationsEnabled(false)
                                }
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.Black,
                                checkedTrackColor = colors.accent,
                                uncheckedThumbColor = colors.textMuted,
                                uncheckedTrackColor = colors.surfaceElevated
                            )
                        )
                    }

                    // Crucial Safety Assurance Note: Notification toggle does NOT affect saving
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(colors.primaryContainer.copy(alpha = 0.5f))
                            .border(1.dp, colors.accent.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                            .padding(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.Top) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = null,
                                tint = colors.accent,
                                modifier = Modifier.size(16.dp).padding(top = 2.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "حماية وحفظ الرسائل: تعمل خدمة رصد وحفظ الرسائل في قاعدة بيانات Room دائماً وبشكل مستقل تماماً، سواء كانت إشعارات التطبيق مفعلة أو معطلة.",
                                color = colors.textPrimary,
                                fontSize = 11.sp,
                                lineHeight = 16.sp
                            )
                        }
                    }

                    if (notifConfig.areNotificationsEnabled) {
                        HorizontalDivider(color = colors.cardBorder, modifier = Modifier.padding(vertical = 12.dp))

                        // Sub-switch 1: New messages
                        NotificationSubToggleRow(
                            title = "إشعارات الرسائل الجديدة",
                            subtitle = "تنبيه فوري عند وصول وحفظ رسالة من التطبيقات المدعومة",
                            checked = notifConfig.notifyOnNewMessages,
                            onCheckedChange = { notifPrefs.setNotifyOnNewMessages(it) }
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Sub-switch 2: Deleted messages
                        NotificationSubToggleRow(
                            title = "إشعارات الرسائل المحذوفة",
                            subtitle = "تنبيه عند اكتشاف حذف الطرف الآخر للرسالة",
                            checked = notifConfig.notifyOnDeletedMessages,
                            onCheckedChange = { notifPrefs.setNotifyOnDeletedMessages(it) }
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Sub-switch 3: Sound
                        NotificationSubToggleRow(
                            title = "التنبيه الصوتي",
                            subtitle = "تشغيل صوت الإشعار المخصص عند التنبيه",
                            checked = notifConfig.isSoundEnabled,
                            onCheckedChange = { notifPrefs.setSoundEnabled(it) }
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Sub-switch 4: Vibration
                        NotificationSubToggleRow(
                            title = "الاهتزاز",
                            subtitle = "اهتزاز الجهاز عند وصول الإشعار",
                            checked = notifConfig.isVibrationEnabled,
                            onCheckedChange = { notifPrefs.setVibrationEnabled(it) }
                        )

                        HorizontalDivider(color = colors.cardBorder, modifier = Modifier.padding(vertical = 12.dp))

                        // Lock Screen Privacy Section
                        Text(
                            text = "مستوى خصوصية شاشة القفل",
                            color = colors.textPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "حدد مقدار التفاصيل المعروضة أثناء قفل الهاتف (مع احترام إعدادات خصوصية النظام):",
                            color = colors.textSecondary,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        LockScreenPrivacy.entries.forEach { option ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { notifPrefs.setLockScreenPrivacy(option) }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = notifConfig.lockScreenPrivacy == option,
                                    onClick = { notifPrefs.setLockScreenPrivacy(option) },
                                    colors = RadioButtonDefaults.colors(
                                        selectedColor = colors.accent,
                                        unselectedColor = colors.textMuted
                                    )
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Column {
                                    Text(
                                        text = option.title,
                                        color = if (notifConfig.lockScreenPrivacy == option) colors.accent else colors.textPrimary,
                                        fontSize = 12.sp,
                                        fontWeight = if (notifConfig.lockScreenPrivacy == option) FontWeight.Bold else FontWeight.Normal
                                    )
                                    Text(
                                        text = option.description,
                                        color = colors.textMuted,
                                        fontSize = 10.sp
                                    )
                                }
                            }
                        }

                        HorizontalDivider(color = colors.cardBorder, modifier = Modifier.padding(vertical = 12.dp))

                        // App Filters
                        Text(
                            text = "التطبيقات المشمولة بالإشعارات",
                            color = colors.textPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "اختر التطبيقات التي تريد تلقي إشعارات منها عند وصول رسائلها:",
                            color = colors.textSecondary,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        val appList = listOf(
                            Triple("واتساب الأصلي", "whatsapp", notifConfig.notifyWhatsApp),
                            Triple("واتساب المستنسخ (MagicOS App Twin)", "whatsapp_twin", notifConfig.notifyWhatsAppTwin),
                            Triple("ماسنجر (Messenger)", "messenger", notifConfig.notifyMessenger),
                            Triple("تيليجرام (Telegram)", "telegram", notifConfig.notifyTelegram),
                            Triple("سيجنال (Signal)", "signal", notifConfig.notifySignal),
                            Triple("رسائل SMS", "sms", notifConfig.notifySms)
                        )

                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            appList.forEach { (name, key, isSelected) ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { notifPrefs.setAppFilter(key, !isSelected) }
                                        .padding(vertical = 4.dp, horizontal = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(name, color = if (isSelected) colors.textPrimary else colors.textMuted, fontSize = 12.sp)
                                    Switch(
                                        checked = isSelected,
                                        onCheckedChange = { notifPrefs.setAppFilter(key, it) },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = Color.Black,
                                            checkedTrackColor = colors.accent,
                                            uncheckedThumbColor = colors.textMuted,
                                            uncheckedTrackColor = colors.surfaceElevated
                                        ),
                                        modifier = Modifier.size(width = 44.dp, height = 24.dp)
                                    )
                                }
                            }
                        }

                        HorizontalDivider(color = colors.cardBorder, modifier = Modifier.padding(vertical = 12.dp))
                    }

                    // Test and System Settings Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    val hasPerm = ContextCompat.checkSelfPermission(
                                        context,
                                        Manifest.permission.POST_NOTIFICATIONS
                                    ) == PackageManager.PERMISSION_GRANTED
                                    if (!hasPerm) {
                                        postNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                        return@Button
                                    }
                                }
                                AppNotificationManager.sendTestNotification(context)
                                Toast.makeText(context, "تم إرسال إشعار تجريبي!", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = colors.primaryContainer, contentColor = colors.accent),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Notifications, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("تجربة إشعار", fontSize = 12.sp)
                        }

                        Button(
                            onClick = {
                                AppNotificationManager.openSystemNotificationSettings(context)
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = colors.surfaceElevated, contentColor = colors.textPrimary),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("إعدادات النظام", fontSize = 12.sp)
                        }
                    }

                    // Check if system disabled app notifications
                    val systemNotificationsAllowed = AppNotificationManager.areSystemNotificationsEnabled(context)
                    if (!systemNotificationsAllowed) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(DangerRed.copy(alpha = 0.15f))
                                .padding(8.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Warning, contentDescription = null, tint = DangerRed, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("تنبيه: الإشعارات معطلة لهذا التطبيق في إعدادات Android العامة", color = DangerRed, fontSize = 10.sp)
                        }
                    }
                }
            }

            // ==========================================
            // 5. حالة خدمة استقبال الرسائل والتشخيص
            // ==========================================
            SettingsSectionHeader(title = "5. حالة الخدمة والتشخيص الدقيق (Service Diagnostics)", icon = Icons.Default.Speed)

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, colors.cardBorder, RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = colors.cardBackground),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // 1. Notification Access Permission
                    val accessGranted = preferenceManager.isNotificationAccessGranted()
                    DiagnosticRow(
                        label = "صلاحية الوصول للإشعارات (Permission)",
                        statusText = if (accessGranted) "ممنوحة ومفعلة بالنظام ✓" else "معطلة! يلزم تفعيلها",
                        isSuccess = accessGranted,
                        onActionClick = {
                            context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                        },
                        actionText = if (accessGranted) "تحقق" else "تفعيل"
                    )

                    HorizontalDivider(color = colors.cardBorder, modifier = Modifier.padding(vertical = 10.dp))

                    // 2. Real Connection Status
                    val isRealActive = accessGranted && isServiceConnected
                    val statusLabel = when {
                        !accessGranted -> "معطلة (الصلاحية غير مفعلة)"
                        isServiceConnected -> "نشطة وتستقبل في الخلفية الآن ✓"
                        else -> "الصلاحية ممنوحة - بانتظار ربط النظام (انقر لإعادة الربط)"
                    }

                    DiagnosticRow(
                        label = "حالة اتصال الخدمة الفعلية (Listener)",
                        statusText = statusLabel,
                        isSuccess = isRealActive,
                        onActionClick = if (accessGranted && !isServiceConnected) {
                            { NotificationVaultService.tryRebind(context) }
                        } else null,
                        actionText = if (accessGranted && !isServiceConnected) "إعادة الربط" else null
                    )

                    HorizontalDivider(color = colors.cardBorder, modifier = Modifier.padding(vertical = 10.dp))

                    // Diagnostic telemetry counters (Battery & Processing efficiency)
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "إحصائيات المعالجة واستهلاك الطاقة (HONOR / Android):",
                            color = colors.textPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            DiagnosticMetricBadge(
                                label = "المستلمة",
                                count = diagnostics.receivedCount.toString(),
                                color = colors.primary,
                                modifier = Modifier.weight(1f)
                            )
                            DiagnosticMetricBadge(
                                label = "المعالجة",
                                count = diagnostics.processedCount.toString(),
                                color = com.example.ui.theme.NeonGreen,
                                modifier = Modifier.weight(1f)
                            )
                            DiagnosticMetricBadge(
                                label = "المتجاهلة",
                                count = diagnostics.ignoredCount.toString(),
                                color = colors.textMuted,
                                modifier = Modifier.weight(1f)
                            )
                            DiagnosticMetricBadge(
                                label = "الأخطاء",
                                count = diagnostics.errorsCount.toString(),
                                color = if (diagnostics.errorsCount > 0) DangerRed else colors.textMuted,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    HorizontalDivider(color = colors.cardBorder, modifier = Modifier.padding(vertical = 10.dp))

                    // 3. Last received notification
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text("آخر إشعار مستلم عبر النظام:", color = colors.textPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(4.dp))
                        if (lastReceivedEvent != null) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(colors.surfaceElevated)
                                    .padding(8.dp)
                            ) {
                                Column {
                                    Text(
                                        text = "${lastReceivedEvent?.chatTitle} (${lastReceivedEvent?.appDisplayName})",
                                        color = colors.accent,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = lastReceivedEvent?.messageSnippet ?: "",
                                        color = colors.textSecondary,
                                        fontSize = 12.sp,
                                        maxLines = 1
                                    )
                                    Text(
                                        text = formatTime(lastReceivedEvent?.timestamp ?: 0L),
                                        color = colors.textMuted,
                                        fontSize = 10.sp
                                    )
                                }
                            }
                        } else {
                            Text("لم يصل أي إشعار جديد في الجلسة الحالية", color = colors.textMuted, fontSize = 11.sp)
                        }
                    }

                    HorizontalDivider(color = colors.cardBorder, modifier = Modifier.padding(vertical = 10.dp))

                    // 4. Last saved message in Database
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text("آخر رسالة محفوظة بقاعدة البيانات:", color = colors.textPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(4.dp))
                        val lastSaved = lastSavedMessage ?: stats?.lastMessage?.let { "${it.senderName}: ${it.messageText.take(35)}" }
                        if (!lastSaved.isNullOrBlank()) {
                            Text(lastSaved, color = colors.accent, fontSize = 12.sp)
                        } else {
                            Text("لا توجد رسائل مسجلة بعد", color = colors.textMuted, fontSize = 11.sp)
                        }
                    }

                    // 5. Saving Error Logs
                    if (!lastError.isNullOrBlank()) {
                        HorizontalDivider(color = colors.cardBorder, modifier = Modifier.padding(vertical = 10.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Warning, contentDescription = null, tint = DangerRed, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(lastError ?: "", color = DangerRed, fontSize = 11.sp)
                        }
                    }

                    HorizontalDivider(color = colors.cardBorder, modifier = Modifier.padding(vertical = 10.dp))

                    // Counters
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("المحادثات", color = colors.textMuted, fontSize = 11.sp)
                            Text("${stats?.conversationsCount ?: 0}", color = colors.textPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                        Column {
                            Text("الرسائل الكلية", color = colors.textMuted, fontSize = 11.sp)
                            Text("${stats?.messagesCount ?: 0}", color = colors.textPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                        Column {
                            Text("الرسائل المحذوفة", color = colors.textMuted, fontSize = 11.sp)
                            Text("${stats?.deletedMessagesCount ?: 0}", color = DangerRed, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Real Test Notification Injection Button
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        coroutineScope.launch {
                            val time = System.currentTimeMillis()
                            repository.saveIncomingNotification(
                                packageName = "com.whatsapp",
                                appDisplayName = "واتساب",
                                chatTitle = "تجربة النظام المباشر",
                                senderName = "تجربة النظام المباشر",
                                messageText = "هذه رسالة اختبار تم حفظها بنجاح للتأكد من استقرار الخزينة في نظام HONOR MagicOS",
                                timestamp = time,
                                isClonedApp = false,
                                userHandleId = 0
                            )
                            stats = repository.getStats()
                            Toast.makeText(context, "تم استقبال وتوثيق الرسالة بنجاح!", Toast.LENGTH_SHORT).show()
                        }
                    },
                colors = CardDefaults.cardColors(containerColor = colors.primaryContainer),
                shape = RoundedCornerShape(14.dp)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = Icons.Default.Speed, contentDescription = null, tint = colors.accent)
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text("اختبار استقبال وحفظ رسالة حقيقية", color = colors.textPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Text("انقر لمحاكاة وصول إشعار فوري والتأكد من إضافته لقاعدة البيانات", color = colors.textSecondary, fontSize = 11.sp)
                    }
                }
            }

            // ==========================================
            // 6. النسخ الاحتياطي والاستعادة والترجمة
            // ==========================================
            SettingsSectionHeader(title = "6. النسخ الاحتياطي والترجمة (Backup & ML Kit)", icon = Icons.Default.Backup)

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, colors.cardBorder, RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = colors.cardBackground),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "تخزين البيانات والنسخ الاحتياطي (SAF)",
                        color = colors.textPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "البيانات محفوظة في Room Database محلية داخل التخزين الخاص بالتطبيق. يمكنك تصدير أو استعادة النسخ بصيغة JSON بأمان.",
                        color = colors.textSecondary,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                val timestamp = SimpleDateFormat("yyyyMMdd_HHmm", java.util.Locale.US).format(java.util.Date())
                                exportLauncher.launch("MessageVault_Backup_$timestamp.json")
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = colors.primaryContainer, contentColor = colors.accent),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Backup, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("تصدير نسخة", fontSize = 12.sp)
                        }

                        Button(
                            onClick = {
                                importLauncher.launch(arrayOf("application/json", "text/*"))
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = colors.surfaceElevated, contentColor = colors.textPrimary),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("استعادة نسخة", fontSize = 12.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = colors.cardBorder)
                    Spacer(modifier = Modifier.height(12.dp))

                    // ML Kit Offline Translation
                    Text(
                        text = "حزم الترجمة الفورية دون إنترنت (Google ML Kit)",
                        color = colors.textPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "العربية ↔ الإنجليزية بالكامل على الجهاز لحماية سرية محادثاتك.",
                        color = colors.textMuted,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(top = 2.dp, bottom = 8.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isArabicModelDownloaded && isEnglishModelDownloaded) "الحزم منزلة وجاهزة دون إنترنت ✓" else "الحزم غير منزلة",
                            color = if (isArabicModelDownloaded && isEnglishModelDownloaded) colors.accent else WarningOrange,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )

                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    translationManager.downloadTranslationModels()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = colors.primaryContainer,
                                contentColor = colors.accent
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("تنزيل الحزم", fontSize = 12.sp)
                        }
                    }

                    when (val state = modelDownloadState) {
                        is ModelDownloadState.Downloading -> {
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(color = colors.accent, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(state.progressText, color = colors.accent, fontSize = 11.sp)
                            }
                        }
                        is ModelDownloadState.Success -> {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(state.message, color = colors.accent, fontSize = 11.sp)
                        }
                        is ModelDownloadState.Error -> {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(state.message, color = DangerRed, fontSize = 11.sp)
                        }
                        else -> {}
                    }
                }
            }

            // ==========================================
            // 7. حول التطبيق والمطور وتوافق HONOR MagicOS
            // ==========================================
            SettingsSectionHeader(title = "7. حول التطبيق والمطور وتوافق النظام", icon = Icons.Default.Info)

            HonorOptimizationCard(context = context)

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, colors.cardBorder, RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = colors.cardBackground),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(colors.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(imageVector = Icons.Default.Info, contentDescription = null, tint = colors.accent)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("الرسائل المحذوفة • MessageVault Pro", color = colors.textPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                            Text("الإصدار: 1.0.5 (Build 6) • ترقية الواجهة والتخصيص", color = colors.accent, fontSize = 11.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(color = colors.cardBorder)
                    Spacer(modifier = Modifier.height(10.dp))

                    Text("المطور: م. وليد الخطيب — Waleed Al-khatib", color = colors.textPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Text("النظام المستهدف: HONOR 600 Pro (MagicOS / Android 16)", color = colors.textSecondary, fontSize = 12.sp)
                    Text("الميزات: ثيمات 7 ألوان، خلفيات صور وتعتيم، تمويه الخصوصية، إشعارات شاشة القفل، دعم App Twin", color = colors.textMuted, fontSize = 11.sp, modifier = Modifier.padding(top = 2.dp))
                }
            }

            Spacer(modifier = Modifier.height(80.dp))
        }
    }

    // Set PIN Dialog
    if (showSetPinDialog) {
        AlertDialog(
            onDismissRequest = {
                showSetPinDialog = false
                pinError = null
                pinValue = ""
            },
            containerColor = colors.cardBackground,
            title = {
                Text("تعيين رمز PIN الاحتياطي", color = colors.textPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            },
            text = {
                Column {
                    Text("أدخل رمز PIN مكوّن من 4 أرقام لفتح التطبيق في حال عدم توفر البصمة:", color = colors.textSecondary, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = pinValue,
                        onValueChange = { if (it.length <= 4 && it.all { char -> char.isDigit() }) pinValue = it },
                        label = { Text("رمز PIN (4 أرقام)") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = colors.accent,
                            unfocusedBorderColor = colors.cardBorder,
                            focusedTextColor = colors.textPrimary,
                            unfocusedTextColor = colors.textPrimary
                        )
                    )
                    pinError?.let {
                        Text(it, color = DangerRed, fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp))
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (pinValue.length == 4) {
                            securityManager.setPin(pinValue)
                            securityManager.setLockEnabled(true)
                            isLockEnabled = true
                            showSetPinDialog = false
                            pinValue = ""
                            pinError = null
                            Toast.makeText(context, "تم تفعيل القفل وحفظ رمز PIN", Toast.LENGTH_SHORT).show()
                        } else {
                            pinError = "يجب أن يتكون الرمز من 4 أرقام"
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = colors.accent, contentColor = Color.Black)
                ) {
                    Text("حفظ وتفعيل")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSetPinDialog = false }) {
                    Text("إلغاء", color = colors.textMuted)
                }
            }
        )
    }
}

@Composable
fun SettingsSectionHeader(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector? = null) {
    val colors = VaultTheme.colors
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(start = 4.dp, top = 6.dp)
    ) {
        if (icon != null) {
            Icon(imageVector = icon, contentDescription = null, tint = colors.accent, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
        }
        Text(
            text = title,
            color = colors.accent,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun DiagnosticRow(
    label: String,
    statusText: String,
    isSuccess: Boolean,
    onActionClick: (() -> Unit)? = null,
    actionText: String? = null
) {
    val colors = VaultTheme.colors
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, color = colors.textPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Text(
                statusText,
                color = if (isSuccess) colors.accent else DangerRed,
                fontSize = 11.sp
            )
        }
        if (onActionClick != null && actionText != null) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(colors.surfaceElevated)
                    .clickable(onClick = onActionClick)
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(actionText, color = colors.accent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun DiagnosticMetricBadge(
    label: String,
    count: String,
    color: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    val colors = VaultTheme.colors
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(colors.surfaceElevated)
            .border(1.dp, colors.cardBorder, RoundedCornerShape(8.dp))
            .padding(vertical = 8.dp, horizontal = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = count,
                color = color,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label,
                color = colors.textSecondary,
                fontSize = 10.sp
            )
        }
    }
}


@Composable
fun NotificationSubToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val colors = VaultTheme.colors
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = colors.textPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Text(subtitle, color = colors.textMuted, fontSize = 11.sp)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.Black,
                checkedTrackColor = colors.accent,
                uncheckedThumbColor = colors.textMuted,
                uncheckedTrackColor = colors.surfaceElevated
            )
        )
    }
}
