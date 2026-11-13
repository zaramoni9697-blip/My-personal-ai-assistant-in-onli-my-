package com.mdfoysal.jarvis.core.providers

import com.mdfoysal.jarvis.core.json.Json
import com.mdfoysal.jarvis.core.json.JsonValue

/**
 * Loads the canonical provider registry (`assets/providers.json` on Android, `shared/providers.json`
 * for the backend/tests) and turns it into typed [ProviderSpec] objects.
 */
object ProviderRegistry {

    fun parse(rawJson: String): List<ProviderSpec> {
        val root = Json.parseOrNull(rawJson) ?: return emptyList()
        val providers = root.optArray("providers")
        return providers.mapNotNull { parseSpec(it) }
    }

    fun parseSpec(value: JsonValue): ProviderSpec? {
        val id = value.optString("id").trim()
        if (id.isEmpty()) return null
        val extra = value.optObject("extraHeaders").mapValues { it.value.asStringOrNull() ?: "" }
        return ProviderSpec(
            id = id,
            label = value.optString("label", id),
            description = value.optString("description"),
            requestStyle = parseStyle(value.optString("requestStyle", "openai")),
            defaultBaseUrl = value.optString("defaultBaseUrl"),
            defaultChatPath = value.optString("defaultChatPath", "/chat/completions"),
            defaultTestPath = value.optString("defaultTestPath", "/models"),
            authType = parseAuth(value.optString("authType", "bearer")),
            apiKeyHeader = value.optString("apiKeyHeader", "Authorization"),
            apiKeyQueryParam = value.optString("apiKeyQueryParam").ifBlank { null },
            defaultModel = value.optString("defaultModel"),
            suggestedModels = value.optArray("suggestedModels").mapNotNull { it.asStringOrNull() },
            supportsStreaming = value.optBoolean("supportsStreaming", true),
            supportsImages = value.optBoolean("supportsImages", false),
            customizable = value.optBoolean("customizable", false),
            extraHeaders = extra
        )
    }

    fun parseStyle(raw: String): RequestStyle = when (raw.lowercase().trim()) {
        "anthropic" -> RequestStyle.ANTHROPIC
        "gemini" -> RequestStyle.GEMINI
        "plain" -> RequestStyle.PLAIN
        else -> RequestStyle.OPENAI
    }

    fun parseAuth(raw: String): AuthType = when (raw.lowercase().trim()) {
        "bearer" -> AuthType.BEARER
        "x-api-key", "x_api_key" -> AuthType.X_API_KEY
        "header" -> AuthType.HEADER
        "query" -> AuthType.QUERY
        "none" -> AuthType.NONE
        else -> AuthType.BEARER
    }

    /**
     * Builds the default [ProviderConfig] for a provider — the starting point of the
     * "Add provider" form. The API key is intentionally absent.
     */
    fun defaultConfig(
        spec: ProviderSpec,
        configId: String,
        credentialId: String,
        now: Long
    ): ProviderConfig = ProviderConfig(
        id = configId,
        providerId = spec.id,
        nickname = spec.label,
        baseUrl = spec.defaultBaseUrl,
        chatPath = spec.defaultChatPath,
        testPath = spec.defaultTestPath,
        model = spec.defaultModel,
        authType = spec.authType,
        apiKeyHeader = spec.apiKeyHeader,
        apiKeyQueryParam = spec.apiKeyQueryParam,
        extraHeaders = spec.extraHeaders,
        streaming = spec.supportsStreaming,
        temperature = null,
        maxTokens = null,
        isActive = false,
        createdAt = now,
        credentialId = credentialId,
        requestStyle = spec.requestStyle
    )
}
