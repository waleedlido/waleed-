package com.example.service

import android.app.Notification
import android.content.ComponentName
import android.content.Context
import android.os.Build
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.example.MessageVaultApp
import com.example.data.repository.ExtractedNotificationMessage
import com.example.util.NotificationTextFilter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class NotificationVaultService : NotificationListenerService() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // In-memory LRU debouncing cache (prevents duplicate processing on rapid OS notification updates)
    private val recentNotificationCache = object : LinkedHashMap<String, Long>(64, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Long>?): Boolean {
            return size > 150
        }
    }

    // Rebind throttling: prevents tight rebind loops on aggressive OEM task managers like HONOR MagicOS
    private var lastRebindAttemptTime: Long = 0L
    private var rebindAttempts: Int = 0

    override fun onListenerConnected() {
        super.onListenerConnected()
        _isServiceConnected.value = true
        _lastError.value = null
        rebindAttempts = 0
        lastRebindAttemptTime = 0L
        Log.d(TAG, "NotificationVaultService connected successfully")
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        _isServiceConnected.value = false
        Log.d(TAG, "NotificationVaultService disconnected")

        val now = System.currentTimeMillis()
        if (now - lastRebindAttemptTime < REBIND_COOLDOWN_MS || rebindAttempts >= MAX_REBIND_ATTEMPTS) {
            Log.d(TAG, "Rebind throttled to preserve battery")
            return
        }

        lastRebindAttemptTime = now
        rebindAttempts++

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            try {
                requestRebind(ComponentName(this, NotificationVaultService::class.java))
            } catch (e: Exception) {
                Log.w(TAG, "Rebind attempt failed: ${e.message}")
            }
        }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null) return

        _diagnostics.update { it.copy(receivedCount = it.receivedCount + 1) }

        val packageName = sbn.packageName ?: return

        // 1. Filter for supported messaging and social apps
        if (!isSupportedApp(packageName)) {
            _diagnostics.update { it.copy(ignoredCount = it.ignoredCount + 1) }
            return
        }

        val notification = sbn.notification ?: return
        val extras = notification.extras ?: return

        // 2. Skip ongoing background processes if not a real chat notification
        val isGroupSummary = (notification.flags and Notification.FLAG_GROUP_SUMMARY) != 0
        val isOngoing = (notification.flags and Notification.FLAG_ONGOING_EVENT) != 0
        if (isOngoing && !isMessagingPackage(packageName)) {
            _diagnostics.update { it.copy(ignoredCount = it.ignoredCount + 1) }
            return
        }

        // 3. High-efficiency in-memory signature debounce (drops duplicate OS events within 2.5s)
        val notifKey = "${packageName}|${sbn.id}|${sbn.tag}|${notification.`when`}|${sbn.postTime}"
        val now = System.currentTimeMillis()
        val isDuplicate = synchronized(recentNotificationCache) {
            val lastSeen = recentNotificationCache[notifKey]
            if (lastSeen != null && (now - lastSeen) < 2500L) {
                true
            } else {
                recentNotificationCache[notifKey] = now
                false
            }
        }
        if (isDuplicate) {
            _diagnostics.update { it.copy(ignoredCount = it.ignoredCount + 1) }
            return
        }

        // 4. Cloned app detection (HONOR App Twin, Parallel Space, secondary user handles)
        val userHandleId = extractUserHandleId(sbn)
        val isClonedApp = userHandleId != 0 || isClonedPackageName(packageName) || isHonorAppTwin(sbn)
        val appDisplayName = resolveAppDisplayName(packageName, isClonedApp)

        // 5. Extract metadata
        val chatTitle = extractChatTitle(extras)
        val fallbackSender = extractSenderName(extras, chatTitle)

        val notificationWhen = notification.`when`
        val timestamp = when {
            sbn.postTime > 0 -> sbn.postTime
            notificationWhen > 0 -> notificationWhen
            else -> System.currentTimeMillis()
        }

        if (chatTitle.isBlank()) {
            _diagnostics.update { it.copy(ignoredCount = it.ignoredCount + 1) }
            return
        }

        // 6. Multi-message extraction from Notification.EXTRA_MESSAGES (MessagingStyle) & InboxStyle
        val extractedMessages = extractAllMessages(
            extras = extras,
            fallbackChatTitle = chatTitle,
            fallbackSender = fallbackSender,
            fallbackTimestamp = timestamp
        )

        // 7. Check if notification only contains a summary or counter (e.g. "رسالتان جديدتان")
        if (extractedMessages.isEmpty()) {
            // Check if there is an explicit deletion notice (e.g. "تم حذف هذه الرسالة")
            val candidateText = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()?.trim() ?: ""
            if (isDeletionNotice(candidateText)) {
                processMessageBatch(
                    packageName = packageName,
                    appDisplayName = appDisplayName,
                    chatTitle = chatTitle,
                    messages = listOf(
                        ExtractedNotificationMessage(
                            senderName = fallbackSender,
                            text = candidateText,
                            timestamp = timestamp
                        )
                    ),
                    isClonedApp = isClonedApp,
                    userHandleId = userHandleId,
                    timestamp = timestamp
                )
                return
            }

            // If it's a summary counter like "رسالتان جديدتان" or "2 new messages", drop without fabricating content
            if (NotificationTextFilter.isSummaryOrCounterText(candidateText) || isGroupSummary) {
                _diagnostics.update { it.copy(ignoredCount = it.ignoredCount + 1) }
                return
            }

            // Masked notification where content is not provided by WhatsApp
            val unavailableMsg = ExtractedNotificationMessage(
                senderName = fallbackSender,
                text = NotificationTextFilter.CONTENT_NOT_AVAILABLE,
                timestamp = timestamp
            )
            processMessageBatch(
                packageName = packageName,
                appDisplayName = appDisplayName,
                chatTitle = chatTitle,
                messages = listOf(unavailableMsg),
                isClonedApp = isClonedApp,
                userHandleId = userHandleId,
                timestamp = timestamp
            )
            return
        }

        // 8. Process extracted genuine messages
        processMessageBatch(
            packageName = packageName,
            appDisplayName = appDisplayName,
            chatTitle = chatTitle,
            messages = extractedMessages,
            isClonedApp = isClonedApp,
            userHandleId = userHandleId,
            timestamp = timestamp
        )
    }

    private fun processMessageBatch(
        packageName: String,
        appDisplayName: String,
        chatTitle: String,
        messages: List<ExtractedNotificationMessage>,
        isClonedApp: Boolean,
        userHandleId: Int,
        timestamp: Long
    ) {
        serviceScope.launch {
            try {
                val repository = MessageVaultApp.get().repository
                val result = repository.saveIncomingBatchNotification(
                    packageName = packageName,
                    appDisplayName = appDisplayName,
                    chatTitle = chatTitle,
                    messages = messages,
                    isClonedApp = isClonedApp,
                    userHandleId = userHandleId
                )

                when (result) {
                    is com.example.data.repository.SaveResult.Saved -> {
                        _diagnostics.update { it.copy(processedCount = it.processedCount + 1) }

                        val latestMsg = messages.lastOrNull()
                        val latestText = latestMsg?.text ?: ""
                        val latestSender = latestMsg?.senderName ?: chatTitle

                        _lastReceivedEvent.value = ServiceEvent(
                            packageName = packageName,
                            appDisplayName = appDisplayName,
                            chatTitle = chatTitle,
                            senderName = latestSender,
                            messageSnippet = latestText.take(60),
                            timestamp = timestamp,
                            isClonedApp = isClonedApp
                        )

                        _lastSavedMessage.value = "${chatTitle}: ${latestText.take(40)}"

                        // Privacy: Log count and package without sensitive message text
                        Log.d(TAG, "Notification batch saved: pkg=$packageName, count=${messages.size}")

                        // Dispatch local notification if enabled
                        com.example.util.AppNotificationManager.showNewMessageNotification(
                            context = applicationContext,
                            conversationId = result.conversationId,
                            senderName = latestSender,
                            messageText = latestText,
                            packageName = packageName,
                            isClonedApp = isClonedApp,
                            timestamp = timestamp
                        )
                    }
                    is com.example.data.repository.SaveResult.MarkedDeleted -> {
                        _diagnostics.update { it.copy(processedCount = it.processedCount + 1) }

                        Log.d(TAG, "Notification deletion marked: pkg=$packageName")

                        com.example.util.AppNotificationManager.showDeletedMessageNotification(
                            context = applicationContext,
                            conversationId = result.conversationId,
                            senderName = chatTitle,
                            messageText = result.deletedMessageText,
                            packageName = packageName,
                            isClonedApp = isClonedApp,
                            deletedDetectedAt = timestamp
                        )
                    }
                    is com.example.data.repository.SaveResult.Duplicate -> {
                        _diagnostics.update { it.copy(ignoredCount = it.ignoredCount + 1) }
                    }
                    is com.example.data.repository.SaveResult.Ignored -> {
                        _diagnostics.update { it.copy(ignoredCount = it.ignoredCount + 1) }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error saving notification batch: ${e.message}")
                _diagnostics.update { it.copy(errorsCount = it.errorsCount + 1) }
                _lastError.value = "خطأ أثناء معالجة الإشعار"
            }
        }
    }

    private fun extractAllMessages(
        extras: android.os.Bundle,
        fallbackChatTitle: String,
        fallbackSender: String,
        fallbackTimestamp: Long
    ): List<ExtractedNotificationMessage> {
        val extracted = mutableListOf<ExtractedNotificationMessage>()

        // 1. Primary Strategy: Notification.EXTRA_MESSAGES (MessagingStyle) on API 24+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            try {
                val rawMessages = extras.getParcelableArray(Notification.EXTRA_MESSAGES)
                val parsedList: List<Notification.MessagingStyle.Message>? = when {
                    rawMessages != null && rawMessages.isNotEmpty() -> {
                        Notification.MessagingStyle.Message.getMessagesFromBundleArray(rawMessages)
                    }
                    else -> {
                        val arrayList = extras.getParcelableArrayList<android.os.Bundle>(Notification.EXTRA_MESSAGES)
                        if (arrayList != null && arrayList.isNotEmpty()) {
                            Notification.MessagingStyle.Message.getMessagesFromBundleArray(arrayList.toTypedArray())
                        } else null
                    }
                }

                if (!parsedList.isNullOrEmpty()) {
                    for (msg in parsedList) {
                        val rawText = msg.text?.toString()?.trim() ?: continue
                        if (rawText.isBlank() || NotificationTextFilter.isSummaryOrCounterText(rawText)) {
                            continue
                        }

                        val sender = when {
                            Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && msg.senderPerson != null -> {
                                msg.senderPerson?.name?.toString()?.trim().takeUnless { it.isNullOrBlank() }
                            }
                            else -> {
                                @Suppress("DEPRECATION")
                                msg.sender?.toString()?.trim().takeUnless { it.isNullOrBlank() }
                            }
                        } ?: fallbackSender

                        val time = if (msg.timestamp > 0) msg.timestamp else fallbackTimestamp
                        extracted.add(ExtractedNotificationMessage(senderName = sender, text = rawText, timestamp = time))
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "MessagingStyle extraction fallback: ${e.message}")
            }
        }

        if (extracted.isNotEmpty()) {
            return extracted
        }

        // 2. Secondary Strategy: Notification.EXTRA_TEXT_LINES (InboxStyle)
        val textLines = extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)
        if (textLines != null && textLines.isNotEmpty()) {
            for ((idx, lineSeq) in textLines.withIndex()) {
                val line = lineSeq?.toString()?.trim() ?: continue
                if (line.isBlank() || NotificationTextFilter.isSummaryOrCounterText(line)) continue

                val colonIdx = line.indexOf(':')
                val (sender, text) = if (colonIdx in 1 until line.length - 1) {
                    val s = line.substring(0, colonIdx).trim()
                    val t = line.substring(colonIdx + 1).trim()
                    Pair(s.ifBlank { fallbackSender }, t)
                } else {
                    Pair(fallbackSender, line)
                }

                if (text.isNotBlank() && !NotificationTextFilter.isSummaryOrCounterText(text)) {
                    extracted.add(
                        ExtractedNotificationMessage(
                            senderName = sender,
                            text = text,
                            timestamp = fallbackTimestamp + idx
                        )
                    )
                }
            }
        }

        if (extracted.isNotEmpty()) {
            return extracted
        }

        // 3. Tertiary Strategy: BigText or Regular Text
        val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()?.trim()
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()?.trim()

        val candidate = when {
            !bigText.isNullOrBlank() && !NotificationTextFilter.isSummaryOrCounterText(bigText) -> bigText
            !text.isNullOrBlank() && !NotificationTextFilter.isSummaryOrCounterText(text) -> text
            else -> null
        }

        if (candidate != null) {
            extracted.add(
                ExtractedNotificationMessage(
                    senderName = fallbackSender,
                    text = candidate,
                    timestamp = fallbackTimestamp
                )
            )
        }

        return extracted
    }

    private fun isDeletionNotice(text: String): Boolean {
        val lower = text.lowercase().trim()
        return lower.contains("تم حذف هذه الرسالة") ||
                lower.contains("this message was deleted") ||
                lower.contains("you deleted this message") ||
                lower.contains("قام بحذف هذه الرسالة") ||
                lower.contains("deleted this message") ||
                lower.contains("message deleted")
    }

    private fun extractChatTitle(extras: android.os.Bundle): String {
        val conversationTitle = extras.getCharSequence(Notification.EXTRA_CONVERSATION_TITLE)?.toString()
        if (!conversationTitle.isNullOrBlank()) return conversationTitle

        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()
        if (!title.isNullOrBlank()) return title

        return ""
    }

    private fun extractSenderName(extras: android.os.Bundle, fallbackTitle: String): String {
        val titleBig = extras.getCharSequence(Notification.EXTRA_TITLE_BIG)?.toString()
        if (!titleBig.isNullOrBlank()) return titleBig

        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()
        if (!title.isNullOrBlank()) return title

        return fallbackTitle
    }

    private fun extractUserHandleId(sbn: StatusBarNotification): Int {
        return try {
            val user = sbn.user
            val stringRep = user.toString()
            val match = Regex("\\d+").find(stringRep)
            val extracted = match?.value?.toIntOrNull() ?: 0
            if (extracted != 0) return extracted

            val keyParts = sbn.key?.split("|")
            if (!keyParts.isNullOrEmpty()) {
                val keyUserId = keyParts[0].toIntOrNull()
                if (keyUserId != null && keyUserId != 0) {
                    return keyUserId
                }
            }
            0
        } catch (e: Exception) {
            0
        }
    }

    private fun isHonorAppTwin(sbn: StatusBarNotification): Boolean {
        val key = sbn.key?.lowercase() ?: ""
        val tag = sbn.tag?.lowercase() ?: ""
        return key.contains("clone") || key.contains("twin") || key.contains("dual") ||
                tag.contains("clone") || tag.contains("twin") ||
                extractUserHandleId(sbn) > 0
    }

    private fun isClonedPackageName(packageName: String): Boolean {
        val lower = packageName.lowercase()
        return lower.contains("clone") ||
                lower.contains("parallel") ||
                lower.contains("dual") ||
                lower.contains("twin")
    }

    private fun isMessagingPackage(pkg: String): Boolean {
        return pkg.startsWith("com.whatsapp") ||
                pkg.startsWith("org.telegram") ||
                pkg.startsWith("org.thoughtcrime.securesms") ||
                pkg == "com.facebook.orca" ||
                pkg == "com.google.android.apps.messaging" ||
                pkg == "com.instagram.android"
    }

    private fun isSupportedApp(pkg: String): Boolean {
        val lower = pkg.lowercase()
        return lower.startsWith("com.whatsapp") ||
                lower.startsWith("org.telegram") ||
                lower.startsWith("org.thoughtcrime.securesms") ||
                lower == "com.facebook.orca" ||
                lower == "com.instagram.android" ||
                lower == "com.google.android.apps.messaging" ||
                lower == "com.android.mms" ||
                lower.contains("viber") ||
                lower.contains("line") ||
                lower.contains("signal")
    }

    private fun resolveAppDisplayName(pkg: String, isCloned: Boolean): String {
        val baseName = when {
            pkg == "com.whatsapp" -> "واتساب"
            pkg == "com.whatsapp.w4b" -> "واتساب للأعمال"
            pkg.startsWith("org.telegram") -> "تيليجرام"
            pkg.startsWith("org.thoughtcrime.securesms") -> "سيجنال"
            pkg == "com.facebook.orca" -> "ماسنجر"
            pkg == "com.instagram.android" -> "إنستغرام"
            pkg == "com.google.android.apps.messaging" || pkg == "com.android.mms" -> "رسائل SMS"
            else -> "تطبيق محادثة"
        }
        return if (isCloned) "$baseName (مستنسخ / App Twin)" else baseName
    }

    companion object {
        private const val TAG = "NotificationVault"
        private const val REBIND_COOLDOWN_MS = 60_000L
        private const val MAX_REBIND_ATTEMPTS = 3

        private val _isServiceConnected = MutableStateFlow(false)
        val isServiceConnected: StateFlow<Boolean> = _isServiceConnected.asStateFlow()

        private val _lastReceivedEvent = MutableStateFlow<ServiceEvent?>(null)
        val lastReceivedEvent: StateFlow<ServiceEvent?> = _lastReceivedEvent.asStateFlow()

        private val _lastSavedMessage = MutableStateFlow<String?>(null)
        val lastSavedMessage: StateFlow<String?> = _lastSavedMessage.asStateFlow()

        private val _lastError = MutableStateFlow<String?>(null)
        val lastError: StateFlow<String?> = _lastError.asStateFlow()

        private val _diagnostics = MutableStateFlow(ServiceDiagnostics())
        val diagnostics: StateFlow<ServiceDiagnostics> = _diagnostics.asStateFlow()

        fun tryRebind(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                try {
                    NotificationListenerService.requestRebind(
                        ComponentName(context, NotificationVaultService::class.java)
                    )
                } catch (e: Exception) {
                    Log.w(TAG, "Manual rebind failed: ${e.message}")
                }
            }
        }
    }
}

data class ServiceEvent(
    val packageName: String,
    val appDisplayName: String,
    val chatTitle: String,
    val senderName: String,
    val messageSnippet: String,
    val timestamp: Long,
    val isClonedApp: Boolean
)

data class ServiceDiagnostics(
    val receivedCount: Long = 0L,
    val processedCount: Long = 0L,
    val ignoredCount: Long = 0L,
    val errorsCount: Long = 0L
)
