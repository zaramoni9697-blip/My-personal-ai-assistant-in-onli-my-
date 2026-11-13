package com.mdfoysal.jarvis.core.providers.adapters

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
import com.mdfoysal.jarvis.core.providers.Role
import com.mdfoysal.jarvis.core.providers.StreamEvent

/**
 * Base adapter for the OpenAI-compatible chat-completions format.
 *
 * Used by OpenAI, DeepSeek, Groq, OpenRouter, Mistral and the "Custom API" provider.
 * Each of those gets its own subclass ([ProviderAdapters]) so provider-specific tweaks
 * (extra headers, default models, quirks) stay in a dedicated class.
 */
open class OpenAiCompatibleAdapter(override val spec: ProviderSpec) : AiProviderAdapter {

    override fun buildChatRequest(
        config: ProviderConfig,
        messages: List<ChatMessage>,
        options: ChatOptions
    ): HttpRequest {
        val body = JsonObjectBuilder()
            .put("model", config.model)
            .put("stream", options.stream)
            .put("messages", JsonValue.Arr(messages.map { messageToJson(it) }))
        options.temperature?.let { body.put("temperature", it) }
        options.maxTokens?.let { body.put("max_tokens", it) }
        if (options.stream) {
            body.put("stream_options", JsonValue.obj("include_usage" to JsonValue.Bool(true)))
        }

        val headers = LinkedHashMap<String, String>()
        headers["Content-Type"] = "application/json"
        headers["Accept"] = if (options.stream) "text/event-stream" else "application/json"
        headers.putAll(spec.extraHeaders)
        headers.putAll(config.extraHeaders)

        return HttpRequest(
            url = endpoint(config, config.chatPath),
            method = "POST",
            headers = headers,
            body = body.build().let { com.mdfoysal.jarvis.core.json.Json.encode(it) }
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
        val choices = json.optArray("choices")
        val first = choices.firstOrNull()
        val messageText = first?.path("message.content")?.asStringOrNull()
        val deltaText = first?.path("delta.content")?.asStringOrNull()
        val legacyText = first?.optString("text").orEmpty()
        val text = messageText ?: deltaText ?: legacyText
        return ProviderReply(
            text = text,
            stopReason = first?.optString("finish_reason")?.takeIf { it.isNotBlank() },
            promptTokens = json.path("usage.prompt_tokens")?.asIntOrNull(),
            completionTokens = json.path("usage.completion_tokens")?.asIntOrNull(),
            raw = json
        )
    }

    override fun parseStreamEvent(json: JsonValue): StreamEvent {
        val error = json.optObject("error")
        if (error.isNotEmpty()) {
            val message = error["message"]?.asStringOrNull() ?: "Unknown provider error"
            return StreamEvent.Failure(
                ProviderError(
                    status = json.optInt("status", 0),
                    code = error["type"]?.asStringOrNull() ?: "stream_error",
                    message = message
                )
            )
        }
        val choices = json.optArray("choices")
        val first = choices.firstOrNull()
        val delta = first?.path("delta.content")?.asStringOrNull()
        if (!delta.isNullOrEmpty()) return StreamEvent.Delta(delta)
        val finish = first?.optString("finish_reason")
        if (!finish.isNullOrBlank()) return StreamEvent.Completed(finish)
        return StreamEvent.Ignored
    }

    override fun parseError(status: Int, body: String): ProviderError {
        val json = com.mdfoysal.jarvis.core.json.Json.parseOrNull(body)
        val error = json?.optObject("error")
        val message = error?.get("message")?.asStringOrNull()
            ?: json?.optString("message")?.takeIf { it.isNotBlank() }
            ?: body.take(300).ifBlank { "HTTP $status" }
        val code = error?.get("code")?.asStringOrNull()
            ?: error?.get("type")?.asStringOrNull()
            ?: "http_$status"
        return ProviderError(
            status = status,
            code = code,
            message = message,
            retryable = status == 429 || status >= 500
        )
    }

    override fun authHeaders(config: ProviderConfig, apiKey: String): Map<String, String> {
        if (apiKey.isBlank() || config.authType == AuthType.NONE) return emptyMap()
        return when (config.authType) {
            AuthType.BEARER -> mapOf(config.apiKeyHeader to "Bearer $apiKey")
            AuthType.X_API_KEY, AuthType.HEADER -> mapOf(config.apiKeyHeader to apiKey)
            AuthType.QUERY -> emptyMap()
            AuthType.NONE -> emptyMap()
        }
    }

    override fun endpoint(config: ProviderConfig, path: String): String =
        ProviderConfig.joinUrl(config.baseUrl, path)

    /** Converts a chat message into the provider's JSON shape. */
    protected open fun messageToJson(message: ChatMessage): JsonValue {
        val role = when (message.role) {
            Role.SYSTEM -> "system"
            Role.USER -> "user"
            Role.ASSISTANT -> "assistant"
        }
        val image = message.imageBase64
        return if (image.isNullOrBlank()) {
            JsonValue.obj("role" to JsonValue.Str(role), "content" to JsonValue.Str(message.text))
        } else {
            val mime = message.imageMimeType ?: "image/png"
            JsonValue.obj(
                "role" to JsonValue.Str(role),
                "content" to JsonValue.Arr(
                    listOf(
                        JsonValue.obj("type" to JsonValue.Str("text"), "text" to JsonValue.Str(message.text)),
                        JsonValue.obj(
                            "type" to JsonValue.Str("image_url"),
                            "image_url" to JsonValue.obj(
                                "url" to JsonValue.Str("data:$mime;base64,$image")
                            )
                        )
                    )
                )
            )
        }
    }
}
