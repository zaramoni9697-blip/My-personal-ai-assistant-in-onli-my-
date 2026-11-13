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
 * Google Gemini adapter — `generateContent` payload shape, `x-goog-api-key` authentication,
 * `contents[]`/`systemInstruction` structure and Gemini's own usage metadata.
 */
class GeminiAdapter(spec: ProviderSpec) : AiProviderAdapter {

    override val spec: ProviderSpec = spec

    override fun buildChatRequest(
        config: ProviderConfig,
        messages: List<ChatMessage>,
        options: ChatOptions
    ): HttpRequest {
        val system = messages.filter { it.role == Role.SYSTEM }
            .joinToString("\n\n") { it.text }
            .ifBlank { null }

        val contents = messages.filter { it.role != Role.SYSTEM }.map { messageToJson(it) }

        val builder = JsonObjectBuilder().put("contents", JsonValue.Arr(contents))
        if (system != null) {
            builder.put(
                "systemInstruction",
                JsonValue.obj(
                    "parts" to JsonValue.Arr(
                        listOf(JsonValue.obj("text" to JsonValue.Str(system)))
                    )
                )
            )
        }
        val generationConfig = JsonObjectBuilder()
        options.temperature?.let { generationConfig.put("temperature", it) }
        options.maxTokens?.let { generationConfig.put("maxOutputTokens", it) }
        val configJson = generationConfig.build()
        if (configJson is JsonValue.Obj && configJson.fields.isNotEmpty()) {
            builder.put("generationConfig", configJson)
        }

        val headers = LinkedHashMap<String, String>()
        headers["Content-Type"] = "application/json"
        headers["Accept"] = if (options.stream) "text/event-stream" else "application/json"
        headers.putAll(spec.extraHeaders)
        headers.putAll(config.extraHeaders)

        val path = if (options.stream) {
            config.chatPath.replace(":generateContent", ":streamGenerateContent")
        } else {
            config.chatPath
        }

        return HttpRequest(
            url = endpoint(config, path) + if (options.stream) "?alt=sse" else "",
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
            url = endpoint(config, config.testPath.replace("{model}", config.model)),
            method = "GET",
            headers = headers
        )
    }

    override fun parseReply(json: JsonValue): ProviderReply {
        val candidate = json.optArray("candidates").firstOrNull()
        val parts = candidate?.path("content.parts")?.asArrayOrNull() ?: emptyList()
        val text = parts.mapNotNull { it.optString("text").ifBlank { null } }.joinToString("")
        return ProviderReply(
            text = text,
            stopReason = candidate?.optString("finishReason")?.takeIf { it.isNotBlank() },
            promptTokens = json.path("usageMetadata.promptTokenCount")?.asIntOrNull(),
            completionTokens = json.path("usageMetadata.candidatesTokenCount")?.asIntOrNull(),
            raw = json
        )
    }

    override fun parseStreamEvent(json: JsonValue): StreamEvent {
        val error = json.optObject("error")
        if (error.isNotEmpty()) {
            return StreamEvent.Failure(
                ProviderError(
                    status = json.optInt("code", 0),
                    code = error["status"]?.asStringOrNull() ?: "gemini_error",
                    message = error["message"]?.asStringOrNull() ?: "Gemini returned an error"
                )
            )
        }
        val candidate = json.optArray("candidates").firstOrNull()
        val parts = candidate?.path("content.parts")?.asArrayOrNull() ?: emptyList()
        val text = parts.mapNotNull { it.optString("text").ifBlank { null } }.joinToString("")
        if (text.isNotEmpty()) return StreamEvent.Delta(text)
        val finish = candidate?.optString("finishReason")
        if (!finish.isNullOrBlank()) return StreamEvent.Completed(finish)
        return StreamEvent.Ignored
    }

    override fun parseError(status: Int, body: String): ProviderError {
        val json = Json.parseOrNull(body)
        val error = json?.optObject("error")
        val message = error?.get("message")?.asStringOrNull()
            ?: json?.optString("message")?.takeIf { it.isNotBlank() }
            ?: body.take(300).ifBlank { "HTTP $status" }
        return ProviderError(
            status = status,
            code = error?.get("status")?.asStringOrNull() ?: "http_$status",
            message = message,
            retryable = status == 429 || status >= 500
        )
    }

    override fun authHeaders(config: ProviderConfig, apiKey: String): Map<String, String> {
        if (apiKey.isBlank() || config.authType == AuthType.NONE) return emptyMap()
        return when (config.authType) {
            AuthType.QUERY -> emptyMap()
            AuthType.BEARER -> mapOf(config.apiKeyHeader to "Bearer $apiKey")
            else -> mapOf(config.apiKeyHeader to apiKey)
        }
    }

    override fun applyAuth(
        config: ProviderConfig,
        apiKey: String,
        request: HttpRequest
    ): HttpRequest {
        if (apiKey.isBlank() || config.authType == AuthType.NONE) return request
        if (config.authType == AuthType.QUERY) {
            val param = config.apiKeyQueryParam ?: "key"
            val separator = if (request.url.contains("?")) "&" else "?"
            return request.copy(url = "${request.url}$separator$param=${encodeQuery(apiKey)}")
        }
        val headers = LinkedHashMap(request.headers)
        headers.putAll(authHeaders(config, apiKey))
        return request.copy(headers = headers)
    }

    override fun endpoint(config: ProviderConfig, path: String): String =
        ProviderConfig.joinUrl(config.baseUrl, path.replace("{model}", config.model))

    private fun messageToJson(message: ChatMessage): JsonValue {
        // Gemini only accepts "user" and "model" roles.
        val role = if (message.role == Role.ASSISTANT) "model" else "user"
        val parts = ArrayList<JsonValue>()
        parts.add(JsonValue.obj("text" to JsonValue.Str(message.text)))
        message.imageBase64?.takeIf { it.isNotBlank() }?.let { base64 ->
            parts.add(
                JsonValue.obj(
                    "inline_data" to JsonValue.obj(
                        "mime_type" to JsonValue.Str(message.imageMimeType ?: "image/png"),
                        "data" to JsonValue.Str(base64)
                    )
                )
            )
        }
        return JsonValue.obj("role" to JsonValue.Str(role), "parts" to JsonValue.Arr(parts))
    }

    private fun encodeQuery(value: String): String =
        value.replace(" ", "%20").replace("+", "%2B").replace("&", "%26")
}
