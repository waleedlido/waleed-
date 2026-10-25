package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.MessageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {
    @Query("SELECT * FROM messages WHERE conversationId = :conversationId ORDER BY timestamp ASC")
    fun getMessagesForConversationFlow(conversationId: Long): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages WHERE conversationId = :conversationId ORDER BY timestamp ASC")
    suspend fun getMessagesForConversationList(conversationId: Long): List<MessageEntity>

    @Query("SELECT * FROM messages WHERE conversationId = :conversationId ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestMessage(conversationId: Long): MessageEntity?

    @Query("SELECT * FROM messages WHERE conversationId = :conversationId AND isDeletedBySender = 0 ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestNonDeletedMessage(conversationId: Long): MessageEntity?

    @Query("SELECT * FROM messages WHERE contentHash = :hash LIMIT 1")
    suspend fun findMessageByHash(hash: String): MessageEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: MessageEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessages(messages: List<MessageEntity>)

    @Update
    suspend fun updateMessage(message: MessageEntity)

    @Query("UPDATE messages SET isDeletedBySender = 1, deletedDetectedAt = :deletedAt WHERE id = :messageId")
    suspend fun markMessageDeleted(messageId: Long, deletedAt: Long)

    @Query("UPDATE messages SET translatedText = :translated, translationLanguage = :lang WHERE id = :messageId")
    suspend fun updateTranslation(messageId: Long, translated: String, lang: String)

    @Query("DELETE FROM messages WHERE id = :messageId")
    suspend fun deleteMessageById(messageId: Long)

    @Query("DELETE FROM messages WHERE conversationId = :conversationId")
    suspend fun deleteMessagesForConversation(conversationId: Long)

    @Query("SELECT COUNT(*) FROM messages")
    suspend fun getTotalMessagesCount(): Int

    @Query("SELECT COUNT(*) FROM messages WHERE isDeletedBySender = 1")
    suspend fun getTotalDeletedMessagesCount(): Int

    @Query("SELECT * FROM messages WHERE messageText LIKE '%' || :query || '%' ORDER BY timestamp DESC")
    fun searchAllMessages(query: String): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages ORDER BY timestamp DESC LIMIT 1")
    suspend fun getMostRecentMessage(): MessageEntity?

    @Query("SELECT * FROM messages ORDER BY timestamp ASC")
    suspend fun getAllMessagesList(): List<MessageEntity>
}
