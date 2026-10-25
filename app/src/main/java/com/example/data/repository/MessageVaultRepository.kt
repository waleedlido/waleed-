package com.example.data.repository

import com.example.data.database.MessageVaultDatabase
import com.example.data.model.ConversationEntity
import com.example.data.model.MessageEntity
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
            // Find existing conversation
            val conversation = conversationDao.findConversation(packageName, chatTitle, isClonedApp)
            if (conversation != null) {
                val latestMessage = messageDao.getLatestNonDeletedMessage(conversation.id)
                if (latestMessage != null) {
                    messageDao.markMessageDeleted(latestMessage.id, timestamp)
                    val updatedConv = conversation.copy(
                        deletedMessagesCount = conversation.deletedMessagesCount + 1
                    )
                    conversationDao.updateConversation(updatedConv)
                    return@withContext SaveResult.MarkedDeleted(latestMessage.id)
                }
            }
            return@withContext SaveResult.Ignored("Deletion notice could not be matched")
        }

        // De-duplication hash: package + chatTitle + sender + text + timestamp bucket (3s window)
        val timeBucket = timestamp / 3000
        val rawHash = "$packageName|$chatTitle|$senderName|$trimmedText|$isClonedApp|$timeBucket"
        val contentHash = md5(rawHash)

        val existingMessage = messageDao.findMessageByHash(contentHash)
        if (existingMessage != null) {
            return@withContext SaveResult.Duplicate
        }

        // Retrieve or create conversation
        var conversation = conversationDao.findConversation(packageName, chatTitle, isClonedApp)
        val conversationId: Long

        if (conversation == null) {
            val newConversation = ConversationEntity(
                packageName = packageName,
                appDisplayName = appDisplayName,
                chatTitle = chatTitle,
                isClonedApp = isClonedApp,
                userHandleId = userHandleId,
                lastMessageText = trimmedText,
                lastMessageTimestamp = timestamp,
                messageCount = 1,
                deletedMessagesCount = 0
            )
            conversationId = conversationDao.insertConversation(newConversation)
        } else {
            conversationId = conversation.id
            val updatedConv = conversation.copy(
                lastMessageText = trimmedText,
                lastMessageTimestamp = timestamp,
                messageCount = conversation.messageCount + 1,
                appDisplayName = appDisplayName
            )
            conversationDao.updateConversation(updatedConv)
        }

        val message = MessageEntity(
            conversationId = conversationId,
            senderName = senderName.ifBlank { chatTitle },
            messageText = trimmedText,
            timestamp = timestamp,
            isDeletedBySender = false,
            contentHash = contentHash,
            isFromClonedApp = isClonedApp
        )
        val messageId = messageDao.insertMessage(message)
        SaveResult.Saved(messageId)
    }

    suspend fun updateMessageTranslation(messageId: Long, translatedText: String, language: String) =
        withContext(Dispatchers.IO) {
            messageDao.updateTranslation(messageId, translatedText, language)
        }

    suspend fun deleteConversation(id: Long) = withContext(Dispatchers.IO) {
        conversationDao.deleteConversationById(id)
    }

    suspend fun deleteMessage(id: Long) = withContext(Dispatchers.IO) {
        messageDao.deleteMessageById(id)
    }

    suspend fun clearAllData() = withContext(Dispatchers.IO) {
        conversationDao.clearAllConversations()
    }

    suspend fun getStats(): VaultStats = withContext(Dispatchers.IO) {
        val convCount = conversationDao.getConversationsCount()
        val msgCount = messageDao.getTotalMessagesCount()
        val deletedCount = messageDao.getTotalDeletedMessagesCount()
        val latest = messageDao.getMostRecentMessage()
        VaultStats(
            conversationsCount = convCount,
            messagesCount = msgCount,
            deletedMessagesCount = deletedCount,
            lastMessage = latest
        )
    }

    // Storage Access Framework JSON Export
    suspend fun exportDataToJson(): String = withContext(Dispatchers.IO) {
        val root = JSONObject()
        root.put("version", 1)
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

                var conv = conversationDao.findConversation(packageName, chatTitle, isClonedApp)
                val convId: Long
                if (conv == null) {
                    val newConv = ConversationEntity(
                        packageName = packageName,
                        appDisplayName = appDisplayName,
                        chatTitle = chatTitle,
                        isClonedApp = isClonedApp,
                        lastMessageText = lastMessageText,
                        lastMessageTimestamp = lastMessageTimestamp
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
    data class Saved(val messageId: Long) : SaveResult()
    data class MarkedDeleted(val messageId: Long) : SaveResult()
    object Duplicate : SaveResult()
    data class Ignored(val reason: String) : SaveResult()
}

sealed class ImportResult {
    data class Success(val conversationsRestored: Int, val messagesRestored: Int) : ImportResult()
    data class Error(val message: String) : ImportResult()
}

data class VaultStats(
    val conversationsCount: Int,
    val messagesCount: Int,
    val deletedMessagesCount: Int,
    val lastMessage: MessageEntity?
)
