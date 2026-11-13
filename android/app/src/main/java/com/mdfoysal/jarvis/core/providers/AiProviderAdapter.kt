package com.mdfoysal.jarvis.core.providers

import com.mdfoysal.jarvis.core.json.JsonValue
import com.mdfoysal.jarvis.core.net.HttpRequest

/** Role of a chat message. */
enum class Role { SYSTEM, USER, ASSISTANT }

/** One message of a conversation. */
data class ChatMessage(
    val role: Role,
    val text: String,
    /** Optional base64 image (used by providers that support image input). */
    val imageBase64: String? = null,
    /** Optional MIME type of [imageBase64], e.g. `image/png`. */
    val imageMimeType: String? = null
)

/** Tunable generation options. */
data class ChatOptions(
    val temperature: Double? = null,
    val maxTokens: Int? = null,
    val stream: Boolean = false
)

/** A successful provider response. */
data class ProviderReply(
    val text: String,
    val stopReason: String? = null,
    val promptTokens: Int? = null,
    val completionTokens: Int? = null,
    /** Chain-of-thought text when a provider exposes it separately (DeepSeek, OpenRouter). */
    val reasoning: String? = null,
    val raw: JsonValue? = null
)

/** A normalised provider error (identical shape for every provider). */
data class ProviderError(
    val status: Int,
    val code: String,
    val message: String,
    val retryable: Boolean = false
) {
    val isAuthError: Boolean get() = status == 401 || status == 403 || code.contains("api_key", true)
    val isRateLimited: Boolean get() = status == 429
}

/** A single event decoded from a provider's streaming response. */
sealed class StreamEvent {
    data class Delta(val text: String) : StreamEvent()
    data class Completed(val stopReason: String?) : StreamEvent()
    data class Failure(val error: ProviderError) : StreamEvent()
    object Ignored : StreamEvent()
}

/**
 * Contract implemented by every provider adapter.
 *
 * Each adapter owns the request shape, the response parsing, the error mapping and the
 * connection test for its provider — nothing is assumed to be "the same for everybody".
 */
interface AiProviderAdapter {

    /** Static description of the provider this adapter serves. */
    val spec: ProviderSpec

    /** Builds a fully-formed chat request (URL, headers, JSON body). */
    fun buildChatRequest(
        config: ProviderConfig,
        messages: List<ChatMessage>,
        options: ChatOptions
    ): HttpRequest

    /** Builds the lightweight request used by the "Test connection" button. */
    fun buildTestRequest(config: ProviderConfig): HttpRequest

    /** Parses a non-streaming JSON response body. */
    fun parseReply(json: JsonValue): ProviderReply

    /** Parses one SSE `data:` payload into a [StreamEvent]. */
    fun parseStreamEvent(json: JsonValue): StreamEvent

    /** Maps an HTTP error body into a normalised [ProviderError]. */
    fun parseError(status: Int, body: String): ProviderError

    /** Headers that carry the credential for this provider. */
    fun authHeaders(config: ProviderConfig, apiKey: String): Map<String, String>

    /** Fully-qualified endpoint for a given path. */
    fun endpoint(config: ProviderConfig, path: String): String

    /**
     * Applies the credential to a request. Header-based providers override [authHeaders];
     * query-parameter providers (rare, but supported for custom gateways) override this method.
     */
    fun applyAuth(config: ProviderConfig, apiKey: String, request: HttpRequest): HttpRequest {
        val headers = LinkedHashMap(request.headers)
        headers.putAll(authHeaders(config, apiKey))
        return request.copy(headers = headers)
    }
}
