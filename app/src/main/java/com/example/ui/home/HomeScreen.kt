package com.example.ui.home

import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.material.icons.filled.AutoDelete
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
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
import com.example.ui.components.AppAvatarIcon
import com.example.ui.components.AppSourceBadge
import com.example.ui.components.AppSourceIcon
import com.example.util.DateTimeUtils
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
    onConversationClick: (Long) -> Unit,
    onNavigateToTrash: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var selectedTab by remember { mutableStateOf(MainTab.HOME) }
    val homeListState = rememberLazyListState()

    // Multi-selection state
    val selectedIds = remember { mutableStateListOf<Long>() }
    val isSelectionMode = selectedIds.isNotEmpty()

    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    // Intercept back button:
    // 1. If in selection mode, cancel selection mode first.
    // 2. If on another tab, return to HOME tab.
    // 3. If on HOME tab without selection, default OS back (exit).
    BackHandler(enabled = isSelectionMode || selectedTab != MainTab.HOME) {
        if (isSelectionMode) {
            selectedIds.clear()
        } else {
            selectedTab = MainTab.HOME
        }
    }

    val isNotificationAccessActive = preferenceManager.isNotificationAccessGranted()
    val conversations by repository.allConversations.collectAsState(initial = emptyList())
    val trashCount by repository.trashCount.collectAsState(initial = 0)
    val totalDeleted = conversations.sumOf { it.deletedMessagesCount }

    val themeColors = com.example.ui.theme.VaultTheme.colors

    Scaffold(
        containerColor = themeColors.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            if (isSelectionMode) {
                // Contextual Multi-Selection Top Bar
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = themeColors.surfaceElevated,
                        titleContentColor = themeColors.textPrimary
                    ),
                    navigationIcon = {
                        IconButton(onClick = { selectedIds.clear() }) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "إلغاء التحديد", tint = themeColors.textPrimary)
                        }
                    },
                    title = {
                        Text(
                            text = "تم تحديد ${selectedIds.size}",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = themeColors.textPrimary
                        )
                    },
                    actions = {
                        // Select All button
                        IconButton(onClick = {
                            if (selectedIds.size == conversations.size) {
                                selectedIds.clear()
                            } else {
                                selectedIds.clear()
                                selectedIds.addAll(conversations.map { it.id })
                            }
                        }) {
                            Icon(imageVector = Icons.Default.SelectAll, contentDescription = "تحديد الكل", tint = themeColors.secondary)
                        }

                        // Delete Selected button (moves to Recycle Bin)
                        IconButton(onClick = { showDeleteConfirmDialog = true }) {
                            Icon(imageVector = Icons.Default.Delete, contentDescription = "نقل إلى سلة المحذوفات", tint = DangerRed)
                        }
                    }
                )
            } else {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = themeColors.surface,
                        titleContentColor = themeColors.textPrimary
                    ),
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(themeColors.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Shield,
                                    contentDescription = null,
                                    tint = themeColors.primary,
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
                    },
                    actions = {
                        // Recycle Bin Shortcut with badge count
                        IconButton(
                            onClick = onNavigateToTrash,
                            modifier = Modifier.testTag("recycle_bin_shortcut")
                        ) {
                            BadgedBox(
                                badge = {
                                    if (trashCount > 0) {
                                        Badge(
                                            containerColor = DangerRed,
                                            contentColor = Color.White
                                        ) {
                                            Text("$trashCount")
                                        }
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoDelete,
                                    contentDescription = "سلة المحذوفات",
                                    tint = if (trashCount > 0) DangerRed else TextSecondaryDark
                                )
                            }
                        }
                    }
                )
            }
        },
        bottomBar = {
            NavigationBar(
                containerColor = themeColors.surface,
                tonalElevation = 8.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(width = 0.5.dp, color = themeColors.border)
            ) {
                // Tab 1: Home
                NavigationBarItem(
                    selected = selectedTab == MainTab.HOME,
                    onClick = {
                        if (isSelectionMode) selectedIds.clear()
                        selectedTab = MainTab.HOME
                    },
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
                        selectedIconColor = themeColors.primary,
                        selectedTextColor = themeColors.primary,
                        indicatorColor = themeColors.primaryContainer,
                        unselectedIconColor = themeColors.textMuted,
                        unselectedTextColor = themeColors.textMuted
                    )
                )

                // Tab 2: Deleted Messages
                NavigationBarItem(
                    selected = selectedTab == MainTab.DELETED,
                    onClick = {
                        if (isSelectionMode) selectedIds.clear()
                        selectedTab = MainTab.DELETED
                    },
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
                        unselectedIconColor = themeColors.textMuted,
                        unselectedTextColor = themeColors.textMuted
                    )
                )

                // Tab 3: Search
                NavigationBarItem(
                    selected = selectedTab == MainTab.SEARCH,
                    onClick = {
                        if (isSelectionMode) selectedIds.clear()
                        selectedTab = MainTab.SEARCH
                    },
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
                        selectedIconColor = themeColors.secondary,
                        selectedTextColor = themeColors.secondary,
                        indicatorColor = themeColors.primaryContainer,
                        unselectedIconColor = themeColors.textMuted,
                        unselectedTextColor = themeColors.textMuted
                    )
                )

                // Tab 4: Settings & Service Status
                NavigationBarItem(
                    selected = selectedTab == MainTab.SETTINGS,
                    onClick = {
                        if (isSelectionMode) selectedIds.clear()
                        selectedTab = MainTab.SETTINGS
                    },
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
                        selectedIconColor = themeColors.primary,
                        selectedTextColor = themeColors.primary,
                        indicatorColor = themeColors.primaryContainer,
                        unselectedIconColor = themeColors.textMuted,
                        unselectedTextColor = themeColors.textMuted
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
                        selectedIds = selectedIds,
                        isSelectionMode = isSelectionMode,
                        isNotificationAccessActive = isNotificationAccessActive,
                        onConversationClick = { id ->
                            if (isSelectionMode) {
                                if (selectedIds.contains(id)) selectedIds.remove(id) else selectedIds.add(id)
                            } else {
                                onConversationClick(id)
                            }
                        },
                        onConversationLongClick = { id ->
                            if (!selectedIds.contains(id)) {
                                selectedIds.add(id)
                            }
                        },
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

    // Confirmation Dialog before moving conversations to trash
    if (showDeleteConfirmDialog) {
        val count = selectedIds.size
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            containerColor = SurfaceDark,
            title = {
                Text("نقل إلى سلة المحذوفات؟", color = TextPrimaryDark, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            },
            text = {
                Text(
                    "سيتم نقل $count محادثة إلى «سلة المحذوفات». يمكنك استعادتها في أي وقت أو حذفها نهائيًا من هناك.",
                    color = TextSecondaryDark,
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirmDialog = false
                        val idsToTrash = selectedIds.toList()
                        selectedIds.clear()
                        coroutineScope.launch {
                            repository.moveMultipleToTrash(idsToTrash)
                            val snackbarResult = snackbarHostState.showSnackbar(
                                message = "تم نقل $count محادثة إلى سلة المحذوفات",
                                actionLabel = "تراجع",
                                duration = SnackbarDuration.Short
                            )
                            if (snackbarResult == SnackbarResult.ActionPerformed) {
                                repository.restoreMultipleFromTrash(idsToTrash)
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DangerRed, contentColor = Color.White)
                ) {
                    Text("نقل للسلة")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("إلغاء", color = TextMutedDark)
                }
            }
        )
    }
}

@Composable
fun HomeTabContent(
    listState: LazyListState,
    conversations: List<ConversationEntity>,
    selectedIds: List<Long>,
    isSelectionMode: Boolean,
    isNotificationAccessActive: Boolean,
    onConversationClick: (Long) -> Unit,
    onConversationLongClick: (Long) -> Unit,
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
            "سيجنال" -> conversations.filter { it.packageName.contains("signal") || it.packageName.contains("securesms") }
            "SMS" -> conversations.filter { it.packageName.contains("mms") || it.packageName.contains("messaging") }
            else -> conversations
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        if (!isNotificationAccessActive) {
            AccessRevokedBanner(context = context)
        }

        // App Filters - Horizontally scrollable without clipping
        val filterOptions = listOf("الكل", "واتساب", "مستنسخ (App Twin)", "تيليجرام", "ماسنجر", "سيجنال", "SMS")
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
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
                    val isSelected = selectedIds.contains(conversation.id)
                    ConversationCard(
                        conversation = conversation,
                        isSelected = isSelected,
                        isSelectionMode = isSelectionMode,
                        onClick = { onConversationClick(conversation.id) },
                        onLongClick = { onConversationLongClick(conversation.id) }
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ConversationCard(
    conversation: ConversationEntity,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val context = LocalContext.current
    val themeColors = com.example.ui.theme.VaultTheme.colors
    val privacyManager = remember { com.example.util.PrivacyBlurManager.get(context) }
    val privacyConfig by privacyManager.stateFlow.collectAsState()

    val isSenderBlurred = privacyConfig.isMasterBlurEnabled && privacyConfig.blurSenderNames
    val isPreviewBlurred = privacyConfig.isMasterBlurEnabled && privacyConfig.blurHomePreviews

    val displayTitle = if (isSenderBlurred) "••••••••" else conversation.chatTitle
    val displayPreview = if (isPreviewBlurred) "••••••••••••••••••••••••" else conversation.lastMessageText

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(
                width = if (isSelected) 1.5.dp else 1.dp,
                color = if (isSelected) themeColors.primary else themeColors.border,
                shape = RoundedCornerShape(16.dp)
            )
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .testTag("conversation_card_${conversation.id}"),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) themeColors.surfaceElevated else themeColors.surface
        )
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isSelectionMode) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(if (isSelected) themeColors.primary else themeColors.surfaceElevated)
                        .border(1.5.dp, if (isSelected) themeColors.primary else themeColors.border, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
            }

            // Single distinct App Icon Avatar (48-52dp) with real PackageManager logo and single App Twin badge
            AppAvatarIcon(
                packageName = conversation.packageName,
                isClonedApp = conversation.isClonedApp,
                size = 50.dp
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                // Header row: Sender Name (chatTitle) and Timestamp without icon duplication
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = displayTitle,
                        color = themeColors.textPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = DateTimeUtils.formatConversationTimestamp(conversation.lastMessageTimestamp),
                        color = themeColors.textMuted,
                        fontSize = 11.sp
                    )
                }

                Spacer(modifier = Modifier.height(5.dp))

                // Subtitle row: Last Message Snippet (or blurred) and Badges
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = displayPreview,
                        color = themeColors.textSecondary,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Deleted messages count badge (if any)
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

                        // Total message count badge
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(themeColors.surfaceElevated)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "${conversation.messageCount}",
                                color = themeColors.textMuted,
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
    return DateTimeUtils.formatConversationTimestamp(timestamp)
}
