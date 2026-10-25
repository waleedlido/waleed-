package com.example.ui.chat

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.Warning
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
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ConversationEntity
import com.example.data.model.MessageEntity
import com.example.data.repository.MessageVaultRepository
import com.example.ml.TranslationManager
import com.example.ml.TranslationResult
import com.example.ui.components.AppAvatarIcon
import com.example.ui.components.AppSourceBadge
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
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Global cache to preserve scroll position for each conversation across back/forward navigation.
 */
object ChatScrollStateCache {
    private val scrollPositions = mutableMapOf<Long, Pair<Int, Int>>() // conversationId -> (index, offset)

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

/**
 * Fast and smooth scrolling helper:
 * For long distances (>10 items), jumps close to the target instantly
 * then animates the short remaining distance to eliminate frame lag and scrolling freezes.
 */
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
    val listState = rememberLazyListState()

    var showMenu by remember { mutableStateOf(false) }

    // Intercept back button: if menu is open close it first, otherwise onBack() returns to Home
    BackHandler {
        if (showMenu) {
            showMenu = false
        } else {
            onBack()
        }
    }

    var conversation by remember { mutableStateOf<ConversationEntity?>(null) }
    LaunchedEffect(conversationId) {
        conversation = repository.getConversationById(conversationId)
    }

    val messages by repository.getMessagesForConversation(conversationId)
        .collectAsState(initial = emptyList())

    // Track scroll restoration: restore saved position if exists, otherwise open at latest message
    var hasRestoredScroll by remember(conversationId) { mutableStateOf(false) }

    LaunchedEffect(messages.isNotEmpty(), conversationId) {
        if (messages.isNotEmpty() && !hasRestoredScroll) {
            val saved = ChatScrollStateCache.getPosition(conversationId)
            if (saved != null) {
                val (savedIndex, savedOffset) = saved
                val safeIndex = savedIndex.coerceIn(0, messages.size - 1)
                listState.scrollToItem(safeIndex, savedOffset)
            } else {
                // Open directly at the newest message (bottom)
                listState.scrollToItem(messages.size - 1)
            }
            hasRestoredScroll = true
        }
    }

    // Preserve scroll position when leaving this conversation screen
    DisposableEffect(conversationId) {
        onDispose {
            if (messages.isNotEmpty()) {
                ChatScrollStateCache.savePosition(
                    conversationId,
                    listState.firstVisibleItemIndex,
                    listState.firstVisibleItemScrollOffset
                )
            }
        }
    }

    // Determine visibility of floating navigation arrows in real-time
    val showScrollToTop by remember {
        derivedStateOf { listState.canScrollBackward }
    }

    val showScrollToBottom by remember {
        derivedStateOf { listState.canScrollForward }
    }

    // Handle new incoming messages: if user is scrolled up reading old messages,
    // do NOT force scroll down. Show a floating badge instead.
    var previousMessageCount by remember(conversationId) { mutableStateOf(messages.size) }
    var hasNewMessagePill by remember(conversationId) { mutableStateOf(false) }

    LaunchedEffect(messages.size) {
        if (previousMessageCount > 0 && messages.size > previousMessageCount) {
            if (listState.canScrollForward) {
                // User is scrolled back reading past history: alert them gently
                hasNewMessagePill = true
            } else {
                // User is already at the bottom: follow stream smoothly
                listState.fastScrollTo(messages.size - 1)
            }
        }
        previousMessageCount = messages.size
    }

    // Dismiss "New Messages" pill if the user scrolls down to the bottom
    LaunchedEffect(listState.canScrollForward) {
        if (!listState.canScrollForward) {
            hasNewMessagePill = false
        }
    }

    var isTranslatingAll by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = DeepObsidian,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = SurfaceDark,
                    titleContentColor = TextPrimaryDark
                ),
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("chat_back_button")) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "رجوع",
                            tint = TextPrimaryDark
                        )
                    }
                },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        conversation?.let { conv ->
                            AppAvatarIcon(
                                packageName = conv.packageName,
                                chatTitle = conv.chatTitle,
                                isClonedApp = conv.isClonedApp,
                                modifier = Modifier.size(38.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                        }
                        Column {
                            Text(
                                text = conversation?.chatTitle ?: "المحادثة",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimaryDark,
                                maxLines = 1
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                conversation?.let { conv ->
                                    AppSourceBadge(
                                        packageName = conv.packageName,
                                        isClonedApp = conv.isClonedApp
                                    )
                                    if (conv.deletedMessagesCount > 0) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "• ${conv.deletedMessagesCount} محذوفة",
                                            fontSize = 10.sp,
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
                        Icon(imageVector = Icons.Default.MoreVert, contentDescription = "خيارات", tint = TextSecondaryDark)
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                        modifier = Modifier.background(SurfaceDark)
                    ) {
                        DropdownMenuItem(
                            text = { Text("ترجمة جميع الرسائل الظاهرة", color = TextPrimaryDark) },
                            leadingIcon = { Icon(Icons.Default.Translate, contentDescription = null, tint = NeonGreen) },
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
                            text = { Text("حذف المحادثة", color = DangerRed) },
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
            // LazyColumn ordered chronologically (oldest at top index 0, newest at bottom index lastIndex)
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("chat_messages_list"),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(
                    items = messages,
                    key = { it.id }
                ) { message ->
                    MessageBubble(
                        message = message,
                        packageName = conversation?.packageName ?: "",
                        isClonedApp = conversation?.isClonedApp ?: false,
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

            // Floating Navigation Arrows (Top & Bottom)
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
                            .size(48.dp)
                            .testTag("scroll_to_top_button"),
                        shape = CircleShape,
                        containerColor = SurfaceElevatedDark,
                        contentColor = NeonGreen,
                        elevation = FloatingActionButtonDefaults.elevation(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowUpward,
                            contentDescription = "الانتقال لأول رسالة قديمة",
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                // Arrow DOWN: Scroll to newest message (index lastIndex)
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
                            .size(48.dp)
                            .testTag("scroll_to_bottom_button"),
                        shape = CircleShape,
                        containerColor = DarkEmerald,
                        contentColor = NeonGreen,
                        elevation = FloatingActionButtonDefaults.elevation(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowDownward,
                            contentDescription = "الانتقال لآخر رسالة حديثة",
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            // New Message Floating Pill Banner (shown when user is browsing older messages and new ones arrive)
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
                    color = DarkEmerald,
                    tonalElevation = 8.dp,
                    shadowElevation = 8.dp,
                    border = BorderStroke(1.dp, NeonGreen),
                    modifier = Modifier.testTag("new_message_badge")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowDownward,
                            contentDescription = null,
                            tint = NeonGreen,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "رسائل جديدة بالأسفل",
                            color = Color.White,
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
                        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(20.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(color = NeonGreen, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(14.dp))
                            Text("جاري ترجمة الرسائل بالذكاء الاصطناعي...", color = TextPrimaryDark, fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MessageBubble(
    message: MessageEntity,
    packageName: String,
    isClonedApp: Boolean,
    onTranslateClick: () -> Unit
) {
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    var isTranslatingLocal by remember { mutableStateOf(false) }

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
                    color = if (message.isDeletedBySender) DangerRed.copy(alpha = 0.6f) else BorderDark,
                    shape = RoundedCornerShape(topStart = 4.dp, topEnd = 18.dp, bottomStart = 18.dp, bottomEnd = 18.dp)
                ),
            colors = CardDefaults.cardColors(
                containerColor = if (message.isDeletedBySender) Color(0xFF180B0D) else SurfaceDark
            ),
            shape = RoundedCornerShape(topStart = 4.dp, topEnd = 18.dp, bottomStart = 18.dp, bottomEnd = 18.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {

                // Sender Name, App Badge & Full Timestamp
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f, fill = false)
                    ) {
                        Text(
                            text = message.senderName,
                            color = if (message.isDeletedBySender) DangerRed else NeonGreen,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        AppSourceBadge(
                            packageName = packageName,
                            isClonedApp = message.isFromClonedApp || isClonedApp
                        )
                    }

                    Text(
                        text = formatDetailedDate(message.timestamp),
                        color = TextMutedDark,
                        fontSize = 10.sp
                    )
                }

                // Deleted by Sender Alert Banner
                if (message.isDeletedBySender) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(DangerRedContainer)
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = DangerRed,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "قام الطرف الآخر بحذف هذه الرسالة • النص الأصلي محفوظ ومحمي 🔒",
                                color = DangerRed,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Original Message Text (Complete text, never overwritten)
                Text(
                    text = message.messageText,
                    color = TextPrimaryDark,
                    fontSize = 14.sp,
                    lineHeight = 22.sp
                )

                // Translated Text Block if exists
                if (!message.translatedText.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(DarkEmerald.copy(alpha = 0.6f))
                            .border(1.dp, NeonGreen.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                            .padding(10.dp)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (message.translationLanguage == "ar") "الترجمة إلى العربية:" else "Translation to English:",
                                    color = NeonGreen,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "نسخ الترجمة",
                                    color = CyanAccent,
                                    fontSize = 11.sp,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(SurfaceElevatedDark)
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
                                color = Color.White,
                                fontSize = 13.sp,
                                lineHeight = 19.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Bottom Actions Row (Translate button & Copy button)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Translate Button
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(SurfaceElevatedDark)
                            .clickable {
                                if (!isTranslatingLocal) {
                                    isTranslatingLocal = true
                                    onTranslateClick()
                                    isTranslatingLocal = false
                                }
                            }
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Translate,
                                contentDescription = "ترجمة",
                                tint = NeonGreen,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (message.translatedText == null) "ترجمة" else "إعادة الترجمة",
                                color = NeonGreen,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Copy Button
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(SurfaceElevatedDark)
                            .clickable {
                                clipboardManager.setText(AnnotatedString(message.messageText))
                                Toast.makeText(context, "تم نسخ النص الأصلي", Toast.LENGTH_SHORT).show()
                            }
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = "نسخ",
                            color = TextSecondaryDark,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }
    }
}

fun formatDetailedDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("d MMM، h:mm a", Locale("ar"))
    return sdf.format(Date(timestamp))
}
