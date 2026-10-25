package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "conversations",
    indices = [
        Index(value = ["packageName", "chatTitle", "isClonedApp"], unique = true),
        Index(value = ["lastMessageTimestamp"]),
        Index(value = ["isInTrash"])
    ]
)
data class ConversationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val packageName: String,
    val appDisplayName: String,
    val chatTitle: String,
    val isClonedApp: Boolean = false,
    val userHandleId: Int = 0,
    val lastMessageText: String = "",
    val lastMessageTimestamp: Long = System.currentTimeMillis(),
    val messageCount: Int = 0,
    val deletedMessagesCount: Int = 0,
    val isPinned: Boolean = false,
    val isInTrash: Boolean = false,
    val trashedAt: Long? = null
)
