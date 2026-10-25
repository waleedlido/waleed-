package com.example.service

import android.app.Notification
import android.content.ComponentName
import android.content.Context
import android.os.Build
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.example.MessageVaultApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class NotificationVaultService : NotificationListenerService() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onListenerConnected() {
        super.onListenerConnected()
        _isServiceConnected.value = true
        _lastError.value = null
        Log.d(TAG, "NotificationVaultService connected successfully")
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        _isServiceConnected.value = false
        Log.d(TAG, "NotificationVaultService disconnected")
        // Attempt clean rebind if system permits (API 24+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            try {
                requestRebind(ComponentName(this, NotificationVaultService::class.java))
            } catch (e: Exception) {
                Log.w(TAG, "Rebind request failed: ${e.message}")
            }
        }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null) return

        val packageName = sbn.packageName ?: return

        // Filter for supported messaging and social apps
        if (!isSupportedApp(packageName)) {
            return
        }

        val notification = sbn.notification ?: return
        val extras = notification.extras ?: return

        // Skip ongoing or silent summary notifications without message text
        val isGroupSummary = (notification.flags and Notification.FLAG_GROUP_SUMMARY) != 0
        val isOngoing = (notification.flags and Notification.FLAG_ONGOING_EVENT) != 0
        if (isOngoing && !isMessagingPackage(packageName)) {
            return
        }

        // Determine if cloned / twin app
        val userHandleId = extractUserHandleId(sbn)
        val isClonedApp = userHandleId != 0 || isClonedPackageName(packageName)

        // Resolve app display name
        val appDisplayName = resolveAppDisplayName(packageName, isClonedApp)

        // Extract message fields
        val chatTitle = extractChatTitle(extras)
        val messageText = extractMessageText(extras)
        val senderName = extractSenderName(extras, chatTitle)
        val timestamp = if (sbn.postTime > 0) sbn.postTime else System.currentTimeMillis()

        if (chatTitle.isBlank() || messageText.isBlank()) {
            return
        }

        // Skip generic summary lines like "X new messages" if individual text isn't present
        if (isGroupSummary && messageText.matches(Regex("\\d+\\s+new messages?|\\d+\\s+رسائل جديدة", RegexOption.IGNORE_CASE))) {
            return
        }

        // Process message in database
        serviceScope.launch {
            try {
                val repository = MessageVaultApp.get().repository
                val result = repository.saveIncomingNotification(
                    packageName = packageName,
                    appDisplayName = appDisplayName,
                    chatTitle = chatTitle,
                    senderName = senderName,
                    messageText = messageText,
                    timestamp = timestamp,
                    isClonedApp = isClonedApp,
                    userHandleId = userHandleId
                )

                _lastReceivedEvent.value = ServiceEvent(
                    packageName = packageName,
                    appDisplayName = appDisplayName,
                    chatTitle = chatTitle,
                    senderName = senderName,
                    messageSnippet = messageText.take(60),
                    timestamp = timestamp,
                    isClonedApp = isClonedApp
                )

                _lastSavedMessage.value = "${chatTitle}: ${messageText.take(40)}"
                Log.d(TAG, "Notification processed: $chatTitle -> $messageText, result: $result")
            } catch (e: Exception) {
                Log.e(TAG, "Error saving notification", e)
                _lastError.value = "خطأ أثناء حفظ الإشعار: ${e.localizedMessage}"
            }
        }
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

    private fun extractMessageText(extras: android.os.Bundle): String {
        // Priority 1: MessagingStyle lines
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val messages = extras.getParcelableArray(Notification.EXTRA_MESSAGES)
            if (messages != null && messages.isNotEmpty()) {
                val lastMsg = messages.lastOrNull()
                if (lastMsg is android.os.Bundle) {
                    val text = lastMsg.getCharSequence("text")?.toString()
                    if (!text.isNullOrBlank()) return text
                }
            }
        }

        // Priority 2: BigText
        val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()
        if (!bigText.isNullOrBlank()) return bigText

        // Priority 3: Normal Text
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()
        if (!text.isNullOrBlank()) return text

        // Priority 4: Text lines from InboxStyle
        val textLines = extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)
        if (textLines != null && textLines.isNotEmpty()) {
            return textLines.last().toString()
        }

        return ""
    }

    private fun extractUserHandleId(sbn: StatusBarNotification): Int {
        return try {
            val user = sbn.user
            val stringRep = user.toString()
            val match = Regex("\\d+").find(stringRep)
            match?.value?.toIntOrNull() ?: 0
        } catch (e: Exception) {
            0
        }
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

        private val _isServiceConnected = MutableStateFlow(false)
        val isServiceConnected: StateFlow<Boolean> = _isServiceConnected.asStateFlow()

        private val _lastReceivedEvent = MutableStateFlow<ServiceEvent?>(null)
        val lastReceivedEvent: StateFlow<ServiceEvent?> = _lastReceivedEvent.asStateFlow()

        private val _lastSavedMessage = MutableStateFlow<String?>(null)
        val lastSavedMessage: StateFlow<String?> = _lastSavedMessage.asStateFlow()

        private val _lastError = MutableStateFlow<String?>(null)
        val lastError: StateFlow<String?> = _lastError.asStateFlow()

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
