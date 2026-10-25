package com.example.ui.settings

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.VolumeUp
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.repository.ImportResult
import com.example.data.repository.MessageVaultRepository
import com.example.data.repository.VaultStats
import com.example.ml.ModelDownloadState
import com.example.ml.TranslationManager
import com.example.security.SecurityManager
import com.example.service.NotificationVaultService
import com.example.ui.home.formatTime
import com.example.ui.permissions.HonorOptimizationCard
import com.example.ui.theme.BorderDark
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.DangerRed
import com.example.ui.theme.DarkEmerald
import com.example.ui.theme.DeepObsidian
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.SurfaceElevatedDark
import com.example.ui.theme.TextMutedDark
import com.example.ui.theme.TextPrimaryDark
import com.example.ui.theme.TextSecondaryDark
import com.example.ui.theme.WarningOrange
import androidx.core.content.ContextCompat
import com.example.util.AppNotificationManager
import com.example.util.LockScreenPrivacy
import com.example.util.NotificationConfig
import com.example.util.NotificationPreferences
import com.example.util.PreferenceManager
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

    var showSetPinDialog by remember { mutableStateOf(false) }

    val notifPrefs = remember { NotificationPreferences.get(context) }
    val notifConfig by notifPrefs.configFlow.collectAsState(initial = NotificationConfig())

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

    // Intercept back button: if PIN dialog open, dismiss it first; otherwise invoke onBack()
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
                    Toast.makeText(context, "تم حفظ النسخة الاحتياطية بنجاح", Toast.LENGTH_LONG).show()
                } catch (e: Exception) {
                    Toast.makeText(context, "فشل تصدير النسخة: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
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
                                "تم استعادة ${result.conversationsRestored} محادثة و ${result.messagesRestored} رسالة",
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
        containerColor = DeepObsidian,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = SurfaceDark,
                    titleContentColor = TextPrimaryDark
                ),
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("settings_back_button")) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع", tint = TextPrimaryDark)
                    }
                },
                title = {
                    Text("الإعدادات وحالة الخدمة", fontSize = 17.sp, fontWeight = FontWeight.Bold)
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // Section 1: Detailed Service Status & Diagnostics
            SettingsSectionHeader(title = "حالة الخدمة والتشخيص الدقيق")

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, BorderDark, RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
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

                    HorizontalDivider(color = BorderDark, modifier = Modifier.padding(vertical = 10.dp))

                    // 2. Real Connection Status (Do not claim active merely because permission is granted)
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

                    HorizontalDivider(color = BorderDark, modifier = Modifier.padding(vertical = 10.dp))

                    // 3. Last received notification
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text("آخر إشعار مستلم عبر النظام:", color = TextPrimaryDark, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(4.dp))
                        if (lastReceivedEvent != null) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(SurfaceElevatedDark)
                                    .padding(8.dp)
                            ) {
                                Column {
                                    Text(
                                        text = "${lastReceivedEvent?.chatTitle} (${lastReceivedEvent?.appDisplayName})",
                                        color = NeonGreen,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = lastReceivedEvent?.messageSnippet ?: "",
                                        color = TextSecondaryDark,
                                        fontSize = 12.sp,
                                        maxLines = 1
                                    )
                                    Text(
                                        text = formatTime(lastReceivedEvent?.timestamp ?: 0L),
                                        color = TextMutedDark,
                                        fontSize = 10.sp
                                    )
                                }
                            }
                        } else {
                            Text("لم يصل أي إشعار جديد في الجلسة الحالية", color = TextMutedDark, fontSize = 11.sp)
                        }
                    }

                    HorizontalDivider(color = BorderDark, modifier = Modifier.padding(vertical = 10.dp))

                    // 4. Last saved message in Database
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text("آخر رسالة محفوظة بقاعدة البيانات:", color = TextPrimaryDark, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(4.dp))
                        val lastSaved = lastSavedMessage ?: stats?.lastMessage?.let { "${it.senderName}: ${it.messageText.take(35)}" }
                        if (!lastSaved.isNullOrBlank()) {
                            Text(lastSaved, color = CyanAccent, fontSize = 12.sp)
                        } else {
                            Text("لا توجد رسائل مسجلة بعد", color = TextMutedDark, fontSize = 11.sp)
                        }
                    }

                    // 5. Saving Error Logs
                    if (!lastError.isNullOrBlank()) {
                        HorizontalDivider(color = BorderDark, modifier = Modifier.padding(vertical = 10.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Warning, contentDescription = null, tint = DangerRed, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(lastError ?: "", color = DangerRed, fontSize = 11.sp)
                        }
                    }

                    HorizontalDivider(color = BorderDark, modifier = Modifier.padding(vertical = 10.dp))

                    // Counters
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("المحادثات", color = TextMutedDark, fontSize = 11.sp)
                            Text("${stats?.conversationsCount ?: 0}", color = TextPrimaryDark, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                        Column {
                            Text("الرسائل الكلية", color = TextMutedDark, fontSize = 11.sp)
                            Text("${stats?.messagesCount ?: 0}", color = TextPrimaryDark, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                        Column {
                            Text("الرسائل المحذوفة", color = TextMutedDark, fontSize = 11.sp)
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
                colors = CardDefaults.cardColors(containerColor = DarkEmerald),
                shape = RoundedCornerShape(14.dp)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = Icons.Default.Speed, contentDescription = null, tint = NeonGreen)
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text("اختبار استقبال وحفظ رسالة حقيقية", color = TextPrimaryDark, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Text("انقر لمحاكاة وصول إشعار فوري والتأكد من إضافته لقاعدة البيانات", color = TextSecondaryDark, fontSize = 11.sp)
                    }
                }
            }

            // Section 2: App Notifications Settings (إشعارات التطبيق)
            SettingsSectionHeader(title = "إشعارات التطبيق (نظام التنبيهات المحلي)")

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, BorderDark, RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
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
                                tint = if (notifConfig.areNotificationsEnabled) NeonGreen else TextMutedDark,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    "إشعارات التطبيق",
                                    color = TextPrimaryDark,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    if (notifConfig.areNotificationsEnabled) "مفعلة (شريط الحالة وشاشة القفل)" else "معطلة (افتراضياً)",
                                    color = if (notifConfig.areNotificationsEnabled) NeonGreen else TextMutedDark,
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
                                checkedTrackColor = NeonGreen,
                                uncheckedThumbColor = TextMutedDark,
                                uncheckedTrackColor = SurfaceElevatedDark
                            )
                        )
                    }

                    // Crucial Safety Assurance Note: Notification toggle does NOT affect saving
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(DarkEmerald.copy(alpha = 0.5f))
                            .border(1.dp, NeonGreen.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                            .padding(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.Top) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = null,
                                tint = NeonGreen,
                                modifier = Modifier.size(16.dp).padding(top = 2.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "حماية وحفظ الرسائل: تعمل خدمة رصد وحفظ الرسائل في قاعدة بيانات Room دائماً وبشكل مستقل تماماً، سواء كانت إشعارات التطبيق مفعلة أو معطلة.",
                                color = TextPrimaryDark,
                                fontSize = 11.sp,
                                lineHeight = 16.sp
                            )
                        }
                    }

                    if (notifConfig.areNotificationsEnabled) {
                        HorizontalDivider(color = BorderDark, modifier = Modifier.padding(vertical = 12.dp))

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

                        HorizontalDivider(color = BorderDark, modifier = Modifier.padding(vertical = 12.dp))

                        // Lock Screen Privacy Section
                        Text(
                            text = "مستوى خصوصية شاشة القفل",
                            color = TextPrimaryDark,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "حدد مقدار التفاصيل المعروضة أثناء قفل الهاتف (مع احترام إعدادات خصوصية النظام):",
                            color = TextSecondaryDark,
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
                                        selectedColor = NeonGreen,
                                        unselectedColor = TextMutedDark
                                    )
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Column {
                                    Text(
                                        text = option.title,
                                        color = if (notifConfig.lockScreenPrivacy == option) NeonGreen else TextPrimaryDark,
                                        fontSize = 12.sp,
                                        fontWeight = if (notifConfig.lockScreenPrivacy == option) FontWeight.Bold else FontWeight.Normal
                                    )
                                    Text(
                                        text = option.description,
                                        color = TextMutedDark,
                                        fontSize = 10.sp
                                    )
                                }
                            }
                        }

                        HorizontalDivider(color = BorderDark, modifier = Modifier.padding(vertical = 12.dp))

                        // App Filters
                        Text(
                            text = "التطبيقات المشمولة بالإشعارات",
                            color = TextPrimaryDark,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "اختر التطبيقات التي تريد تلقي إشعارات منها عند وصول رسائلها:",
                            color = TextSecondaryDark,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        val appList = listOf(
                            Triple("واتساب الأصلي", "whatsapp", notifConfig.notifyWhatsApp),
                            Triple("واتساب المستنسخ (MagicOS)", "whatsapp_twin", notifConfig.notifyWhatsAppTwin),
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
                                    Text(name, color = if (isSelected) TextPrimaryDark else TextMutedDark, fontSize = 12.sp)
                                    Switch(
                                        checked = isSelected,
                                        onCheckedChange = { notifPrefs.setAppFilter(key, it) },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = Color.Black,
                                            checkedTrackColor = CyanAccent,
                                            uncheckedThumbColor = TextMutedDark,
                                            uncheckedTrackColor = SurfaceElevatedDark
                                        ),
                                        modifier = Modifier.size(width = 44.dp, height = 24.dp)
                                    )
                                }
                            }
                        }

                        HorizontalDivider(color = BorderDark, modifier = Modifier.padding(vertical = 12.dp))
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
                            colors = ButtonDefaults.buttonColors(containerColor = DarkEmerald, contentColor = NeonGreen),
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
                            colors = ButtonDefaults.buttonColors(containerColor = SurfaceElevatedDark, contentColor = CyanAccent),
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

            // Section 3: Biometric & Security Lock
            SettingsSectionHeader(title = "الأمان وقفل التطبيق")

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, BorderDark, RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Fingerprint, contentDescription = null, tint = NeonGreen)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("القفل بالبصمة / الوجه", color = TextPrimaryDark, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                Text("طلب المصادقة عند فتح التطبيق", color = TextMutedDark, fontSize = 11.sp)
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
                                checkedTrackColor = NeonGreen,
                                uncheckedThumbColor = TextMutedDark,
                                uncheckedTrackColor = SurfaceElevatedDark
                            )
                        )
                    }

                    if (isLockEnabled) {
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedButton(
                            onClick = { showSetPinDialog = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Lock, contentDescription = null, tint = CyanAccent, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (securityManager.hasPinSet()) "تغيير رمز PIN الاحتياطي" else "تعيين رمز PIN الاحتياطي",
                                color = CyanAccent,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }

            // Section 3: Translation Models Management
            SettingsSectionHeader(title = "الترجمة الفورية (Google ML Kit)")

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, BorderDark, RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "حزم الترجمة دون إنترنت (العربية ↔ الإنجليزية)",
                        color = TextPrimaryDark,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "عند تنزيل الحزم، تعمل الترجمة الفورية بالكامل على الجهاز دون إرسال البيانات لأي خوادم خارجية.",
                        color = TextSecondaryDark,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isArabicModelDownloaded && isEnglishModelDownloaded) "الحزم منزلة وجاهزة دون إنترنت ✓" else "الحزم غير منزلة",
                            color = if (isArabicModelDownloaded && isEnglishModelDownloaded) NeonGreen else WarningOrange,
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
                                containerColor = DarkEmerald,
                                contentColor = NeonGreen
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
                                CircularProgressIndicator(color = NeonGreen, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(state.progressText, color = NeonGreen, fontSize = 11.sp)
                            }
                        }
                        is ModelDownloadState.Success -> {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(state.message, color = NeonGreen, fontSize = 11.sp)
                        }
                        is ModelDownloadState.Error -> {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(state.message, color = DangerRed, fontSize = 11.sp)
                        }
                        else -> {}
                    }
                }
            }

            // Section 4: Backup & Restore (SAF)
            SettingsSectionHeader(title = "النسخ الاحتياطي والاستعادة (SAF)")

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, BorderDark, RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "تخزين البيانات والنسخ الاحتياطي",
                        color = TextPrimaryDark,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "البيانات محفوظة في Room Database محلية داخل التخزين الخاص بالتطبيق. يمكنك تصدير أو استعادة النسخ بصيغة JSON بأمان.",
                        color = TextSecondaryDark,
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
                            colors = ButtonDefaults.buttonColors(containerColor = DarkEmerald, contentColor = NeonGreen),
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
                            colors = ButtonDefaults.buttonColors(containerColor = SurfaceElevatedDark, contentColor = CyanAccent),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("استعادة نسخة", fontSize = 12.sp)
                        }
                    }
                }
            }

            // Section 5: HONOR MagicOS Background Guide
            SettingsSectionHeader(title = "توافق HONOR MagicOS")
            HonorOptimizationCard(context = context)

            // Section 6: App Info & Developer Credits
            SettingsSectionHeader(title = "حول التطبيق والمطور")

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, BorderDark, RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(DarkEmerald),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(imageVector = Icons.Default.Info, contentDescription = null, tint = NeonGreen)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("الرسائل المحذوفة • MessageVault Pro", color = TextPrimaryDark, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Text("الإصدار: 1.0.4 (Native Android)", color = NeonGreen, fontSize = 11.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(color = BorderDark)
                    Spacer(modifier = Modifier.height(10.dp))

                    Text("المطور: Waleed Al-khatib (م. وليد الخطيب)", color = TextPrimaryDark, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Text("الجهاز المستهدف: HONOR 600 Pro (MagicOS / Android 16)", color = TextSecondaryDark, fontSize = 12.sp)
                    Text("التقنيات: Kotlin + Jetpack Compose + Room + Google ML Kit", color = TextMutedDark, fontSize = 11.sp, modifier = Modifier.padding(top = 2.dp))
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
            containerColor = SurfaceDark,
            title = {
                Text("تعيين رمز PIN الاحتياطي", color = TextPrimaryDark, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            },
            text = {
                Column {
                    Text("أدخل رمز PIN مكوّن من 4 أرقام لفتح التطبيق في حال عدم توفر البصمة:", color = TextSecondaryDark, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = pinValue,
                        onValueChange = { if (it.length <= 4 && it.all { char -> char.isDigit() }) pinValue = it },
                        label = { Text("رمز PIN (4 أرقام)") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonGreen,
                            unfocusedBorderColor = BorderDark,
                            focusedTextColor = TextPrimaryDark,
                            unfocusedTextColor = TextPrimaryDark
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
                    colors = ButtonDefaults.buttonColors(containerColor = NeonGreen, contentColor = Color.Black)
                ) {
                    Text("حفظ وتفعيل")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSetPinDialog = false }) {
                    Text("إلغاء", color = TextMutedDark)
                }
            }
        )
    }
}

@Composable
fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        color = NeonGreen,
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(start = 4.dp, top = 4.dp)
    )
}

@Composable
fun DiagnosticRow(
    label: String,
    statusText: String,
    isSuccess: Boolean,
    onActionClick: (() -> Unit)? = null,
    actionText: String? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, color = TextPrimaryDark, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Text(
                statusText,
                color = if (isSuccess) NeonGreen else DangerRed,
                fontSize = 11.sp
            )
        }
        if (onActionClick != null && actionText != null) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(SurfaceElevatedDark)
                    .clickable(onClick = onActionClick)
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(actionText, color = CyanAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
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
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = TextPrimaryDark, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Text(subtitle, color = TextMutedDark, fontSize = 11.sp)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.Black,
                checkedTrackColor = NeonGreen,
                uncheckedThumbColor = TextMutedDark,
                uncheckedTrackColor = SurfaceElevatedDark
            )
        )
    }
}

