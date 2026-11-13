package com.mdfoysal.jarvis.data.repository

import android.content.Context
import com.mdfoysal.jarvis.core.chat.ChatEngine
import com.mdfoysal.jarvis.core.providers.AuthType
import com.mdfoysal.jarvis.core.providers.ProviderConfig
import com.mdfoysal.jarvis.core.providers.ProviderRegistry
import com.mdfoysal.jarvis.core.providers.ProviderSpec
import com.mdfoysal.jarvis.core.providers.RequestStyle
import com.mdfoysal.jarvis.core.providers.adapters.ProviderAdapters
import com.mdfoysal.jarvis.data.local.Mappers
import com.mdfoysal.jarvis.data.local.SettingsStore
import com.mdfoysal.jarvis.data.local.dao.ProviderConfigDao
import com.mdfoysal.jarvis.data.security.CredentialVault
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.UUID

/** Loads the canonical provider registry from `assets/providers.json`. */
class ProviderSpecService(context: Context) {

    private val appContext = context.applicationContext

    val specs: List<ProviderSpec> by lazy {
        runCatching {
            appContext.assets.open(PROVIDERS_ASSET).bufferedReader().use { reader ->
                ProviderRegistry.parse(reader.readText())
            }
        }.getOrElse { emptyList() }
    }

    fun all(): List<ProviderSpec> = specs

    fun byId(id: String): ProviderSpec? = specs.firstOrNull { it.id == id }

    companion object {
        const val PROVIDERS_ASSET = "providers.json"
    }
}

/** Everything the configuration screen collects for one provider configuration. */
data class ProviderDraft(
    val providerId: String,
    val nickname: String,
    val baseUrl: String,
    val chatPath: String,
    val testPath: String,
    val model: String,
    val apiKey: String = "",
    val authType: AuthType = AuthType.BEARER,
    val apiKeyHeader: String = "Authorization",
    val apiKeyQueryParam: String? = null,
    val extraHeaders: Map<String, String> = emptyMap(),
    val streaming: Boolean = true,
    val temperature: Double? = null,
    val maxTokens: Int? = null,
    val requestStyle: RequestStyle = RequestStyle.OPENAI,
    val responseTextPath: String? = null
)

/** Outcome of the "Test connection" button. */
sealed class ConnectionTestResult {
    data class Success(val message: String, val latencyMs: Long) : ConnectionTestResult()
    data class Failure(val message: String, val code: String, val retryable: Boolean) : ConnectionTestResult()
}

/**
 * Manages the user's provider configurations and their encrypted credentials.
 *
 * Every configuration belongs to exactly one user — queries are always scoped by `userId`,
 * so one user can never see (or use) another user's key.
 */
class ProviderRepository(
    private val dao: ProviderConfigDao,
    private val vault: CredentialVault,
    private val settingsStore: SettingsStore,
    private val specService: ProviderSpecService,
    private val chatEngine: ChatEngine
) {

    fun specs(): List<ProviderSpec> = specService.all()

    fun specFor(providerId: String): ProviderSpec? = specService.byId(providerId)

    fun observeConfigs(userId: String): Flow<List<ProviderConfig>> =
        dao.observeForUser(userId).map { list -> list.map { Mappers.toDomain(it) } }

    fun observeActive(userId: String): Flow<ProviderConfig?> =
        dao.observeActive(userId).map { it?.let { entity -> Mappers.toDomain(entity) } }

    suspend fun configById(id: String): ProviderConfig? =
        dao.findById(id)?.let { Mappers.toDomain(it) }

    suspend fun activeConfig(userId: String): ProviderConfig? {
        val settings = settingsStore.settings.first()
        val preferred = settings.activeProviderId
        if (!preferred.isNullOrBlank()) {
            dao.findById(preferred)?.let { return Mappers.toDomain(it) }
        }
        return dao.activeForUser(userId)?.let { Mappers.toDomain(it) }
    }

    suspend fun hasConfiguration(userId: String): Boolean = dao.countForUser(userId) > 0

    /** Creates a new configuration and stores its API key in the encrypted vault. */
    suspend fun create(userId: String, draft: ProviderDraft): Result<ProviderConfig> {
        val spec = specService.byId(draft.providerId)
            ?: return Result.failure(IllegalArgumentException("Unknown provider '${draft.providerId}'"))
        val validation = validate(draft)
        if (validation != null) return Result.failure(IllegalArgumentException(validation))

        val now = System.currentTimeMillis()
        val configId = UUID.randomUUID().toString()
        val credentialId = UUID.randomUUID().toString()
        val isFirst = dao.countForUser(userId) == 0

        val base = ProviderRegistry.defaultConfig(spec, configId, credentialId, now)
        val domain = base.copy(
            nickname = draft.nickname.trim().ifBlank { spec.label },
            baseUrl = draft.baseUrl.trim().ifBlank { spec.defaultBaseUrl },
            chatPath = draft.chatPath.trim().ifBlank { spec.defaultChatPath },
            testPath = draft.testPath.trim().ifBlank { spec.defaultTestPath },
            model = draft.model.trim().ifBlank { spec.defaultModel },
            authType = draft.authType,
            apiKeyHeader = draft.apiKeyHeader.ifBlank { spec.apiKeyHeader },
            apiKeyQueryParam = draft.apiKeyQueryParam,
            extraHeaders = draft.extraHeaders.ifEmpty { spec.extraHeaders },
            streaming = draft.streaming,
            temperature = draft.temperature,
            maxTokens = draft.maxTokens,
            isActive = isFirst,
            requestStyle = if (spec.id == "custom") draft.requestStyle else spec.requestStyle,
            responseTextPath = draft.responseTextPath
        )
        vault.saveApiKey(credentialId, draft.apiKey.trim())
        dao.insert(Mappers.toEntity(domain, userId, now))
        if (isFirst) settingsStore.setActiveProviderId(configId)
        return Result.success(domain)
    }

    /** Updates an existing configuration (and its key when a new one was typed). */
    suspend fun update(
        userId: String,
        config: ProviderConfig,
        draft: ProviderDraft
    ): Result<ProviderConfig> {
        val validation = validate(draft)
        if (validation != null) return Result.failure(IllegalArgumentException(validation))

        val updated = config.copy(
            nickname = draft.nickname.trim().ifBlank { config.nickname },
            baseUrl = draft.baseUrl.trim().ifBlank { config.baseUrl },
            chatPath = draft.chatPath.trim().ifBlank { config.chatPath },
            testPath = draft.testPath.trim().ifBlank { config.testPath },
            model = draft.model.trim().ifBlank { config.model },
            authType = draft.authType,
            apiKeyHeader = draft.apiKeyHeader.ifBlank { config.apiKeyHeader },
            apiKeyQueryParam = draft.apiKeyQueryParam,
            extraHeaders = draft.extraHeaders,
            streaming = draft.streaming,
            temperature = draft.temperature,
            maxTokens = draft.maxTokens,
            requestStyle = draft.requestStyle,
            responseTextPath = draft.responseTextPath
        )
        if (draft.apiKey.isNotBlank()) {
            vault.saveApiKey(config.credentialId, draft.apiKey.trim())
        }
        dao.update(Mappers.toEntity(updated, userId, System.currentTimeMillis()))
        return Result.success(updated)
    }

    /** Deletes a configuration and its encrypted credential. */
    suspend fun delete(config: ProviderConfig) {
        vault.delete(config.credentialId)
        dao.deleteById(config.id)
    }

    /** Makes [configId] the active provider for the user. */
    suspend fun setActive(userId: String, configId: String) {
        dao.clearActive(userId)
        dao.setActive(configId)
        settingsStore.setActiveProviderId(configId)
    }

    /** Reads the decrypted API key for a configuration (only used to build requests). */
    fun apiKeyFor(config: ProviderConfig): String? = vault.getApiKey(config.credentialId)

    fun hasApiKey(config: ProviderConfig): Boolean = vault.hasCredential(config.credentialId)

    /**
     * Stores a key for a configuration that is not saved yet.
     * Used by the "Test connection" button so a draft configuration can be validated.
     */
    fun tempCredential(credentialId: String, apiKey: String) {
        if (apiKey.isNotBlank()) vault.saveApiKey(credentialId, apiKey)
    }

    /** Removes a temporary credential created for a connection test. */
    fun deleteTemporaryCredential(credentialId: String) = vault.delete(credentialId)

    /** Runs a real, minimal completion against the provider to validate the configuration. */
    suspend fun testConnection(config: ProviderConfig): ConnectionTestResult {
        val spec = specService.byId(config.providerId)
        val adapter = ProviderAdapters.create(
            spec ?: ProviderSpec(
                id = config.providerId,
                label = config.nickname,
                description = "",
                requestStyle = config.requestStyle,
                defaultBaseUrl = config.baseUrl,
                defaultChatPath = config.chatPath,
                defaultTestPath = config.testPath,
                authType = config.authType,
                apiKeyHeader = config.apiKeyHeader,
                apiKeyQueryParam = config.apiKeyQueryParam,
                defaultModel = config.model,
                suggestedModels = emptyList(),
                supportsStreaming = config.streaming,
                supportsImages = false,
                customizable = true,
                extraHeaders = config.extraHeaders
            )
        )
        val apiKey = apiKeyFor(config)
        if (apiKey.isNullOrBlank()) {
            return ConnectionTestResult.Failure("No API key stored for this configuration", "missing_api_key", false)
        }
        val started = System.currentTimeMillis()
        return try {
            val reply = chatEngine.testConnection(config, adapter, apiKey)
            val latency = System.currentTimeMillis() - started
            val preview = reply.text.trim().ifBlank { "OK" }
            ConnectionTestResult.Success("Connected in ${latency}ms — model replied: $preview", latency)
        } catch (e: com.mdfoysal.jarvis.core.chat.ProviderException) {
            ConnectionTestResult.Failure(e.error.message, e.error.code, e.error.retryable)
        } catch (e: Exception) {
            ConnectionTestResult.Failure(e.message ?: "Connection failed", "network_error", true)
        }
    }

    private fun validate(draft: ProviderDraft): String? {
        if (draft.baseUrl.isBlank() && draft.providerId != "custom") {
            return "Base URL is required"
        }
        if (draft.model.isBlank()) {
            return "Model ID is required"
        }
        if (draft.baseUrl.isNotBlank() && !draft.baseUrl.startsWith("http")) {
            return "Base URL must start with http:// or https://"
        }
        return null
    }
}
