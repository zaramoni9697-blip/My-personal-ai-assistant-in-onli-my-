package com.mdfoysal.jarvis.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A saved AI provider configuration.
 *
 * The API key is **not** stored here: only the [credentialId] that points into the
 * encrypted credential vault (Android Keystore protected).
 */
@Entity(
    tableName = "provider_configs",
    indices = [Index(value = ["userId"]), Index(value = ["credentialId"])]
)
data class ProviderConfigEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val providerId: String,
    val nickname: String,
    val baseUrl: String,
    val chatPath: String,
    val testPath: String,
    val model: String,
    val authType: String,
    val apiKeyHeader: String,
    val apiKeyQueryParam: String?,
    val extraHeadersJson: String,
    val streaming: Boolean,
    val temperature: Double?,
    val maxTokens: Int?,
    val isActive: Boolean,
    val requestStyle: String,
    val responseTextPath: String?,
    val credentialId: String,
    val createdAt: Long,
    val updatedAt: Long
)
