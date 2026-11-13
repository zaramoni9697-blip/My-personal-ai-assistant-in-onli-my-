package com.mdfoysal.jarvis.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** A single message inside a conversation. */
@Entity(tableName = "messages", indices = [Index(value = ["conversationId"])])
data class MessageEntity(
    @PrimaryKey val id: String,
    val conversationId: String,
    val role: String,
    val text: String,
    val createdAt: Long,
    val imageBase64: String? = null,
    val imageMimeType: String? = null,
    val errorMessage: String? = null
)
