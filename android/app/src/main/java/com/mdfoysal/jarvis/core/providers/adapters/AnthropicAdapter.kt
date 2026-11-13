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
import com.mdfoysal.jarvis.core.providers.Role
import com.mdfoysal.jarvis.core.providers.StreamEvent

/**
 * Anthropic Claude adapter — Messages API (`/v1/messages`), `x-api-key` + `anthropic-version`
 * headers, a dedicated `system` field and block-based content.
 */
class AnthropicAdapter(spec: ProviderSpec) : AiProviderAdapter {

    override val spec: ProviderSpec = spec

    override fun buildChatRequest(
        config: ProviderConfig,
        messages: List<ChatMessage>,
        options: ChatOptions
    ): HttpRequest {
        val system = messages.filter { it.role == Role.SYSTEM }
            .joinToString("\n\n") { it.text }
            .ifBlank { null }

        val builder = JsonObjectBuilder()
            .put("model", config.model)
            .put("max_tokens", options.maxTokens ?: DEFAULT_MAX_TOKENS)
            .put("stream", options.stream)
            .put(
                "messages",
                JsonValue.Arr(messages.filter { it.role != Role.SYSTEM }.map { messageToJson(it) })
            )
        system?.let { builder.put("system", it) }
        options.temperature?.let { builder.put("temperature", it) }

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
        val blocks = json.optArray("content")
        val text = blocks.mapNotNull { block ->
            when (block.optString("type")) {
                "text" -> block.optString("text")
                else -> null
            }
        }.joinToString("")
        return ProviderReply(
            text = text,
            stopReason = json.optString("stop_reason").takeIf { it.isNotBlank() },
            promptTokens = json.path("usage.input_tokens")?.asIntOrNull(),
            completionTokens = json.path("usage.output_tokens")?.asIntOrNull(),
            raw = json
        )
    }

    override fun parseStreamEvent(json: JsonValue): StreamEvent {
        when (json.optString("type")) {
            "content_block_delta" -> {
                val text = json.path("delta.text")?.asStringOrNull()
                if (!text.isNullOrEmpty()) return StreamEvent.Delta(text)
                return StreamEvent.Ignored
            }
            "message_stop" -> return StreamEvent.Completed(json.optString("stop_reason").ifBlank { null })
            "message_delta" -> {
                val stop = json.path("delta.stop_reason")?.asStringOrNull()
                if (!stop.isNullOrBlank()) return StreamEvent.Completed(stop)
                return StreamEvent.Ignored
            }
            "error" -> {
                val error = json.optObject("error")
                return StreamEvent.Failure(
                    ProviderError(
                        status = 0,
                        code = error["type"]?.asStringOrNull() ?: "anthropic_error",
                        message = error["message"]?.asStringOrNull() ?: "Anthropic returned an error"
                    )
                )
            }
            else -> return StreamEvent.Ignored
        }
    }

    override fun parseError(status: Int, body: String): ProviderError {
        val json = Json.parseOrNull(body)
        val error = json?.optObject("error")
        val message = error?.get("message")?.asStringOrNull()
            ?: json?.optString("message")?.takeIf { it.isNotBlank() }
            ?: body.take(300).ifBlank { "HTTP $status" }
        return ProviderError(
            status = status,
            code = error?.get("type")?.asStringOrNull() ?: "http_$status",
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

    private fun messageToJson(message: ChatMessage): JsonValue {
        val role = if (message.role == Role.ASSISTANT) "assistant" else "user"
        val blocks = ArrayList<JsonValue>()
        blocks.add(
            JsonValue.obj(
                "type" to JsonValue.Str("text"),
                "text" to JsonValue.Str(message.text)
            )
        )
        message.imageBase64?.takeIf { it.isNotBlank() }?.let { base64 ->
            blocks.add(
                JsonValue.obj(
                    "type" to JsonValue.Str("image"),
                    "source" to JsonValue.obj(
                        "type" to JsonValue.Str("base64"),
                        "media_type" to JsonValue.Str(message.imageMimeType ?: "image/png"),
                        "data" to JsonValue.Str(base64)
                    )
                )
            )
        }
        return JsonValue.obj(
            "role" to JsonValue.Str(role),
            "content" to JsonValue.Arr(blocks)
        )
    }

    companion object {
        const val DEFAULT_MAX_TOKENS = 2048
    }
}
