package com.mdfoysal.jarvis.core.chat

import com.mdfoysal.jarvis.core.json.Json
import com.mdfoysal.jarvis.core.net.HttpRequest
import com.mdfoysal.jarvis.core.net.HttpTransport
import com.mdfoysal.jarvis.core.providers.AiProviderAdapter
import com.mdfoysal.jarvis.core.providers.ChatMessage
import com.mdfoysal.jarvis.core.providers.ChatOptions
import com.mdfoysal.jarvis.core.providers.ProviderConfig
import com.mdfoysal.jarvis.core.providers.ProviderError
import com.mdfoysal.jarvis.core.providers.ProviderReply
import com.mdfoysal.jarvis.core.providers.StreamEvent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/** Raised when a provider call fails. Carries the normalised [ProviderError]. */
class ProviderException(val error: ProviderError) : Exception(error.message)

/**
 * Transport-agnostic chat engine.
 *
 * It owns the provider-independent parts of a conversation (request dispatch, error mapping,
 * streaming decoding) and delegates every provider-specific detail to an [AiProviderAdapter].
 */
class ChatEngine(private val transport: HttpTransport) {

    /** Executes a single non-streaming chat completion. */
    suspend fun chat(
        config: ProviderConfig,
        adapter: AiProviderAdapter,
        apiKey: String,
        messages: List<ChatMessage>,
        options: ChatOptions = ChatOptions()
    ): ProviderReply {
        val request = adapter.applyAuth(
            config,
            apiKey,
            adapter.buildChatRequest(config, messages, options)
        )
        val response = transport.execute(request)
        if (!response.isSuccessful) {
            val body = response.body.ifBlank { response.errorMessage.orEmpty() }
            throw ProviderException(adapter.parseError(response.code, body))
        }
        val json = Json.parseOrNull(response.body)
            ?: throw ProviderException(
                ProviderError(response.code, "invalid_response", "Provider returned a response that is not valid JSON")
            )
        return adapter.parseReply(json)
    }

    /** Executes a streaming chat completion, emitting decoded [StreamEvent]s. */
    fun stream(
        config: ProviderConfig,
        adapter: AiProviderAdapter,
        apiKey: String,
        messages: List<ChatMessage>,
        options: ChatOptions = ChatOptions()
    ): Flow<StreamEvent> = flow {
        val request = adapter.applyAuth(
            config,
            apiKey,
            adapter.buildChatRequest(config, messages, options.copy(stream = true))
        )
        val decoder = SseDecoder()
        try {
            transport.stream(request).forEach { chunk ->
                decoder.feed(chunk).forEach { payload -> emitPayload(adapter, payload) }
            }
            decoder.flush().forEach { payload -> emitPayload(adapter, payload) }
        } catch (e: Exception) {
            throw ProviderException(
                ProviderError(0, "network_error", e.message ?: "Network error while streaming")
            )
        }
    }

    private suspend fun kotlinx.coroutines.flow.FlowCollector<StreamEvent>.emitPayload(
        adapter: AiProviderAdapter,
        payload: String
    ) {
        if (payload.isBlank() || payload.trim() == "[DONE]") return
        val json = Json.parseOrNull(payload) ?: return
        when (val event = adapter.parseStreamEvent(json)) {
            is StreamEvent.Delta -> emit(event)
            is StreamEvent.Completed -> emit(event)
            is StreamEvent.Failure -> throw ProviderException(event.error)
            StreamEvent.Ignored -> Unit
        }
    }

    /**
     * Verifies a configuration end-to-end with a minimal, cheap completion.
     * This proves the key, the model id and the endpoint all work together.
     */
    suspend fun testConnection(
        config: ProviderConfig,
        adapter: AiProviderAdapter,
        apiKey: String
    ): ProviderReply {
        val probe = listOf(
            ChatMessage(com.mdfoysal.jarvis.core.providers.Role.USER, "Reply with the single word: OK")
        )
        return chat(
            config = config,
            adapter = adapter,
            apiKey = apiKey,
            messages = probe,
            options = ChatOptions(temperature = 0.0, maxTokens = 16, stream = false)
        )
    }

    /** Sends a raw request — used by the diagnostics screen. */
    suspend fun executeRaw(request: HttpRequest): HttpRequest = request
}
