package com.mdfoysal.jarvis.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A locally registered account.
 *
 * Passwords are never stored — only a PBKDF2-HMAC-SHA256 hash with a per-user salt.
 */
@Entity(tableName = "users", indices = [Index(value = ["email"], unique = true)])
data class UserEntity(
    @PrimaryKey val id: String,
    val email: String,
    val displayName: String,
    val passwordHash: String,
    val createdAt: Long,
    val lastLoginAt: Long?
)
