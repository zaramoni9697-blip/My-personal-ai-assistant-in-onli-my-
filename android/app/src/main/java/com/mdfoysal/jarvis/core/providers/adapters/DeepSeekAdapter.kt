package com.mdfoysal.jarvis.core.providers.adapters

import com.mdfoysal.jarvis.core.json.Json
import com.mdfoysal.jarvis.core.json.JsonValue
import com.mdfoysal.jarvis.core.net.HttpRequest
import com.mdfoysal.jarvis.core.providers.ChatMessage
import com.mdfoysal.jarvis.core.providers.ChatOptions
import com.mdfoysal.jarvis.core.providers.ProviderConfig
import com.mdfoysal.jarvis.core.providers.ProviderReply
import com.mdfoysal.jarvis.core.providers.ProviderSpec

/**
 * DeepSeek adapter.
 *
 * DeepSeek speaks the OpenAI dialect but has two quirks handled here:
 *  - the `deepseek-reasoner` model rejects an explicit `temperature`,
 *  - reasoning traces come back in a separate `reasoning_content` field.
 */
class DeepSeekAdapter(spec: ProviderSpec) : OpenAiCompatibleAdapter(spec) {

    override fun buildChatRequest(
        config: ProviderConfig,
        messages: List<ChatMessage>,
        options: ChatOptions
    ): HttpRequest {
        val effective = if (isReasoner(config.model)) options.copy(temperature = null) else options
        return super.buildChatRequest(config, messages, effective)
    }

    override fun parseReply(json: JsonValue): ProviderReply {
        val reply = super.parseReply(json)
        val reasoning = json.path("choices.0.message.reasoning_content")?.asStringOrNull()
        return reply.copy(reasoning = reasoning?.ifBlank { null })
    }

    private fun isReasoner(model: String): Boolean =
        model.lowercase().contains("reasoner")
}

/**
 * Groq adapter — OpenAI-compatible with Groq's own model catalogue and quotas.
 */
class GroqAdapter(spec: ProviderSpec) : OpenAiCompatibleAdapter(spec) {
    // Groq uses the standard OpenAI Chat Completions payload; only defaults differ (see spec).
}

/**
 * Mistral adapter — OpenAI-compatible; Mistral requires `max_tokens` on most models.
 */
class MistralAdapter(spec: ProviderSpec) : OpenAiCompatibleAdapter(spec) {

    override fun buildChatRequest(
        config: ProviderConfig,
        messages: List<ChatMessage>,
        options: ChatOptions
    ): HttpRequest {
        val effective = if (options.maxTokens == null) options.copy(maxTokens = DEFAULT_MAX_TOKENS) else options
        return super.buildChatRequest(config, messages, effective)
    }

    companion object {
        const val DEFAULT_MAX_TOKENS = 1024
    }
}

/**
 * OpenRouter adapter.
 *
 * OpenRouter proxies many vendors; it expects optional attribution headers and model ids in
 * the `vendor/model` form. Error payloads are `{"error": {"code": 402, "message": "..."}}`.
 */
class OpenRouterAdapter(spec: ProviderSpec) : OpenAiCompatibleAdapter(spec) {

    override fun parseReply(json: JsonValue): ProviderReply {
        val reply = super.parseReply(json)
        val reasoning = json.path("choices.0.message.reasoning")?.asStringOrNull()
        return reply.copy(reasoning = reasoning?.ifBlank { null })
    }
}
