package com.example.ui.home

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Badge
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.MessageVaultApp
import com.example.data.model.ConversationEntity
import com.example.data.repository.MessageVaultRepository
import com.example.service.NotificationVaultService
import com.example.ui.theme.BorderDark
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.DangerRed
import com.example.ui.theme.DangerRedContainer
import com.example.ui.theme.DarkEmerald
import com.example.ui.theme.DeepObsidian
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.SurfaceElevatedDark
import com.example.ui.theme.SurfaceVariantDark
import com.example.ui.theme.TelegramColor
import com.example.ui.theme.TextMutedDark
import com.example.ui.theme.TextPrimaryDark
import com.example.ui.theme.TextSecondaryDark
import com.example.ui.theme.WarningOrange
import com.example.ui.theme.WhatsAppColor
import com.example.util.PreferenceManager
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    repository: MessageVaultRepository,
    preferenceManager: PreferenceManager,
    onConversationClick: (Long) -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("الكل") }
    var isSearchActive by remember { mutableStateOf(false) }

    val conversationsFlow = remember(searchQuery) {
        if (searchQuery.isBlank()) {
            repository.allConversations
        } else {
            repository.searchConversations(searchQuery)
        }
    }
    val conversations by conversationsFlow.collectAsState(initial = emptyList())

    val isNotificationAccessActive = preferenceManager.isNotificationAccessGranted()
    val isServiceConnected by NotificationVaultService.isServiceConnected.collectAsState()

    // Filter conversations based on selected tab
    val filteredConversations = remember(conversations, selectedFilter) {
        when (selectedFilter) {
            "واتساب" -> conversations.filter { it.packageName.contains("whatsapp") && !it.isClonedApp }
            "مستنسخ (App Twin)" -> conversations.filter { it.isClonedApp }
            "تيليجرام" -> conversations.filter { it.packageName.contains("telegram") }
            "المحذوفة فقط" -> conversations.filter { it.deletedMessagesCount > 0 }
            else -> conversations
        }
    }

    val totalDeleted = conversations.sumOf { it.deletedMessagesCount }

    Scaffold(
        containerColor = DeepObsidian,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = SurfaceDark,
                    titleContentColor = TextPrimaryDark
                ),
                title = {
                    if (isSearchActive) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("بحث باسم المرسل أو محتوى الرسالة...", fontSize = 13.sp, color = TextMutedDark) },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("search_text_field"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NeonGreen,
                                unfocusedBorderColor = BorderDark,
                                focusedTextColor = TextPrimaryDark,
                                unfocusedTextColor = TextPrimaryDark
                            ),
                            trailingIcon = {
                                IconButton(onClick = {
                                    searchQuery = ""
                                    isSearchActive = false
                                }) {
                                    Icon(imageVector = Icons.Default.Close, contentDescription = "إغلاق البحث", tint = TextMutedDark)
                                }
                            }
                        )
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(DarkEmerald),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Shield,
                                    contentDescription = null,
                                    tint = NeonGreen,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "الرسائل المحذوفة",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimaryDark
                                )
                                Text(
                                    text = if (totalDeleted > 0) "$totalDeleted رسالة محذوفة موثقة 🔒" else "الخزينة نشطة وتراقب",
                                    fontSize = 11.sp,
                                    color = if (totalDeleted > 0) NeonGreen else TextMutedDark
                                )
                            }
                        }
                    }
                },
                actions = {
                    if (!isSearchActive) {
                        IconButton(
                            onClick = { isSearchActive = true },
                            modifier = Modifier.testTag("search_button")
                        ) {
                            Icon(imageVector = Icons.Default.Search, contentDescription = "بحث", tint = TextSecondaryDark)
                        }
                    }
                    IconButton(
                        onClick = onNavigateToSettings,
                        modifier = Modifier.testTag("settings_button")
                    ) {
                        Icon(imageVector = Icons.Default.Settings, contentDescription = "الإعدادات", tint = TextSecondaryDark)
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Warning Banner if Notification Access was revoked
            if (!isNotificationAccessActive) {
                AccessRevokedBanner(context = context)
            }

            // Filter chips
            FilterChipsRow(
                selectedFilter = selectedFilter,
                onFilterSelected = { selectedFilter = it },
                totalDeleted = totalDeleted
            )

            // Conversations List or Empty State
            if (filteredConversations.isEmpty()) {
                EmptyStateView(
                    isSearch = searchQuery.isNotBlank(),
                    onInjectTestMessage = {
                        coroutineScope.launch {
                            // Inject a test message and a test deleted message to demonstrate pipeline
                            val testTime = System.currentTimeMillis()
                            repository.saveIncomingNotification(
                                packageName = "com.whatsapp",
                                appDisplayName = "واتساب",
                                chatTitle = "أحمد خالد",
                                senderName = "أحمد خالد",
                                messageText = "السلام عليكم ورحمة الله، هذا نص الرسالة الأصلي الذي سيتم توثيقه وحفظه حتى لو حذفه المرسل!",
                                timestamp = testTime - 60000,
                                isClonedApp = false,
                                userHandleId = 0
                            )
                            // Simulate second message that gets marked deleted
                            val delTime = testTime - 20000
                            repository.saveIncomingNotification(
                                packageName = "com.whatsapp",
                                appDisplayName = "واتساب",
                                chatTitle = "أحمد خالد",
                                senderName = "أحمد خالد",
                                messageText = "رابط الاجتماع السري وكلمة المرور الخاصة بالمشروع: 88924",
                                timestamp = delTime,
                                isClonedApp = false,
                                userHandleId = 0
                            )
                            // Now send deletion notification
                            repository.saveIncomingNotification(
                                packageName = "com.whatsapp",
                                appDisplayName = "واتساب",
                                chatTitle = "أحمد خالد",
                                senderName = "أحمد خالد",
                                messageText = "تم حذف هذه الرسالة",
                                timestamp = testTime - 5000,
                                isClonedApp = false,
                                userHandleId = 0
                            )
                        }
                    }
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("conversations_list"),
                    contentPadding = PaddingValues(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(
                        items = filteredConversations,
                        key = { it.id }
                    ) { conversation ->
                        ConversationCard(
                            conversation = conversation,
                            onClick = { onConversationClick(conversation.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AccessRevokedBanner(context: Context) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp)
            .clickable {
                context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
            },
        colors = CardDefaults.cardColors(containerColor = DangerRedContainer),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(imageVector = Icons.Default.NotificationsOff, contentDescription = null, tint = DangerRed)
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "صلاحية الإشعارات معطلة!",
                    color = DangerRed,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
                Text(
                    text = "انقر هنا لإعادة تفعيل الصلاحية حتى يستمر التطبيق بحفظ الرسائل",
                    color = TextSecondaryDark,
                    fontSize = 11.sp
                )
            }
        }
    }
}

@Composable
fun FilterChipsRow(
    selectedFilter: String,
    onFilterSelected: (String) -> Unit,
    totalDeleted: Int
) {
    val filters = listOf(
        "الكل",
        "المحذوفة فقط",
        "واتساب",
        "مستنسخ (App Twin)",
        "تيليجرام"
    )

    LazyRow(
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(filters) { filter ->
            val isSelected = filter == selectedFilter
            FilterChip(
                selected = isSelected,
                onClick = { onFilterSelected(filter) },
                label = {
                    Text(
                        text = if (filter == "المحذوفة فقط" && totalDeleted > 0) "المحذوفة ($totalDeleted)" else filter,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = DarkEmerald,
                    selectedLabelColor = NeonGreen,
                    containerColor = SurfaceDark,
                    labelColor = TextSecondaryDark
                ),
                border = FilterChipDefaults.filterChipBorder(
                    borderColor = BorderDark,
                    selectedBorderColor = NeonGreen,
                    enabled = true,
                    selected = isSelected
                )
            )
        }
    }
}

@Composable
fun ConversationCard(
    conversation: ConversationEntity,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, BorderDark, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .testTag("conversation_card_${conversation.id}"),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // App / Avatar Icon
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(
                        when {
                            conversation.packageName.contains("whatsapp") -> DarkEmerald
                            conversation.packageName.contains("telegram") -> Color(0xFF0D324D)
                            else -> SurfaceElevatedDark
                        }
                    )
                    .border(
                        1.dp,
                        if (conversation.isClonedApp) CyanAccent else BorderDark,
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = conversation.chatTitle.take(1).uppercase(),
                    color = if (conversation.packageName.contains("whatsapp")) NeonGreen else CyanAccent,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = conversation.chatTitle,
                        color = TextPrimaryDark,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )

                    Text(
                        text = formatTime(conversation.lastMessageTimestamp),
                        color = TextMutedDark,
                        fontSize = 11.sp
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = conversation.lastMessageText,
                        color = TextSecondaryDark,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Badge if cloned app
                        if (conversation.isClonedApp) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFF003840))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "مستنسخ",
                                    color = CyanAccent,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Badge if has deleted messages
                        if (conversation.deletedMessagesCount > 0) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(DangerRedContainer)
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "${conversation.deletedMessagesCount} محذوفة",
                                    color = DangerRed,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Message count badge
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(SurfaceElevatedDark)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "${conversation.messageCount}",
                                color = TextMutedDark,
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyStateView(
    isSearch: Boolean,
    onInjectTestMessage: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(DarkEmerald),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Chat,
                    contentDescription = null,
                    tint = NeonGreen,
                    modifier = Modifier.size(40.dp)
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = if (isSearch) "لا توجد نتائج مطابقة لبحثك" else "لم يتم استقبال أي رسائل بعد",
                color = TextPrimaryDark,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = if (isSearch) "جرّب البحث بكلمة أخرى أو اسم مرسل مختلف"
                else "ستظهر المحادثات هنا تلقائيًا فور وصول إشعارات واتساب أو تيليجرام على هاتفك.",
                color = TextMutedDark,
                fontSize = 13.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.padding(top = 6.dp, bottom = 20.dp)
            )

            if (!isSearch) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onInjectTestMessage),
                    colors = CardDefaults.cardColors(containerColor = SurfaceElevatedDark),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(imageVector = Icons.Default.Warning, contentDescription = null, tint = NeonGreen, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "اختبار فوري: إنشاء رسالة تجريبية ومحذوفة",
                            color = NeonGreen,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

fun formatTime(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    return when {
        diff < 60_000 -> "الآن"
        diff < 3600_000 -> "منذ ${diff / 60_000} د"
        diff < 86400_000 -> {
            val sdf = SimpleDateFormat("h:mm a", Locale("ar"))
            sdf.format(Date(timestamp))
        }
        else -> {
            val sdf = SimpleDateFormat("d MMM", Locale("ar"))
            sdf.format(Date(timestamp))
        }
    }
}
