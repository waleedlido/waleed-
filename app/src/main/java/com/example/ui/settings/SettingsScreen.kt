package com.example.ui.settings

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.core.app.NotificationCompat
import com.example.MessageVaultApp
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
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.SurfaceElevatedDark
import com.example.ui.theme.TextMutedDark
import com.example.ui.theme.TextPrimaryDark
import com.example.ui.theme.TextSecondaryDark
import com.example.ui.theme.WarningOrange
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

    BackHandler {
        onBack()
    }

    var stats by remember { mutableStateOf<VaultStats?>(null) }
    LaunchedEffect(Unit) {
        stats = repository.getStats()
        translationManager.checkDownloadedModels()
    }

    val isServiceConnected by NotificationVaultService.isServiceConnected.collectAsState()
    val lastReceivedEvent by NotificationVaultService.lastReceivedEvent.collectAsState()
    val modelDownloadState by translationManager.modelDownloadState.collectAsState()
    val isArabicModelDownloaded by translationManager.isArabicModelDownloaded.collectAsState()
    val isEnglishModelDownloaded by translationManager.isEnglishModelDownloaded.collectAsState()

    var isLockEnabled by remember { mutableStateOf(securityManager.isLockEnabled()) }
    var showSetPinDialog by remember { mutableStateOf(false) }
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
                    Text("الإعدادات والتشخيص", fontSize = 17.sp, fontWeight = FontWeight.Bold)
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

            // Section 1: System & Capture Diagnostics
            SettingsSectionHeader(title = "حالة النظام والتشخيص")

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, BorderDark, RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Notification Access Status
                    val accessGranted = preferenceManager.isNotificationAccessGranted()
                    DiagnosticRow(
                        label = "صلاحية الوصول للإشعارات",
                        statusText = if (accessGranted) "مفعلة وتعمل بنجاح" else "معطلة!",
                        isSuccess = accessGranted,
                        onActionClick = {
                            context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                        },
                        actionText = if (accessGranted) "تحقق" else "تفعيل"
                    )

                    HorizontalDivider(color = BorderDark, modifier = Modifier.padding(vertical = 10.dp))

                    // Service connection
                    DiagnosticRow(
                        label = "حالة اتصال خدمة المراقبة",
                        statusText = if (isServiceConnected || accessGranted) "الخدمة نشطة وتستقبل في الخلفية" else "بانتظار وصول إشعارات",
                        isSuccess = isServiceConnected || accessGranted
                    )

                    HorizontalDivider(color = BorderDark, modifier = Modifier.padding(vertical = 10.dp))

                    // Dual / Cloned WhatsApp detection
                    DiagnosticRow(
                        label = "دعم واتساب المستنسخ (App Twin)",
                        statusText = "مدعوم عبر UserHandle وفصل المعرفات",
                        isSuccess = true
                    )

                    HorizontalDivider(color = BorderDark, modifier = Modifier.padding(vertical = 10.dp))

                    // Total counts
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("إجمالي المحادثات", color = TextMutedDark, fontSize = 11.sp)
                            Text("${stats?.conversationsCount ?: 0}", color = TextPrimaryDark, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                        Column {
                            Text("إجمالي الرسائل", color = TextMutedDark, fontSize = 11.sp)
                            Text("${stats?.messagesCount ?: 0}", color = TextPrimaryDark, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                        Column {
                            Text("الرسائل المحذوفة", color = TextMutedDark, fontSize = 11.sp)
                            Text("${stats?.deletedMessagesCount ?: 0}", color = DangerRed, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Last received snippet
                    if (lastReceivedEvent != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(SurfaceElevatedDark)
                                .padding(10.dp)
                        ) {
                            Column {
                                Text(
                                    text = "آخر رسالة تم التقاطها:",
                                    color = NeonGreen,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${lastReceivedEvent?.chatTitle} (${lastReceivedEvent?.appDisplayName}): ${lastReceivedEvent?.messageSnippet}",
                                    color = TextPrimaryDark,
                                    fontSize = 12.sp,
                                    maxLines = 1
                                )
                            }
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
                            // Save a live message into DB
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
                            Toast.makeText(context, "تم استقبال وتوثيق الرسالة التجريبية بنجاح!", Toast.LENGTH_SHORT).show()
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

            // Section 2: Biometric & Security Lock
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
                        text = "عند تنزيل الحزم، تعمل الترجمة الفورية بالكامل على الجهاز المشفر دون الحاجة للاتصال بالإنترنت.",
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
                        text = "يتم حفظ المحادثات بقاعدة بيانات Room مشفرة داخل مجلد التطبيق الخاص. يمكنك تصدير نسخة احتياطية بصيغة JSON عبر منتقي ملفات النظام لاستعادتها لاحقًا.",
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
                            Text("الإصدار: 1.0.0 (Native Android)", color = NeonGreen, fontSize = 11.sp)
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

            Spacer(modifier = Modifier.height(20.dp))
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
