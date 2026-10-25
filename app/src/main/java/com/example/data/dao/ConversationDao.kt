package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.ConversationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ConversationDao {
    @Query("SELECT * FROM conversations WHERE isInTrash = 0 ORDER BY isPinned DESC, lastMessageTimestamp DESC")
    fun getAllConversationsFlow(): Flow<List<ConversationEntity>>

    @Query("""
        SELECT * FROM conversations 
        WHERE isInTrash = 0 
          AND (chatTitle LIKE '%' || :query || '%' OR lastMessageText LIKE '%' || :query || '%')
        ORDER BY isPinned DESC, lastMessageTimestamp DESC
    """)
    fun searchConversationsFlow(query: String): Flow<List<ConversationEntity>>

    @Query("SELECT * FROM conversations WHERE isInTrash = 1 ORDER BY trashedAt DESC, lastMessageTimestamp DESC")
    fun getTrashConversationsFlow(): Flow<List<ConversationEntity>>

    @Query("SELECT COUNT(*) FROM conversations WHERE isInTrash = 1")
    fun getTrashCountFlow(): Flow<Int>

    @Query("SELECT * FROM conversations WHERE id = :id LIMIT 1")
    suspend fun getConversationById(id: Long): ConversationEntity?

    @Query("""
        SELECT * FROM conversations 
        WHERE packageName = :packageName AND chatTitle = :chatTitle AND isClonedApp = :isClonedApp 
        LIMIT 1
    """)
    suspend fun findConversation(packageName: String, chatTitle: String, isClonedApp: Boolean): ConversationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConversation(conversation: ConversationEntity): Long

    @Update
    suspend fun updateConversation(conversation: ConversationEntity)

    @Query("UPDATE conversations SET isInTrash = 1, trashedAt = :trashedAt WHERE id = :id")
    suspend fun moveToTrash(id: Long, trashedAt: Long)

    @Query("UPDATE conversations SET isInTrash = 1, trashedAt = :trashedAt WHERE id IN (:ids)")
    suspend fun moveMultipleToTrash(ids: List<Long>, trashedAt: Long)

    @Query("UPDATE conversations SET isInTrash = 0, trashedAt = NULL WHERE id = :id")
    suspend fun restoreFromTrash(id: Long)

    @Query("UPDATE conversations SET isInTrash = 0, trashedAt = NULL WHERE id IN (:ids)")
    suspend fun restoreMultipleFromTrash(ids: List<Long>)

    @Query("DELETE FROM conversations WHERE id = :id")
    suspend fun permanentlyDeleteById(id: Long)

    @Query("DELETE FROM conversations WHERE id IN (:ids)")
    suspend fun permanentlyDeleteMultiple(ids: List<Long>)

    @Query("DELETE FROM conversations WHERE isInTrash = 1")
    suspend fun emptyTrash()

    @Query("DELETE FROM conversations")
    suspend fun clearAllConversations()

    @Query("SELECT COUNT(*) FROM conversations WHERE isInTrash = 0")
    suspend fun getConversationsCount(): Int

    @Query("SELECT COUNT(*) FROM conversations WHERE isInTrash = 1")
    suspend fun getTrashConversationsCount(): Int

    @Query("SELECT * FROM conversations WHERE isInTrash = 0 ORDER BY lastMessageTimestamp DESC")
    suspend fun getAllConversationsList(): List<ConversationEntity>
}
