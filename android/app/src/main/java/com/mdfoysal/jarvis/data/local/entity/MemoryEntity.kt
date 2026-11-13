package com.mdfoysal.jarvis.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A user-controlled memory item ("remember that I prefer Bangla").
 * Users can list and delete these at any time.
 */
@Entity(tableName = "memories", indices = [Index(value = ["userId"])])
data class MemoryEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val content: String,
    val createdAt: Long,
    val isPinned: Boolean = false
)
