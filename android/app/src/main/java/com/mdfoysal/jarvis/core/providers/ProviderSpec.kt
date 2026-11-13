package com.mdfoysal.jarvis.core.providers

/**
 * How a provider expects the chat request to be shaped.
 *
 * JARVIS never assumes "one API format for everybody": every provider family gets its own
 * request/response adapter built on top of this enum.
 */
enum class RequestStyle {
    /** OpenAI-style `POST {base}/chat/completions` with `messages[]`. */
    OPENAI,

    /** Anthropic Messages API `POST {base}/messages` with `system` + `messages[]`. */
    ANTHROPIC,

    /** Google Gemini `POST {base}/models/{model}:generateContent` with `contents[]`. */
    GEMINI,

    /** Simple `{"model", "messages": [{"role", "content"}]}` shape for custom endpoints. */
    PLAIN
}

/** How the API key is attached to the outgoing request. */
enum class AuthType {
    /** `Authorization: Bearer <key>` */
    BEARER,

    /** `x-api-key: <key>` (Anthropic style) */
    X_API_KEY,

    /** Arbitrary header, e.g. `x-goog-api-key: <key>` */
    HEADER,

    /** `?key=<key>` query parameter */
    QUERY,

    /** No key required (local models / open gateways) */
    NONE
}

/**
 * Static, non-secret description of an AI provider.
 *
 * Mirrors `shared/providers.json`, which is the single source of truth for the whole project
 * (the Android app loads it from `assets/providers.json`, the web backend loads it from disk).
 */
data class ProviderSpec(
    val id: String,
    val label: String,
    val description: String,
    val requestStyle: RequestStyle,
    val defaultBaseUrl: String,
    val defaultChatPath: String,
    val defaultTestPath: String,
    val authType: AuthType,
    val apiKeyHeader: String,
    val apiKeyQueryParam: String?,
    val defaultModel: String,
    val suggestedModels: List<String>,
    val supportsStreaming: Boolean,
    val supportsImages: Boolean,
    val customizable: Boolean,
    val extraHeaders: Map<String, String>
) {
    /** Default model shown in the configuration screen. */
    val placeholderModel: String get() = if (defaultModel.isNotBlank()) defaultModel else "your-model-id"
}

/**
 * A user's saved provider configuration. Never contains the API key itself —
 * keys live in the encrypted credential vault and are referenced by [credentialId].
 */
data class ProviderConfig(
    val id: String,
    val providerId: String,
    val nickname: String,
    val baseUrl: String,
    val chatPath: String,
    val testPath: String,
    val model: String,
    val authType: AuthType,
    val apiKeyHeader: String,
    val apiKeyQueryParam: String?,
    val extraHeaders: Map<String, String>,
    val streaming: Boolean,
    val temperature: Double?,
    val maxTokens: Int?,
    val isActive: Boolean,
    val createdAt: Long,
    val credentialId: String,
    /** Wire format used for this configuration (custom providers can pick any of the supported ones). */
    val requestStyle: RequestStyle = RequestStyle.OPENAI,
    /** Optional dotted JSON path used to read the reply text (custom / plain-format gateways). */
    val responseTextPath: String? = null
) {
    /** Full chat endpoint, e.g. `https://api.openai.com/v1/chat/completions`. */
    val chatEndpoint: String get() = joinUrl(baseUrl, chatPath)

    /** Connection-test endpoint. */
    val testEndpoint: String get() = joinUrl(baseUrl, testPath)

    companion object {
        fun joinUrl(base: String, path: String): String {
            val b = base.trim().trimEnd('/')
            val p = path.trim()
            if (p.isEmpty()) return b
            if (p.startsWith("http://") || p.startsWith("https://")) return p
            return if (p.startsWith("/")) "$b$p" else "$b/$p"
        }
    }
}
