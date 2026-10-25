package com.example.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "messages",
    foreignKeys = [
        ForeignKey(
            entity = ConversationEntity::class,
            parentColumns = ["id"],
            childColumns = ["conversationId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["conversationId"]),
        Index(value = ["timestamp"]),
        Index(value = ["contentHash"])
    ]
)
data class MessageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val conversationId: Long,
    val senderName: String,
    val messageText: String,
    val timestamp: Long,
    val isDeletedBySender: Boolean = false,
    val deletedDetectedAt: Long? = null,
    val originalLanguage: String? = null,
    val translatedText: String? = null,
    val translationLanguage: String? = null,
    val contentHash: String = "",
    val isFromClonedApp: Boolean = false
)
