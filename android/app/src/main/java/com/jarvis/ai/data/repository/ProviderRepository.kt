package com.jarvis.ai.data.repository

import com.jarvis.ai.data.local.db.ProviderDao
import com.jarvis.ai.data.local.db.ProviderEntity
import com.jarvis.ai.data.security.CryptoManager
import com.jarvis.ai.network.provider.ProviderAdapterFactory
import com.jarvis.ai.network.provider.ProviderType
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProviderRepository @Inject constructor(
    private val dao: ProviderDao,
    private val crypto: CryptoManager
) {
    fun observeProviders(userId: String): Flow<List<ProviderEntity>> = dao.observeForUser(userId)

    suspend fun getProviders(userId: String) = dao.getForUser(userId)
    suspend fun getActive(userId: String) = dao.getActive(userId)

    suspend fun saveProvider(
        userId: String,
        type: ProviderType,
        displayName: String,
        apiKey: String,
        modelId: String,
        baseUrl: String?
    ): Result<ProviderEntity> {
        if (apiKey.isBlank()) return Result.failure(Exception("API key required"))
        if (modelId.isBlank()) return Result.failure(Exception("Model ID required"))
        val id = java.util.UUID.randomUUID().toString()
        crypto.saveApiKey(id, apiKey)
        val entity = ProviderEntity(
            id = id, userId = userId, providerType = type.id,
            displayName = displayName.ifBlank { type.displayName },
            apiKeyEncrypted = "enc_ref:$id", modelId = modelId, baseUrl = baseUrl?.ifBlank { null }
        )
        dao.insert(entity)
        return Result.success(entity)
    }

    suspend fun updateProvider(entity: ProviderEntity, newApiKey: String?): Result<ProviderEntity> {
        if (newApiKey != null && newApiKey.isNotBlank()) crypto.saveApiKey(entity.id, newApiKey)
        val updated = entity.copy(updatedAt = System.currentTimeMillis())
        dao.update(updated)
        return Result.success(updated)
    }

    suspend fun deleteProvider(id: String) {
        dao.deleteById(id)
        crypto.removeApiKey(id)
    }

    suspend fun setActive(userId: String, providerId: String) {
        dao.deactivateAll(userId); dao.activate(providerId)
    }

    suspend fun testConnection(provider: ProviderEntity): Result<String> {
        val apiKey = crypto.getApiKey(provider.id) ?: return Result.failure(Exception("API key not found"))
        val type = ProviderType.values().find { it.id == provider.providerType } ?: ProviderType.CUSTOM
        val adapter = ProviderAdapterFactory.get(type)
        return adapter.testConnection(apiKey, provider.modelId, provider.baseUrl ?: type.defaultBaseUrl)
    }

    fun getDecryptedKey(id: String): String? = crypto.getApiKey(id)
}
