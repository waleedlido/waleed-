package com.example.data.repository

import com.example.MessageVaultApp
import com.example.data.database.MessageVaultDatabase
import com.example.data.model.ConversationEntity
import com.example.data.model.MessageEntity
import com.example.util.NotificationTextFilter
import com.example.util.PreferenceManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest

class MessageVaultRepository(private val database: MessageVaultDatabase) {

    private val conversationDao = database.conversationDao()
    private val messageDao = database.messageDao()

    val allConversations: Flow<List<ConversationEntity>> = conversationDao.getAllConversationsFlow()
    val trashConversations: Flow<List<ConversationEntity>> = conversationDao.getTrashConversationsFlow()
    val trashCount: Flow<Int> = conversationDao.getTrashCountFlow()

    fun searchConversations(query: String): Flow<List<ConversationEntity>> =
        conversationDao.searchConversationsFlow(query)

    fun getMessagesForConversation(conversationId: Long): Flow<List<MessageEntity>> =
        messageDao.getMessagesForConversationFlow(conversationId)

    suspend fun getConversationById(id: Long): ConversationEntity? = withContext(Dispatchers.IO) {
        conversationDao.getConversationById(id)
    }

    suspend fun saveIncomingNotification(
        packageName: String,
        appDisplayName: String,
        chatTitle: String,
        senderName: String,
        messageText: String,
        timestamp: Long,
        isClonedApp: Boolean,
        userHandleId: Int
    ): SaveResult = withContext(Dispatchers.IO) {
        val trimmedText = messageText.trim()
        if (trimmedText.isEmpty() || chatTitle.isBlank()) {
            return@withContext SaveResult.Ignored("Empty message or title")
        }

        // Check if this notification indicates a deleted message
        if (isDeletionIndicator(trimmedText)) {
            val conversation = conversationDao.findConversation(packageName, chatTitle, isClonedApp)
            if (conversation != null) {
                val latestMessage = messageDao.getLatestNonDeletedMessage(conversation.id)
                if (latestMessage != null) {
                    messageDao.markMessageDeleted(latestMessage.id, timestamp)
                    val updatedConv = conversation.copy(
                        deletedMessagesCount = conversation.deletedMessagesCount + 1
                    )
                    conversationDao.updateConversation(updatedConv)
                    return@withContext SaveResult.MarkedDeleted(
                        messageId = latestMessage.id,
                        conversationId = conversation.id,
                        deletedMessageText = latestMessage.messageText
                    )
                }
            }
            return@withContext SaveResult.Ignored("Deletion notice could not be matched")
        }

        // Prevent saving summary or counter phrases (e.g. "رسالتان جديدتان" or "2 new messages")
        if (NotificationTextFilter.isSummaryOrCounterText(trimmedText)) {
            return@withContext SaveResult.Ignored("Summary counter not saved as message")
        }

        val singleMsg = ExtractedNotificationMessage(
            senderName = senderName.ifBlank { chatTitle },
            text = trimmedText,
            timestamp = timestamp
        )

        saveIncomingBatchNotification(
            packageName = packageName,
            appDisplayName = appDisplayName,
            chatTitle = chatTitle,
            messages = listOf(singleMsg),
            isClonedApp = isClonedApp,
            userHandleId = userHandleId
        )
    }

    suspend fun saveIncomingBatchNotification(
        packageName: String,
        appDisplayName: String,
        chatTitle: String,
        messages: List<ExtractedNotificationMessage>,
        isClonedApp: Boolean,
        userHandleId: Int
    ): SaveResult = withContext(Dispatchers.IO) {
        if (chatTitle.isBlank()) {
            return@withContext SaveResult.Ignored("Empty chat title")
        }

        // Filter valid messages: eliminate counters like "رسالتان جديدتان", "2 new messages", etc.
        val validMessages = messages.filter { NotificationTextFilter.isValidMessageContent(it.text) }

        if (validMessages.isEmpty()) {
            // Check if any message is a deletion indicator
            val deletionMsg = messages.firstOrNull { isDeletionIndicator(it.text) }
            if (deletionMsg != null) {
                val conversation = conversationDao.findConversation(packageName, chatTitle, isClonedApp)
                if (conversation != null) {
                    val latestMessage = messageDao.getLatestNonDeletedMessage(conversation.id)
                    if (latestMessage != null) {
                        messageDao.markMessageDeleted(latestMessage.id, deletionMsg.timestamp)
                        val updatedConv = conversation.copy(
                            deletedMessagesCount = conversation.deletedMessagesCount + 1
                        )
                        conversationDao.updateConversation(updatedConv)
                        return@withContext SaveResult.MarkedDeleted(
                            messageId = latestMessage.id,
                            conversationId = conversation.id,
                            deletedMessageText = latestMessage.messageText
                        )
                    }
                }
            }
            return@withContext SaveResult.Ignored("No valid content to persist")
        }

        // Sort chronologically
        val sortedMessages = validMessages.sortedBy { it.timestamp }

        var conversation = conversationDao.findConversation(packageName, chatTitle, isClonedApp)
        val conversationId: Long

        val prefManager = PreferenceManager(MessageVaultApp.get())
        val shouldAutoRestore = prefManager.isAutoRestoreOnNewMessage()

        if (conversation == null) {
            val newConversation = ConversationEntity(
                packageName = packageName,
                appDisplayName = appDisplayName,
                chatTitle = chatTitle,
                isClonedApp = isClonedApp,
                userHandleId = userHandleId,
                lastMessageText = sortedMessages.last().text.trim(),
                lastMessageTimestamp = sortedMessages.last().timestamp,
                messageCount = 0,
                deletedMessagesCount = 0,
                isInTrash = false,
                trashedAt = null
            )
            conversationId = conversationDao.insertConversation(newConversation)
            conversation = conversationDao.getConversationById(conversationId)
        } else {
            conversationId = conversation.id
        }

        var savedCount = 0
        var lastSavedMessageId = 0L
        var latestSavedMessage: ExtractedNotificationMessage? = null

        for ((index, item) in sortedMessages.withIndex()) {
            val trimmedText = item.text.trim()
            val sender = item.senderName.ifBlank { chatTitle }
            val time = item.timestamp

            // Stable hash: package + chatTitle + sender + text + clone status + exact timestamp + batch index
            // Distinguishes genuine identical texts sent at different timestamps or distinct order
            val rawHash = "$packageName|$chatTitle|$sender|$trimmedText|$isClonedApp|${time}#$index"
            val contentHash = md5(rawHash)

            val existing = messageDao.findMessageByHash(contentHash)
            if (existing == null) {
                val entity = MessageEntity(
                    conversationId = conversationId,
                    senderName = sender,
                    messageText = trimmedText,
                    timestamp = time,
                    isDeletedBySender = false,
                    contentHash = contentHash,
                    isFromClonedApp = isClonedApp
                )
                lastSavedMessageId = messageDao.insertMessage(entity)
                savedCount++
                latestSavedMessage = item
            }
        }

        if (savedCount > 0 && conversation != null) {
            val latest = latestSavedMessage ?: sortedMessages.last()
            val newTrashState = if (conversation.isInTrash && shouldAutoRestore) false else conversation.isInTrash
            val newTrashedAt = if (conversation.isInTrash && shouldAutoRestore) null else conversation.trashedAt

            val updatedConv = conversation.copy(
                lastMessageText = latest.text.trim(),
                lastMessageTimestamp = latest.timestamp,
                messageCount = conversation.messageCount + savedCount,
                appDisplayName = appDisplayName,
                isInTrash = newTrashState,
                trashedAt = newTrashedAt
            )
            conversationDao.updateConversation(updatedConv)
            return@withContext SaveResult.Saved(lastSavedMessageId, conversationId)
        }

        if (savedCount == 0) {
            return@withContext SaveResult.Duplicate
        }

        SaveResult.Saved(lastSavedMessageId, conversationId)
    }

    suspend fun updateMessageTranslation(messageId: Long, translatedText: String, language: String) =
        withContext(Dispatchers.IO) {
            messageDao.updateTranslation(messageId, translatedText, language)
        }

    // Soft delete: move to trash
    suspend fun moveConversationToTrash(id: Long) = withContext(Dispatchers.IO) {
        conversationDao.moveToTrash(id, System.currentTimeMillis())
    }

    suspend fun deleteConversation(id: Long) = moveConversationToTrash(id)

    suspend fun moveMultipleToTrash(ids: List<Long>) = withContext(Dispatchers.IO) {
        if (ids.isNotEmpty()) {
            conversationDao.moveMultipleToTrash(ids, System.currentTimeMillis())
        }
    }

    // Restore from trash
    suspend fun restoreConversationFromTrash(id: Long) = withContext(Dispatchers.IO) {
        conversationDao.restoreFromTrash(id)
    }

    suspend fun restoreMultipleFromTrash(ids: List<Long>) = withContext(Dispatchers.IO) {
        if (ids.isNotEmpty()) {
            conversationDao.restoreMultipleFromTrash(ids)
        }
    }

    // Permanent delete
    suspend fun permanentlyDeleteConversation(id: Long) = withContext(Dispatchers.IO) {
        conversationDao.permanentlyDeleteById(id)
    }

    suspend fun permanentlyDeleteMultiple(ids: List<Long>) = withContext(Dispatchers.IO) {
        if (ids.isNotEmpty()) {
            conversationDao.permanentlyDeleteMultiple(ids)
        }
    }

    suspend fun emptyTrash() = withContext(Dispatchers.IO) {
        conversationDao.emptyTrash()
    }

    suspend fun deleteMessage(id: Long) = withContext(Dispatchers.IO) {
        messageDao.deleteMessageById(id)
    }

    suspend fun clearAllData() = withContext(Dispatchers.IO) {
        conversationDao.clearAllConversations()
    }

    suspend fun getStats(): VaultStats = withContext(Dispatchers.IO) {
        val convCount = conversationDao.getConversationsCount()
        val trashCount = conversationDao.getTrashConversationsCount()
        val msgCount = messageDao.getTotalMessagesCount()
        val deletedCount = messageDao.getTotalDeletedMessagesCount()
        val latest = messageDao.getMostRecentMessage()
        VaultStats(
            conversationsCount = convCount,
            trashCount = trashCount,
            messagesCount = msgCount,
            deletedMessagesCount = deletedCount,
            lastMessage = latest
        )
    }

    // Storage Access Framework JSON Export
    suspend fun exportDataToJson(): String = withContext(Dispatchers.IO) {
        val root = JSONObject()
        root.put("version", 2)
        root.put("exportedAt", System.currentTimeMillis())
        root.put("appName", "الرسائل المحذوفة")
        root.put("developer", "Waleed Al-khatib")

        val conversationsList = conversationDao.getAllConversationsList()
        val conversationsArray = JSONArray()

        for (c in conversationsList) {
            val convObj = JSONObject()
            convObj.put("id", c.id)
            convObj.put("packageName", c.packageName)
            convObj.put("appDisplayName", c.appDisplayName)
            convObj.put("chatTitle", c.chatTitle)
            convObj.put("isClonedApp", c.isClonedApp)
            convObj.put("lastMessageText", c.lastMessageText)
            convObj.put("lastMessageTimestamp", c.lastMessageTimestamp)
            convObj.put("messageCount", c.messageCount)
            convObj.put("deletedMessagesCount", c.deletedMessagesCount)
            convObj.put("isInTrash", c.isInTrash)

            val messages = messageDao.getMessagesForConversationList(c.id)
            val msgArray = JSONArray()
            for (m in messages) {
                val mObj = JSONObject()
                mObj.put("id", m.id)
                mObj.put("senderName", m.senderName)
                mObj.put("messageText", m.messageText)
                mObj.put("timestamp", m.timestamp)
                mObj.put("isDeletedBySender", m.isDeletedBySender)
                mObj.put("translatedText", m.translatedText ?: "")
                mObj.put("isFromClonedApp", m.isFromClonedApp)
                msgArray.put(mObj)
            }
            convObj.put("messages", msgArray)
            conversationsArray.put(convObj)
        }
        root.put("conversations", conversationsArray)
        root.toString(2)
    }

    // Storage Access Framework JSON Import
    suspend fun importDataFromJson(jsonString: String): ImportResult = withContext(Dispatchers.IO) {
        try {
            val root = JSONObject(jsonString)
            val conversationsArray = root.optJSONArray("conversations")
                ?: return@withContext ImportResult.Error("ملف النسخة الاحتياطية غير صالح")

            var totalRestoredConvs = 0
            var totalRestoredMsgs = 0

            for (i in 0 until conversationsArray.length()) {
                val cObj = conversationsArray.getJSONObject(i)
                val packageName = cObj.getString("packageName")
                val chatTitle = cObj.getString("chatTitle")
                val isClonedApp = cObj.optBoolean("isClonedApp", false)
                val appDisplayName = cObj.optString("appDisplayName", "تطبيق محادثة")
                val lastMessageText = cObj.optString("lastMessageText", "")
                val lastMessageTimestamp = cObj.optLong("lastMessageTimestamp", System.currentTimeMillis())
                val isInTrash = cObj.optBoolean("isInTrash", false)

                var conv = conversationDao.findConversation(packageName, chatTitle, isClonedApp)
                val convId: Long
                if (conv == null) {
                    val newConv = ConversationEntity(
                        packageName = packageName,
                        appDisplayName = appDisplayName,
                        chatTitle = chatTitle,
                        isClonedApp = isClonedApp,
                        lastMessageText = lastMessageText,
                        lastMessageTimestamp = lastMessageTimestamp,
                        isInTrash = isInTrash
                    )
                    convId = conversationDao.insertConversation(newConv)
                    totalRestoredConvs++
                } else {
                    convId = conv.id
                }

                val msgArray = cObj.optJSONArray("messages")
                if (msgArray != null) {
                    for (j in 0 until msgArray.length()) {
                        val mObj = msgArray.getJSONObject(j)
                        val text = mObj.getString("messageText")
                        val sender = mObj.optString("senderName", chatTitle)
                        val timestamp = mObj.optLong("timestamp", System.currentTimeMillis())
                        val isDeleted = mObj.optBoolean("isDeletedBySender", false)
                        val translated = mObj.optString("translatedText", "").takeIf { it.isNotBlank() }

                        val hash = md5("$packageName|$chatTitle|$sender|$text|$isClonedApp|${timestamp / 3000}")
                        if (messageDao.findMessageByHash(hash) == null) {
                            val msg = MessageEntity(
                                conversationId = convId,
                                senderName = sender,
                                messageText = text,
                                timestamp = timestamp,
                                isDeletedBySender = isDeleted,
                                translatedText = translated,
                                contentHash = hash,
                                isFromClonedApp = isClonedApp
                            )
                            messageDao.insertMessage(msg)
                            totalRestoredMsgs++
                        }
                    }
                }
            }

            ImportResult.Success(totalRestoredConvs, totalRestoredMsgs)
        } catch (e: Exception) {
            ImportResult.Error("فشل استيراد النسخة الاحتياطية: ${e.localizedMessage}")
        }
    }

    private fun isDeletionIndicator(text: String): Boolean {
        val lower = text.lowercase().trim()
        return lower.contains("تم حذف هذه الرسالة") ||
                lower.contains("this message was deleted") ||
                lower.contains("you deleted this message") ||
                lower.contains("قام بحذف هذه الرسالة") ||
                lower.contains("deleted this message") ||
                lower.contains("message deleted")
    }

    private fun md5(input: String): String {
        val md = MessageDigest.getInstance("MD5")
        val digested = md.digest(input.toByteArray())
        return digested.joinToString("") { "%02x".format(it) }
    }
}

sealed class SaveResult {
    data class Saved(val messageId: Long, val conversationId: Long = 0L) : SaveResult()
    data class MarkedDeleted(
        val messageId: Long,
        val conversationId: Long = 0L,
        val deletedMessageText: String = ""
    ) : SaveResult()
    object Duplicate : SaveResult()
    data class Ignored(val reason: String) : SaveResult()
}

sealed class ImportResult {
    data class Success(val conversationsRestored: Int, val messagesRestored: Int) : ImportResult()
    data class Error(val message: String) : ImportResult()
}

data class VaultStats(
    val conversationsCount: Int,
    val trashCount: Int,
    val messagesCount: Int,
    val deletedMessagesCount: Int,
    val lastMessage: MessageEntity?
)

data class ExtractedNotificationMessage(
    val senderName: String,
    val text: String,
    val timestamp: Long
)

