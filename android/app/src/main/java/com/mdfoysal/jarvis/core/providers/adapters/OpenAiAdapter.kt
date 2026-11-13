package com.mdfoysal.jarvis.core.providers.adapters

import com.mdfoysal.jarvis.core.providers.ProviderSpec

/**
 * OpenAI adapter.
 *
 * Uses the standard Chat Completions format. Kept as its own class (instead of a bare
 * generic adapter) so OpenAI-specific behaviour has a home: `max_completion_tokens` support
 * for reasoning models and multi-part (image) content.
 */
class OpenAiAdapter(spec: ProviderSpec) : OpenAiCompatibleAdapter(spec) {

    override fun buildChatRequest(
        config: com.mdfoysal.jarvis.core.providers.ProviderConfig,
        messages: List<com.mdfoysal.jarvis.core.providers.ChatMessage>,
        options: com.mdfoysal.jarvis.core.providers.ChatOptions
    ): com.mdfoysal.jarvis.core.net.HttpRequest {
        val request = super.buildChatRequest(config, messages, options)
        // Reasoning models (o-series / gpt-5 family) reject `max_tokens` in favour of
        // `max_completion_tokens`; send the field the model understands.
        if (isReasoningModel(config.model) && options.maxTokens != null) {
            val body = com.mdfoysal.jarvis.core.json.Json.parse(request.body ?: "{}")
                .let { it as? com.mdfoysal.jarvis.core.json.JsonValue.Obj }
            if (body != null) {
                val fields = LinkedHashMap(body.fields)
                fields.remove("max_tokens")
                fields["max_completion_tokens"] = com.mdfoysal.jarvis.core.json.JsonValue.Num(
                    options.maxTokens.toDouble()
                )
                return request.copy(
                    body = com.mdfoysal.jarvis.core.json.Json.encode(
                        com.mdfoysal.jarvis.core.json.JsonValue.Obj(fields)
                    )
                )
            }
        }
        return request
    }

    private fun isReasoningModel(model: String): Boolean {
        val m = model.lowercase()
        return m.startsWith("o1") || m.startsWith("o3") || m.startsWith("o4") ||
            m.startsWith("gpt-5") || m.contains("reasoning")
    }
}
