package com.example.ui.chat

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.ConversationEntity
import com.example.data.model.MessageEntity
import com.example.data.repository.MessageVaultRepository
import com.example.ml.TranslationManager
import com.example.ml.TranslationResult
import com.example.ui.components.AppAvatarIcon
import com.example.ui.components.AppSourceIcon
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.DangerRed
import com.example.ui.theme.DangerRedContainer
import com.example.ui.theme.DarkEmerald
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.VaultTheme
import com.example.util.AppIconProvider
import com.example.util.ChatWallpaperManager
import com.example.util.DateTimeUtils
import com.example.util.PresetWallpapers
import com.example.util.PrivacyBlurManager
import com.example.util.WallpaperConfig
import com.example.util.WallpaperScale
import com.example.util.WallpaperType
import kotlinx.coroutines.launch
import java.io.File

/**
 * Global cache to preserve scroll position for each conversation across back/forward navigation.
 */
object ChatScrollStateCache {
    private val scrollPositions = mutableMapOf<Long, Pair<Int, Int>>()

    fun getPosition(conversationId: Long): Pair<Int, Int>? = scrollPositions[conversationId]

    fun savePosition(conversationId: Long, index: Int, offset: Int) {
        scrollPositions[conversationId] = Pair(index, offset)
    }

    fun clear(conversationId: Long) {
        scrollPositions.remove(conversationId)
    }

    fun clearAll() {
        scrollPositions.clear()
    }
}

suspend fun LazyListState.fastScrollTo(targetIndex: Int) {
    val total = layoutInfo.totalItemsCount
    if (total <= 0) return
    val safeTarget = targetIndex.coerceIn(0, total - 1)
    val current = firstVisibleItemIndex
    val distance = kotlin.math.abs(current - safeTarget)
    if (distance > 10) {
        val intermediate = if (safeTarget > current) {
            (safeTarget - 5).coerceAtLeast(0)
        } else {
            (safeTarget + 5).coerceAtMost(total - 1)
        }
        scrollToItem(intermediate)
    }
    animateScrollToItem(safeTarget)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatDetailScreen(
    conversationId: Long,
    repository: MessageVaultRepository,
    translationManager: TranslationManager,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val themeColors = VaultTheme.colors

    val wallpaperManager = remember { ChatWallpaperManager.get(context) }
    val wallpaperConfig by wallpaperManager.getConversationWallpaperFlow(conversationId)
        .collectAsState(initial = WallpaperConfig())

    val privacyManager = remember { PrivacyBlurManager.get(context) }
    val privacyConfig by privacyManager.stateFlow.collectAsState()

    // Temporary reveal state: automatically reblurs on exit
    val revealedMessageIds = remember { mutableStateListOf<Long>() }

    BackHandler {
        revealedMessageIds.clear()
        onBack()
    }

    var conversation by remember { mutableStateOf<ConversationEntity?>(null) }
    LaunchedEffect(conversationId) {
        conversation = repository.getConversationById(conversationId)
    }

    val messages by repository.getMessagesForConversation(conversationId).collectAsState(initial = emptyList())

    val cachedPosition = remember(conversationId) { ChatScrollStateCache.getPosition(conversationId) }
    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = cachedPosition?.first ?: 0,
        initialFirstVisibleItemScrollOffset = cachedPosition?.second ?: 0
    )

    DisposableEffect(conversationId) {
        onDispose {
            ChatScrollStateCache.savePosition(
                conversationId,
                listState.firstVisibleItemIndex,
                listState.firstVisibleItemScrollOffset
            )
        }
    }

    val isScrolledToBottom by remember {
        derivedStateOf {
            val total = listState.layoutInfo.totalItemsCount
            if (total == 0) return@derivedStateOf true
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            lastVisible >= total - 2
        }
    }

    var lastSeenMessageCount by remember { mutableStateOf(0) }
    var hasNewMessagePill by remember { mutableStateOf(false) }

    LaunchedEffect(messages.size) {
        if (lastSeenMessageCount == 0) {
            if (cachedPosition == null && messages.isNotEmpty()) {
                listState.fastScrollTo(messages.size - 1)
            }
            lastSeenMessageCount = messages.size
        } else if (messages.size > lastSeenMessageCount) {
            if (isScrolledToBottom) {
                listState.fastScrollTo(messages.size - 1)
            } else {
                hasNewMessagePill = true
            }
            lastSeenMessageCount = messages.size
        }
    }

    val showScrollToTop by remember {
        derivedStateOf { listState.firstVisibleItemIndex > 3 }
    }
    val showScrollToBottom by remember {
        derivedStateOf {
            val total = listState.layoutInfo.totalItemsCount
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            total > 6 && lastVisible < total - 3
        }
    }

    var showMenu by remember { mutableStateOf(false) }
    var isTranslatingAll by remember { mutableStateOf(false) }
    var selectedMessageForDetails by remember { mutableStateOf<MessageEntity?>(null) }

    Scaffold(
        containerColor = themeColors.background,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = themeColors.surface,
                    titleContentColor = themeColors.textPrimary
                ),
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("chat_back_button")) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "رجوع",
                            tint = themeColors.textPrimary
                        )
                    }
                },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // 1. Single Application Icon Avatar in Chat Header (38-42dp)
                        conversation?.let { conv ->
                            AppAvatarIcon(
                                packageName = conv.packageName,
                                isClonedApp = conv.isClonedApp,
                                size = 38.dp
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                        }

                        Column {
                            val isSenderBlurred = privacyConfig.isMasterBlurEnabled && privacyConfig.blurSenderNames
                            val displayTitle = if (isSenderBlurred) "••••••••" else (conversation?.chatTitle ?: "المحادثة")

                            Text(
                                text = displayTitle,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = themeColors.textPrimary,
                                maxLines = 1
                            )

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "${messages.size} رسالة",
                                    fontSize = 11.sp,
                                    color = themeColors.textMuted
                                )
                                conversation?.let { conv ->
                                    if (conv.deletedMessagesCount > 0) {
                                        Text(
                                            text = " • ${conv.deletedMessagesCount} محذوفة",
                                            fontSize = 11.sp,
                                            color = DangerRed,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "خيارات",
                            tint = themeColors.textSecondary
                        )
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                        modifier = Modifier.background(themeColors.surface)
                    ) {
                        DropdownMenuItem(
                            text = { Text("ترجمة جميع الرسائل الظاهرة", color = themeColors.textPrimary) },
                            leadingIcon = { Icon(Icons.Default.Translate, contentDescription = null, tint = themeColors.primary) },
                            onClick = {
                                showMenu = false
                                isTranslatingAll = true
                                coroutineScope.launch {
                                    messages.forEach { msg ->
                                        if (msg.translatedText == null) {
                                            val result = translationManager.translateText(msg.messageText)
                                            if (result is TranslationResult.Success) {
                                                repository.updateMessageTranslation(msg.id, result.translatedText, result.languageCode)
                                            }
                                        }
                                    }
                                    isTranslatingAll = false
                                    Toast.makeText(context, "تمت ترجمة الرسائل بنجاح", Toast.LENGTH_SHORT).show()
                                }
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("حذف المحادثة إلى السلة", color = DangerRed) },
                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = DangerRed) },
                            onClick = {
                                showMenu = false
                                coroutineScope.launch {
                                    repository.moveConversationToTrash(conversationId)
                                    onBack()
                                }
                            }
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Layer 1: Chat Wallpaper with dimming and optional blur
            when (wallpaperConfig.type) {
                WallpaperType.CUSTOM_PHOTO -> {
                    wallpaperConfig.customImagePath?.let { path ->
                        val file = File(path)
                        if (file.exists()) {
                            val scale = when (wallpaperConfig.scaleMode) {
                                WallpaperScale.FILL_CROP -> ContentScale.Crop
                                WallpaperScale.FIT -> ContentScale.Fit
                                WallpaperScale.CENTER -> ContentScale.Inside
                            }
                            AsyncImage(
                                model = file,
                                contentDescription = null,
                                contentScale = scale,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .then(if (wallpaperConfig.isBlurEnabled) Modifier.blur(16.dp) else Modifier)
                            )
                            // Dimming layer (Default 40%, range 0-80%)
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black.copy(alpha = wallpaperConfig.dimmingOpacity))
                            )
                        }
                    }
                }

                WallpaperType.PRESET -> {
                    val preset = PresetWallpapers.find(wallpaperConfig.presetId) ?: PresetWallpapers.list.first()
                    val colors = preset.previewGradientColors.map { Color(it) }
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Brush.verticalGradient(colors))
                            .then(if (wallpaperConfig.isBlurEnabled) Modifier.blur(16.dp) else Modifier)
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = wallpaperConfig.dimmingOpacity))
                    )
                }

                WallpaperType.DEFAULT -> {
                    // Default clean background
                    Box(modifier = Modifier.fillMaxSize().background(themeColors.background))
                }
            }

            // Layer 2: Messages List
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("chat_messages_list"),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                itemsIndexed(
                    items = messages,
                    key = { _, it -> it.id }
                ) { index, message ->
                    val showDateHeader = index == 0 || !DateTimeUtils.isSameDay(messages[index - 1].timestamp, message.timestamp)

                    Column(modifier = Modifier.fillMaxWidth()) {
                        if (showDateHeader) {
                            DateSeparatorHeader(
                                label = DateTimeUtils.formatDateHeader(message.timestamp),
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }

                        val isBlurActive = privacyConfig.isMasterBlurEnabled && privacyConfig.blurChatMessages
                        val isMessageRevealed = revealedMessageIds.contains(message.id)

                        MessageBubble(
                            message = message,
                            isBlurred = isBlurActive && !isMessageRevealed,
                            onToggleReveal = {
                                if (revealedMessageIds.contains(message.id)) {
                                    revealedMessageIds.remove(message.id)
                                } else {
                                    revealedMessageIds.add(message.id)
                                }
                            },
                            onBubbleClick = { selectedMessageForDetails = message },
                            onTranslateClick = {
                                coroutineScope.launch {
                                    val result = translationManager.translateText(message.messageText)
                                    when (result) {
                                        is TranslationResult.Success -> {
                                            repository.updateMessageTranslation(message.id, result.translatedText, result.languageCode)
                                        }
                                        is TranslationResult.Error -> {
                                            Toast.makeText(context, result.message, Toast.LENGTH_LONG).show()
                                        }
                                    }
                                }
                            }
                        )
                    }
                }
            }

            // Floating Navigation Arrows (Scroll Up & Scroll Down)
            Column(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Arrow UP: Scroll to oldest message (index 0)
                AnimatedVisibility(
                    visible = showScrollToTop,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    FloatingActionButton(
                        onClick = {
                            coroutineScope.launch {
                                listState.fastScrollTo(0)
                            }
                        },
                        modifier = Modifier
                            .size(46.dp)
                            .testTag("scroll_to_top_button"),
                        shape = CircleShape,
                        containerColor = themeColors.surfaceElevated,
                        contentColor = themeColors.primary,
                        elevation = FloatingActionButtonDefaults.elevation(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowUpward,
                            contentDescription = "الانتقال لأول رسالة",
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                // Arrow DOWN: Scroll to newest message
                AnimatedVisibility(
                    visible = showScrollToBottom,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    FloatingActionButton(
                        onClick = {
                            coroutineScope.launch {
                                if (messages.isNotEmpty()) {
                                    listState.fastScrollTo(messages.size - 1)
                                }
                            }
                        },
                        modifier = Modifier
                            .size(46.dp)
                            .testTag("scroll_to_bottom_button"),
                        shape = CircleShape,
                        containerColor = themeColors.primaryContainer,
                        contentColor = themeColors.primary,
                        elevation = FloatingActionButtonDefaults.elevation(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowDownward,
                            contentDescription = "الانتقال لآخر رسالة",
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            // New Message Floating Pill Banner
            AnimatedVisibility(
                visible = hasNewMessagePill,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 24.dp)
            ) {
                Surface(
                    onClick = {
                        hasNewMessagePill = false
                        coroutineScope.launch {
                            if (messages.isNotEmpty()) {
                                listState.fastScrollTo(messages.size - 1)
                            }
                        }
                    },
                    shape = RoundedCornerShape(24.dp),
                    color = themeColors.primaryContainer,
                    shadowElevation = 6.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowDownward,
                            contentDescription = null,
                            tint = themeColors.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "رسائل جديدة في الأسفل ↓",
                            color = themeColors.primary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            if (isTranslatingAll) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(20.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(color = themeColors.primary, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(14.dp))
                            Text("جاري ترجمة الرسائل بالذكاء الاصطناعي...", color = themeColors.textPrimary, fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }

    // Message Details Modal Dialog
    selectedMessageForDetails?.let { msg ->
        MessageDetailsDialog(
            message = msg,
            packageName = conversation?.packageName ?: "",
            isClonedApp = conversation?.isClonedApp ?: false,
            onDismiss = { selectedMessageForDetails = null }
        )
    }
}

@Composable
fun DateSeparatorHeader(
    label: String,
    modifier: Modifier = Modifier
) {
    val themeColors = VaultTheme.colors
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(themeColors.surfaceElevated)
                .border(1.dp, themeColors.border, RoundedCornerShape(12.dp))
                .padding(horizontal = 14.dp, vertical = 5.dp)
        ) {
            Text(
                text = label,
                color = themeColors.textSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

/**
 * Modern Clean Message Bubble:
 * - NO app logo inside bubble.
 * - Organized layout with natural text wrapping.
 * - Privacy blur support with Eye reveal button.
 * - Compact action buttons (translate, copy, details).
 * - Arabic Date & Time below the bubble.
 */
@Composable
fun MessageBubble(
    message: MessageEntity,
    isBlurred: Boolean,
    onToggleReveal: () -> Unit,
    onBubbleClick: () -> Unit,
    onTranslateClick: () -> Unit
) {
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    val themeColors = VaultTheme.colors

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("message_bubble_${message.id}"),
        horizontalAlignment = Alignment.Start
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .border(
                    width = 1.dp,
                    color = if (message.isDeletedBySender) themeColors.deletedBubbleBorder else themeColors.incomingBubbleBorder,
                    shape = RoundedCornerShape(topStart = 4.dp, topEnd = 18.dp, bottomStart = 18.dp, bottomEnd = 18.dp)
                )
                .clickable { onBubbleClick() },
            colors = CardDefaults.cardColors(
                containerColor = if (message.isDeletedBySender) themeColors.deletedBubble else themeColors.incomingBubble
            ),
            shape = RoundedCornerShape(topStart = 4.dp, topEnd = 18.dp, bottomStart = 18.dp, bottomEnd = 18.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {

                // Top row: Sender Name and Deletion alert tag if deleted
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = message.senderName,
                        color = if (message.isDeletedBySender) DangerRed else themeColors.primary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )

                    if (message.isDeletedBySender) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(DangerRedContainer)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "محذوفة من الطرف الآخر 🔒",
                                color = DangerRed,
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Message Text with Privacy Blurring support
                if (isBlurred) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "••••••••••••••••••••••••••••••",
                            color = themeColors.textMuted,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = onToggleReveal,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Visibility,
                                contentDescription = "إظهار الرسالة مؤقتاً",
                                tint = themeColors.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = message.messageText,
                            color = themeColors.textPrimary,
                            fontSize = 14.sp,
                            lineHeight = 21.sp,
                            modifier = Modifier.weight(1f)
                        )

                        // If user revealed message manually, provide hide button
                        IconButton(
                            onClick = onToggleReveal,
                            modifier = Modifier.size(24.dp).padding(start = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.VisibilityOff,
                                contentDescription = "تمويه الرسالة",
                                tint = themeColors.textMuted,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                // Translated Text Block if exists
                if (!message.translatedText.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(themeColors.primaryContainer.copy(alpha = 0.4f))
                            .border(1.dp, themeColors.primary.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                            .padding(8.dp)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (message.translationLanguage == "ar") "الترجمة إلى العربية:" else "Translation to English:",
                                    color = themeColors.primary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "نسخ",
                                    color = themeColors.secondary,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(themeColors.surfaceElevated)
                                        .clickable {
                                            clipboardManager.setText(AnnotatedString(message.translatedText ?: ""))
                                            Toast.makeText(context, "تم نسخ الترجمة", Toast.LENGTH_SHORT).show()
                                        }
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = message.translatedText,
                                color = themeColors.textPrimary,
                                fontSize = 13.sp,
                                lineHeight = 19.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Compact, tidy action buttons row (Details, Translate, Copy)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left (RTL right): Details button
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable { onBubbleClick() }
                            .padding(horizontal = 6.dp, vertical = 3.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "تفاصيل الرسالة",
                            tint = themeColors.secondary,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "تفاصيل",
                            color = themeColors.secondary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    // Right (RTL left): Translate and Copy buttons
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Translate Button
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(themeColors.surfaceElevated)
                                .clickable { onTranslateClick() }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Translate,
                                    contentDescription = "ترجمة",
                                    tint = themeColors.primary,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = if (message.translatedText == null) "ترجمة" else "إعادة ترجمة",
                                    color = themeColors.primary,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        // Copy Button
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(themeColors.surfaceElevated)
                                .clickable {
                                    clipboardManager.setText(AnnotatedString(message.messageText))
                                    Toast.makeText(context, "تم نسخ النص", Toast.LENGTH_SHORT).show()
                                }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = "نسخ",
                                    tint = themeColors.textSecondary,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "نسخ",
                                    color = themeColors.textSecondary,
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        // Rich Date & Time under message bubble
        Row(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(top = 3.dp, start = 6.dp, end = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = DateTimeUtils.formatMessageBubbleTime(message.timestamp),
                color = themeColors.textMuted,
                fontSize = 10.sp
            )
            if (message.isDeletedBySender && message.deletedDetectedAt != null) {
                Text(
                    text = "رُصد الحذف: ${DateTimeUtils.formatShortTime(message.deletedDetectedAt)}",
                    color = DangerRed,
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun MessageDetailsDialog(
    message: MessageEntity,
    packageName: String,
    isClonedApp: Boolean,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val themeColors = VaultTheme.colors
    val appLabel = remember(packageName, isClonedApp) {
        AppIconProvider.getAppLabel(context, packageName, isClonedApp)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = themeColors.surface,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AppSourceIcon(
                    packageName = packageName,
                    isClonedApp = message.isFromClonedApp || isClonedApp,
                    size = 26.dp
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "تفاصيل الرسالة وتوثيقها",
                    color = themeColors.textPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                DetailItem(
                    label = "التطبيق المصدر:",
                    value = "$appLabel ($packageName)"
                )

                DetailItem(
                    label = "حالة الاستنساخ (App Twin):",
                    value = if (message.isFromClonedApp || isClonedApp) "تطبيق مستنسخ (HONOR App Twin / Multi-App)" else "تطبيق أصلي أساسي",
                    valueColor = if (message.isFromClonedApp || isClonedApp) CyanAccent else themeColors.textSecondary
                )

                DetailItem(
                    label = "اسم المرسل:",
                    value = message.senderName
                )

                DetailItem(
                    label = "تاريخ ووقت وصول الإشعار:",
                    value = DateTimeUtils.formatFullDetailedDateTime(message.timestamp)
                )

                if (message.isDeletedBySender) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(DangerRedContainer)
                            .border(1.dp, DangerRed.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                            .padding(10.dp)
                    ) {
                        Column {
                            Text(
                                text = "⚠️ تم رصد قيام الطرف الآخر بحذف هذه الرسالة!",
                                color = DangerRed,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            if (message.deletedDetectedAt != null && message.deletedDetectedAt > 0) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "توقيت رصد الحذف: ${DateTimeUtils.formatFullDetailedDateTime(message.deletedDetectedAt)}",
                                    color = themeColors.textPrimary,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                } else {
                    DetailItem(
                        label = "حالة الحذف:",
                        value = "رسالة أصلية محفوظة (لم يتم حذفها)",
                        valueColor = themeColors.primary
                    )
                }

                Column {
                    Text("النص الأصلي المحفوظ:", color = themeColors.textMuted, fontSize = 11.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(themeColors.surfaceElevated)
                            .border(1.dp, themeColors.border, RoundedCornerShape(8.dp))
                            .padding(10.dp)
                    ) {
                        Text(
                            text = message.messageText,
                            color = themeColors.textPrimary,
                            fontSize = 13.sp
                        )
                    }
                }

                if (message.contentHash.isNotBlank()) {
                    DetailItem(
                        label = "بصمة التحقق المشفرة (Hash):",
                        value = message.contentHash
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val detailsReport = """
                        --- تقرير توثيق الرسالة المحفوظة ---
                        المرسل: ${message.senderName}
                        التطبيق المصدر: $appLabel ($packageName)
                        مستنسخ: ${if (message.isFromClonedApp || isClonedApp) "نعم (HONOR App Twin)" else "لا"}
                        وقت الاستلام: ${DateTimeUtils.formatFullDetailedDateTime(message.timestamp)}
                        حالة الحذف: ${if (message.isDeletedBySender) "تم حذفها من الطرف الآخر (توقيت الرصد: ${DateTimeUtils.formatFullDetailedDateTime(message.deletedDetectedAt ?: 0)})" else "غير محذوفة"}
                        النص المحفوظ: ${message.messageText}
                        بصمة التحقق: ${message.contentHash}
                    """.trimIndent()
                    clipboardManager.setText(AnnotatedString(detailsReport))
                    Toast.makeText(context, "تم نسخ تقرير الرسالة بالكامل", Toast.LENGTH_SHORT).show()
                },
                colors = ButtonDefaults.buttonColors(containerColor = themeColors.primaryContainer, contentColor = themeColors.primary)
            ) {
                Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("نسخ التقرير")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إغلاق", color = themeColors.textSecondary)
            }
        }
    )
}

@Composable
private fun DetailItem(
    label: String,
    value: String,
    valueColor: Color = VaultTheme.colors.textPrimary
) {
    val themeColors = VaultTheme.colors
    Column {
        Text(text = label, color = themeColors.textMuted, fontSize = 11.sp)
        Text(text = value, color = valueColor, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}
