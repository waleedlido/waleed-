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
    @Query("SELECT * FROM conversations ORDER BY isPinned DESC, lastMessageTimestamp DESC")
    fun getAllConversationsFlow(): Flow<List<ConversationEntity>>

    @Query("""
        SELECT * FROM conversations 
        WHERE chatTitle LIKE '%' || :query || '%' 
           OR lastMessageText LIKE '%' || :query || '%'
        ORDER BY isPinned DESC, lastMessageTimestamp DESC
    """)
    fun searchConversationsFlow(query: String): Flow<List<ConversationEntity>>

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

    @Query("DELETE FROM conversations WHERE id = :id")
    suspend fun deleteConversationById(id: Long)

    @Query("DELETE FROM conversations")
    suspend fun clearAllConversations()

    @Query("SELECT COUNT(*) FROM conversations")
    suspend fun getConversationsCount(): Int

    @Query("SELECT * FROM conversations ORDER BY lastMessageTimestamp DESC")
    suspend fun getAllConversationsList(): List<ConversationEntity>
}
