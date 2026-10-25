package com.example.ui.permissions

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
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
import com.example.util.PreferenceManager

@Composable
fun PermissionsScreen(
    preferenceManager: PreferenceManager,
    onSetupCompleted: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var isNotificationAccessGranted by remember {
        mutableStateOf(preferenceManager.isNotificationAccessGranted())
    }

    var isPostNotificationGranted by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                androidx.core.content.ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
        )
    }

    // Refresh status when returning from System Settings
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isNotificationAccessGranted = preferenceManager.isNotificationAccessGranted()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    isPostNotificationGranted = androidx.core.content.ContextCompat.checkSelfPermission(
                        context,
                        android.Manifest.permission.POST_NOTIFICATIONS
                    ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val postNotificationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        isPostNotificationGranted = granted
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepObsidian)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Header Icon
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(DarkEmerald)
                    .border(1.5.dp, NeonGreen, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = null,
                    tint = NeonGreen,
                    modifier = Modifier.size(34.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "إعداد الصلاحيات الأساسية",
                color = TextPrimaryDark,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "يحتاج تطبيق الرسائل المحذوفة إلى صلاحية وصول الإشعارات لالتقاط محتوى الرسائل قبل حذفها، بالإضافة لضبط إعدادات بطارية HONOR MagicOS لضمان الاستمرار بالخلفية.",
                color = TextSecondaryDark,
                fontSize = 13.sp,
                lineHeight = 20.sp,
                modifier = Modifier.padding(top = 8.dp, bottom = 20.dp)
            )

            // Step 1: Notification Access Card
            PermissionStepCard(
                stepNumber = "1",
                title = "الوصول إلى الإشعارات (إلزامي)",
                description = "تمكّن التطبيق من التقاط الإشعارات وحفظ الرسائل فور وصولها تلقائيًا على الجهاز المشفر.",
                isGranted = isNotificationAccessGranted,
                actionLabel = if (isNotificationAccessGranted) "مفعلة بنجاح ✓" else "منح صلاحية الإشعارات",
                onAction = {
                    val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                    try {
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        context.startActivity(Intent(Settings.ACTION_SETTINGS))
                    }
                }
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Step 2: Post Notifications (Android 13+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                PermissionStepCard(
                    stepNumber = "2",
                    title = "إشعارات التطبيق (اختياري)",
                    description = "لعرض تنبيه فوري عند قيام الطرف الآخر بحذف رسالة مسجلة.",
                    isGranted = isPostNotificationGranted,
                    actionLabel = if (isPostNotificationGranted) "مفعلة ✓" else "تفعيل إشعارات التطبيق",
                    onAction = {
                        postNotificationLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                    }
                )
                Spacer(modifier = Modifier.height(14.dp))
            }

            // Step 3: HONOR MagicOS Background Guide Card
            HonorOptimizationCard(context = context)

            Spacer(modifier = Modifier.height(28.dp))

            // Confirm & Continue Button
            Button(
                onClick = {
                    preferenceManager.setSetupCompleted(true)
                    onSetupCompleted()
                },
                enabled = isNotificationAccessGranted,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = NeonGreen,
                    contentColor = Color.Black,
                    disabledContainerColor = SurfaceElevatedDark,
                    disabledContentColor = TextMutedDark
                ),
                shape = RoundedCornerShape(14.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = if (isNotificationAccessGranted) "متابعة ودخول التطبيق" else "يرجى تفعيل صلاحية الإشعارات أولاً",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    if (isNotificationAccessGranted) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun PermissionStepCard(
    stepNumber: String,
    title: String,
    description: String,
    isGranted: Boolean,
    actionLabel: String,
    onAction: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, if (isGranted) DarkEmerald else BorderDark, RoundedCornerShape(18.dp)),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(if (isGranted) DarkEmerald else SurfaceElevatedDark)
                        .border(1.dp, if (isGranted) NeonGreen else BorderDark, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (isGranted) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "مفعل",
                            tint = NeonGreen,
                            modifier = Modifier.size(20.dp)
                        )
                    } else {
                        Text(
                            text = stepNumber,
                            color = TextPrimaryDark,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        color = TextPrimaryDark,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (isGranted) "الصلاحية نشطة" else "بانتظار التفعيل",
                        color = if (isGranted) NeonGreen else WarningOrange,
                        fontSize = 11.sp
                    )
                }
            }

            Text(
                text = description,
                color = TextSecondaryDark,
                fontSize = 12.sp,
                lineHeight = 18.sp,
                modifier = Modifier.padding(vertical = 10.dp)
            )

            Button(
                onClick = onAction,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isGranted) SurfaceElevatedDark else DarkEmerald,
                    contentColor = if (isGranted) NeonGreen else BrightNeonGreen
                ),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(
                    text = actionLabel,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

val BrightNeonGreen = Color(0xFF00FF88)

@Composable
fun HonorOptimizationCard(context: Context) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, BorderDark, RoundedCornerShape(18.dp)),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.PhoneAndroid,
                    contentDescription = null,
                    tint = CyanAccent,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "إرشادات HONOR 600 Pro (MagicOS)",
                    color = TextPrimaryDark,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Text(
                text = "لمنع نظام MagicOS من إيقاف الخدمة في الخلفية عند قفل الشاشة:",
                color = TextMutedDark,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 6.dp, bottom = 10.dp)
            )

            HonorInstructionStep(number = "١", text = "افتح الإعدادات > البطارية > تشغيل التطبيقات (App Launch)")
            HonorInstructionStep(number = "٢", text = "ابحث عن تطبيق 'الرسائل المحذوفة' واجعله 'إدارة يدوية'")
            HonorInstructionStep(number = "٣", text = "فعّل الخيارات الثلاثة: (التشغيل التلقائي + التشغيل الثانوي + التشغيل بالخلفية)")
            HonorInstructionStep(number = "٤", text = "في تحسين البطارية: اختر 'عدم التحسين / بلا قيود'")

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedButton(
                onClick = {
                    try {
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.fromParts("package", context.packageName, null)
                        }
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        context.startActivity(Intent(Settings.ACTION_SETTINGS))
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(imageVector = Icons.Default.BatteryChargingFull, contentDescription = null, tint = CyanAccent, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("فتح إعدادات التطبيق بالنظام", color = CyanAccent, fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun HonorInstructionStep(number: String, text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = "$number. ",
            color = NeonGreen,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = text,
            color = TextSecondaryDark,
            fontSize = 12.sp,
            lineHeight = 18.sp
        )
    }
}
