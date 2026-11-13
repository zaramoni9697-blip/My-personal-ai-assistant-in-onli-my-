package com.mdfoysal.jarvis.data.local

import com.mdfoysal.jarvis.core.json.Json
import com.mdfoysal.jarvis.core.json.JsonValue
import com.mdfoysal.jarvis.core.providers.AuthType
import com.mdfoysal.jarvis.core.providers.ProviderConfig
import com.mdfoysal.jarvis.core.providers.ProviderRegistry
import com.mdfoysal.jarvis.core.providers.RequestStyle
import com.mdfoysal.jarvis.data.local.entity.ProviderConfigEntity

/** Converts between the Room entities and the pure-Kotlin core models. */
object Mappers {

    fun toDomain(entity: ProviderConfigEntity): ProviderConfig = ProviderConfig(
        id = entity.id,
        providerId = entity.providerId,
        nickname = entity.nickname,
        baseUrl = entity.baseUrl,
        chatPath = entity.chatPath,
        testPath = entity.testPath,
        model = entity.model,
        authType = runCatching { AuthType.valueOf(entity.authType) }.getOrDefault(AuthType.BEARER),
        apiKeyHeader = entity.apiKeyHeader,
        apiKeyQueryParam = entity.apiKeyQueryParam,
        extraHeaders = parseHeaders(entity.extraHeadersJson),
        streaming = entity.streaming,
        temperature = entity.temperature,
        maxTokens = entity.maxTokens,
        isActive = entity.isActive,
        createdAt = entity.createdAt,
        credentialId = entity.credentialId,
        requestStyle = runCatching { RequestStyle.valueOf(entity.requestStyle) }
            .getOrDefault(RequestStyle.OPENAI),
        responseTextPath = entity.responseTextPath
    )

    fun toEntity(domain: ProviderConfig, userId: String, updatedAt: Long): ProviderConfigEntity =
        ProviderConfigEntity(
            id = domain.id,
            userId = userId,
            providerId = domain.providerId,
            nickname = domain.nickname,
            baseUrl = domain.baseUrl,
            chatPath = domain.chatPath,
            testPath = domain.testPath,
            model = domain.model,
            authType = domain.authType.name,
            apiKeyHeader = domain.apiKeyHeader,
            apiKeyQueryParam = domain.apiKeyQueryParam,
            extraHeadersJson = encodeHeaders(domain.extraHeaders),
            streaming = domain.streaming,
            temperature = domain.temperature,
            maxTokens = domain.maxTokens,
            isActive = domain.isActive,
            requestStyle = domain.requestStyle.name,
            responseTextPath = domain.responseTextPath,
            credentialId = domain.credentialId,
            createdAt = domain.createdAt,
            updatedAt = updatedAt
        )

    fun encodeHeaders(headers: Map<String, String>): String =
        Json.encode(JsonValue.Obj(headers.mapValues { JsonValue.Str(it.value) }))

    fun parseHeaders(json: String): Map<String, String> {
        if (json.isBlank()) return emptyMap()
        val parsed = Json.parseOrNull(json) ?: return emptyMap()
        return parsed.asObjectOrNull()?.mapNotNull { (key, value) ->
            value.asStringOrNull()?.let { key to it }
        }?.toMap() ?: emptyMap()
    }

    /** Builds the default entity for a provider (used when the user adds a new configuration). */
    fun defaultEntity(
        spec: com.mdfoysal.jarvis.core.providers.ProviderSpec,
        userId: String,
        configId: String,
        credentialId: String,
        now: Long
    ): ProviderConfigEntity {
        val domain = ProviderRegistry.defaultConfig(spec, configId, credentialId, now)
        return toEntity(domain, userId, now)
    }
}
