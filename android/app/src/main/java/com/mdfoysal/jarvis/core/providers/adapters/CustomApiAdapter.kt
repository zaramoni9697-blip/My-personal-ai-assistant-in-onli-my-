package com.mdfoysal.jarvis.core.providers.adapters

import com.mdfoysal.jarvis.core.json.Json
import com.mdfoysal.jarvis.core.json.JsonObjectBuilder
import com.mdfoysal.jarvis.core.json.JsonValue
import com.mdfoysal.jarvis.core.net.HttpRequest
import com.mdfoysal.jarvis.core.providers.AiProviderAdapter
import com.mdfoysal.jarvis.core.providers.AuthType
import com.mdfoysal.jarvis.core.providers.ChatMessage
import com.mdfoysal.jarvis.core.providers.ChatOptions
import com.mdfoysal.jarvis.core.providers.ProviderConfig
import com.mdfoysal.jarvis.core.providers.ProviderError
import com.mdfoysal.jarvis.core.providers.ProviderReply
import com.mdfoysal.jarvis.core.providers.ProviderSpec
import com.mdfoysal.jarvis.core.providers.RequestStyle
import com.mdfoysal.jarvis.core.providers.Role
import com.mdfoysal.jarvis.core.providers.StreamEvent

/**
 * Adapter for the "Custom API" provider.
 *
 * The user can point JARVIS at any HTTP endpoint and choose which wire format to speak:
 *  - [RequestStyle.OPENAI]   → OpenAI chat-completions shape
 *  - [RequestStyle.ANTHROPIC]→ Anthropic Messages shape
 *  - [RequestStyle.GEMINI]   → Gemini generateContent shape
 *  - [RequestStyle.PLAIN]    → a simple `{model, messages:[{role,content}]}` shape with a
 *                              user-configurable path to the reply text (self-hosted gateways,
 *                              Ollama-style servers, internal LLM proxies).
 */
class CustomApiAdapter(providerSpec: ProviderSpec) : AiProviderAdapter {

    override val spec: ProviderSpec = providerSpec

    private fun delegate(style: RequestStyle): AiProviderAdapter = when (style) {
        RequestStyle.ANTHROPIC -> AnthropicAdapter(spec)
        RequestStyle.GEMINI -> GeminiAdapter(spec)
        RequestStyle.PLAIN -> PlainAdapter(spec)
        RequestStyle.OPENAI -> OpenAiCompatibleAdapter(spec)
    }

    override fun buildChatRequest(
        config: ProviderConfig,
        messages: List<ChatMessage>,
        options: ChatOptions
    ): HttpRequest = delegate(config.requestStyle).buildChatRequest(config, messages, options)

    override fun buildTestRequest(config: ProviderConfig): HttpRequest =
        delegate(config.requestStyle).buildTestRequest(config)

    override fun parseReply(json: JsonValue): ProviderReply =
        delegate(configStyleFallback).parseReply(json)

    override fun parseStreamEvent(json: JsonValue): StreamEvent =
        delegate(configStyleFallback).parseStreamEvent(json)

    override fun parseError(status: Int, body: String): ProviderError =
        delegate(configStyleFallback).parseError(status, body)

    override fun authHeaders(config: ProviderConfig, apiKey: String): Map<String, String> =
        when (config.authType) {
            AuthType.BEARER -> mapOf(config.apiKeyHeader to "Bearer $apiKey")
            AuthType.X_API_KEY, AuthType.HEADER -> mapOf(config.apiKeyHeader to apiKey)
            AuthType.QUERY, AuthType.NONE -> emptyMap()
        }

    override fun endpoint(config: ProviderConfig, path: String): String =
        ProviderConfig.joinUrl(config.baseUrl, path.replace("{model}", config.model))

    /** Used when a parsing call happens without a concrete config (e.g. error payloads). */
    private val configStyleFallback: RequestStyle get() = spec.requestStyle
}

/**
 * Plain/simple wire format for custom endpoints.
 *
 * Request: `{"model": ..., "messages": [{"role": "user", "content": "..."}]}`
 * Response text is read from [ProviderConfig.responseTextPath] when provided, otherwise the
 * adapter probes a list of common locations.
 */
class PlainAdapter(providerSpec: ProviderSpec) : AiProviderAdapter {

    override val spec: ProviderSpec = providerSpec

    override fun buildChatRequest(
        config: ProviderConfig,
        messages: List<ChatMessage>,
        options: ChatOptions
    ): HttpRequest {
        val builder = JsonObjectBuilder()
            .put("model", config.model)
            .put("stream", options.stream)
            .put(
                "messages",
                JsonValue.Arr(
                    messages.map { message ->
                        val role = when (message.role) {
                            Role.SYSTEM -> "system"
                            Role.USER -> "user"
                            Role.ASSISTANT -> "assistant"
                        }
                        JsonValue.obj(
                            "role" to JsonValue.Str(role),
                            "content" to JsonValue.Str(message.text)
                        )
                    }
                )
            )
        options.temperature?.let { builder.put("temperature", it) }
        options.maxTokens?.let { builder.put("max_tokens", it) }

        val headers = LinkedHashMap<String, String>()
        headers["Content-Type"] = "application/json"
        headers["Accept"] = if (options.stream) "text/event-stream" else "application/json"
        headers.putAll(spec.extraHeaders)
        headers.putAll(config.extraHeaders)

        return HttpRequest(
            url = endpoint(config, config.chatPath),
            method = "POST",
            headers = headers,
            body = Json.encode(builder.build())
        )
    }

    override fun buildTestRequest(config: ProviderConfig): HttpRequest {
        val headers = LinkedHashMap<String, String>()
        headers["Accept"] = "application/json"
        headers.putAll(spec.extraHeaders)
        headers.putAll(config.extraHeaders)
        return HttpRequest(
            url = endpoint(config, config.testPath),
            method = "GET",
            headers = headers
        )
    }

    override fun parseReply(json: JsonValue): ProviderReply {
        val text = readText(json)
        return ProviderReply(text = text, raw = json)
    }

    override fun parseStreamEvent(json: JsonValue): StreamEvent {
        val text = readText(json)
        return if (text.isNotEmpty()) StreamEvent.Delta(text) else StreamEvent.Ignored
    }

    override fun parseError(status: Int, body: String): ProviderError {
        val json = Json.parseOrNull(body)
        val message = json?.optString("message")?.takeIf { it.isNotBlank() }
            ?: json?.path("error.message")?.asStringOrNull()
            ?: body.take(300).ifBlank { "HTTP $status" }
        return ProviderError(status = status, code = "http_$status", message = message, retryable = status == 429 || status >= 500)
    }

    override fun authHeaders(config: ProviderConfig, apiKey: String): Map<String, String> =
        when (config.authType) {
            AuthType.BEARER -> mapOf(config.apiKeyHeader to "Bearer $apiKey")
            AuthType.X_API_KEY, AuthType.HEADER -> mapOf(config.apiKeyHeader to apiKey)
            AuthType.QUERY, AuthType.NONE -> emptyMap()
        }

    override fun endpoint(config: ProviderConfig, path: String): String =
        ProviderConfig.joinUrl(config.baseUrl, path.replace("{model}", config.model))

    private fun readText(json: JsonValue): String {
        val candidates = listOf(
            "choices.0.message.content",
            "message.content",
            "response",
            "output",
            "text",
            "content",
            "reply",
            "result"
        )
        for (path in candidates) {
            val value = json.path(path)?.asStringOrNull()
            if (!value.isNullOrBlank()) return value
        }
        return ""
    }
}
