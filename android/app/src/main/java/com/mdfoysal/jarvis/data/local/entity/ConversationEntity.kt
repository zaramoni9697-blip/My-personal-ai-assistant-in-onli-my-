package com.mdfoysal.jarvis.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** A conversation thread owned by a single user. */
@Entity(tableName = "conversations", indices = [Index(value = ["userId"])])
data class ConversationEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val title: String,
    val providerConfigId: String?,
    val createdAt: Long,
    val updatedAt: Long,
    val isArchived: Boolean = false
)
