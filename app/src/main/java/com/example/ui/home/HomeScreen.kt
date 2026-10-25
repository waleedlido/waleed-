package com.example.ui.home

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
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
import com.example.data.model.ConversationEntity
import com.example.data.repository.MessageVaultRepository
import com.example.ml.TranslationManager
import com.example.security.SecurityManager
import com.example.service.NotificationVaultService
import com.example.ui.components.AppAvatarIcon
import com.example.ui.components.AppSourceBadge
import com.example.ui.home.tabs.DeletedTab
import com.example.ui.home.tabs.SearchTab
import com.example.ui.settings.SettingsScreen
import com.example.ui.theme.BorderDark
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.DangerRed
import com.example.ui.theme.DangerRedContainer
import com.example.ui.theme.DarkEmerald
import com.example.ui.theme.DeepObsidian
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.SurfaceElevatedDark
import com.example.ui.theme.TextMutedDark
import com.example.ui.theme.TextPrimaryDark
import com.example.ui.theme.TextSecondaryDark
import com.example.util.PreferenceManager
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class MainTab {
    HOME,
    DELETED,
    SEARCH,
    SETTINGS
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    repository: MessageVaultRepository,
    translationManager: TranslationManager,
    securityManager: SecurityManager,
    preferenceManager: PreferenceManager,
    onConversationClick: (Long) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var selectedTab by remember { mutableStateOf(MainTab.HOME) }
    val homeListState = rememberLazyListState()

    // Back button behavior: if on any sub-tab, return to HOME tab; if on HOME tab, default system exit
    BackHandler(enabled = selectedTab != MainTab.HOME) {
        selectedTab = MainTab.HOME
    }

    val isNotificationAccessActive = preferenceManager.isNotificationAccessGranted()
    val conversations by repository.allConversations.collectAsState(initial = emptyList())
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
                                text = when (selectedTab) {
                                    MainTab.HOME -> if (totalDeleted > 0) "$totalDeleted رسالة محذوفة موثقة 🔒" else "الخزينة نشطة وتراقب"
                                    MainTab.DELETED -> "سجل الرسائل المسترجعة ($totalDeleted)"
                                    MainTab.SEARCH -> "البحث الشامل في الرسائل"
                                    MainTab.SETTINGS -> "الإعدادات وحالة الخدمة"
                                },
                                fontSize = 11.sp,
                                color = if (totalDeleted > 0 && (selectedTab == MainTab.HOME || selectedTab == MainTab.DELETED)) NeonGreen else TextMutedDark
                            )
                        }
                    }
                }
            )
        },
        bottomBar = {
            // Fixed Bottom Navigation Bar with 4 Tabs in Dark Green / Obsidian Style
            NavigationBar(
                containerColor = SurfaceDark,
                tonalElevation = 8.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(width = 0.5.dp, color = BorderDark)
            ) {
                // Tab 1: Home
                NavigationBarItem(
                    selected = selectedTab == MainTab.HOME,
                    onClick = { selectedTab = MainTab.HOME },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Forum,
                            contentDescription = "الرئيسية",
                            modifier = Modifier.size(22.dp)
                        )
                    },
                    label = {
                        Text(
                            text = "الرئيسية",
                            fontSize = 11.sp,
                            fontWeight = if (selectedTab == MainTab.HOME) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = NeonGreen,
                        selectedTextColor = NeonGreen,
                        indicatorColor = DarkEmerald,
                        unselectedIconColor = TextMutedDark,
                        unselectedTextColor = TextMutedDark
                    )
                )

                // Tab 2: Deleted Messages
                NavigationBarItem(
                    selected = selectedTab == MainTab.DELETED,
                    onClick = { selectedTab = MainTab.DELETED },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.DeleteSweep,
                            contentDescription = "المحذوفة",
                            modifier = Modifier.size(22.dp)
                        )
                    },
                    label = {
                        Text(
                            text = if (totalDeleted > 0) "المحذوفة ($totalDeleted)" else "المحذوفة",
                            fontSize = 11.sp,
                            fontWeight = if (selectedTab == MainTab.DELETED) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = DangerRed,
                        selectedTextColor = DangerRed,
                        indicatorColor = DangerRedContainer,
                        unselectedIconColor = TextMutedDark,
                        unselectedTextColor = TextMutedDark
                    )
                )

                // Tab 3: Search
                NavigationBarItem(
                    selected = selectedTab == MainTab.SEARCH,
                    onClick = { selectedTab = MainTab.SEARCH },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "البحث",
                            modifier = Modifier.size(22.dp)
                        )
                    },
                    label = {
                        Text(
                            text = "البحث",
                            fontSize = 11.sp,
                            fontWeight = if (selectedTab == MainTab.SEARCH) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = CyanAccent,
                        selectedTextColor = CyanAccent,
                        indicatorColor = Color(0xFF003840),
                        unselectedIconColor = TextMutedDark,
                        unselectedTextColor = TextMutedDark
                    )
                )

                // Tab 4: Settings & Service Status
                NavigationBarItem(
                    selected = selectedTab == MainTab.SETTINGS,
                    onClick = { selectedTab = MainTab.SETTINGS },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "الإعدادات",
                            modifier = Modifier.size(22.dp)
                        )
                    },
                    label = {
                        Text(
                            text = "الإعدادات",
                            fontSize = 11.sp,
                            fontWeight = if (selectedTab == MainTab.SETTINGS) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = NeonGreen,
                        selectedTextColor = NeonGreen,
                        indicatorColor = DarkEmerald,
                        unselectedIconColor = TextMutedDark,
                        unselectedTextColor = TextMutedDark
                    )
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                MainTab.HOME -> {
                    HomeTabContent(
                        listState = homeListState,
                        conversations = conversations,
                        isNotificationAccessActive = isNotificationAccessActive,
                        onConversationClick = onConversationClick,
                        onInjectTestMessage = {
                            coroutineScope.launch {
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
                }

                MainTab.DELETED -> {
                    DeletedTab(
                        repository = repository,
                        onConversationClick = onConversationClick
                    )
                }

                MainTab.SEARCH -> {
                    SearchTab(
                        repository = repository,
                        onConversationClick = onConversationClick
                    )
                }

                MainTab.SETTINGS -> {
                    SettingsScreen(
                        repository = repository,
                        translationManager = translationManager,
                        securityManager = securityManager,
                        preferenceManager = preferenceManager,
                        onBack = {
                            selectedTab = MainTab.HOME
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun HomeTabContent(
    listState: LazyListState,
    conversations: List<ConversationEntity>,
    isNotificationAccessActive: Boolean,
    onConversationClick: (Long) -> Unit,
    onInjectTestMessage: () -> Unit
) {
    val context = LocalContext.current
    var selectedFilter by remember { mutableStateOf("الكل") }

    val filteredConversations = remember(conversations, selectedFilter) {
        when (selectedFilter) {
            "واتساب" -> conversations.filter { it.packageName.contains("whatsapp") && !it.isClonedApp }
            "مستنسخ (App Twin)" -> conversations.filter { it.isClonedApp }
            "تيليجرام" -> conversations.filter { it.packageName.contains("telegram") }
            "ماسنجر" -> conversations.filter { it.packageName.contains("orca") || it.packageName.contains("messenger") }
            "SMS" -> conversations.filter { it.packageName.contains("mms") || it.packageName.contains("messaging") }
            else -> conversations
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Warning Banner if Notification Access was revoked
        if (!isNotificationAccessActive) {
            AccessRevokedBanner(context = context)
        }

        // App Filters
        val filterOptions = listOf("الكل", "واتساب", "مستنسخ (App Twin)", "تيليجرام", "ماسنجر", "SMS")
        LazyRow(
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(filterOptions) { filter ->
                val isSelected = filter == selectedFilter
                FilterChip(
                    selected = isSelected,
                    onClick = { selectedFilter = filter },
                    label = {
                        Text(
                            text = filter,
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

        if (filteredConversations.isEmpty()) {
            EmptyStateView(
                isSearch = false,
                onInjectTestMessage = onInjectTestMessage
            )
        } else {
            LazyColumn(
                state = listState,
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
            AppAvatarIcon(
                packageName = conversation.packageName,
                chatTitle = conversation.chatTitle,
                isClonedApp = conversation.isClonedApp
            )

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
                        // Badge for App source (WhatsApp, App Twin, Telegram, etc.)
                        AppSourceBadge(
                            packageName = conversation.packageName,
                            isClonedApp = conversation.isClonedApp
                        )

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
                        Icon(imageVector = Icons.Default.Speed, contentDescription = null, tint = NeonGreen, modifier = Modifier.size(18.dp))
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
