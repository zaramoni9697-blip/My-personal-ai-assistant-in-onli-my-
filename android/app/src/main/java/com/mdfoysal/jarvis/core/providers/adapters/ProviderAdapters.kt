package com.mdfoysal.jarvis.core.providers.adapters

import com.mdfoysal.jarvis.core.providers.AiProviderAdapter
import com.mdfoysal.jarvis.core.providers.ProviderSpec
import com.mdfoysal.jarvis.core.providers.RequestStyle

/**
 * Factory that maps a provider id to its dedicated adapter.
 *
 * Each supported provider keeps its own adapter class so provider-specific request shapes,
 * authentication quirks and response parsing stay isolated from one another.
 */
object ProviderAdapters {

    fun create(spec: ProviderSpec): AiProviderAdapter = when (spec.id.lowercase()) {
        "gemini" -> GeminiAdapter(spec)
        "anthropic" -> AnthropicAdapter(spec)
        "openai" -> OpenAiAdapter(spec)
        "deepseek" -> DeepSeekAdapter(spec)
        "groq" -> GroqAdapter(spec)
        "openrouter" -> OpenRouterAdapter(spec)
        "mistral" -> MistralAdapter(spec)
        "custom" -> CustomApiAdapter(spec)
        else -> byStyle(spec, spec.requestStyle)
    }

    /** Builds an adapter for an arbitrary style — used by the "Custom API" provider. */
    fun byStyle(spec: ProviderSpec, style: RequestStyle): AiProviderAdapter = when (style) {
        RequestStyle.ANTHROPIC -> AnthropicAdapter(spec)
        RequestStyle.GEMINI -> GeminiAdapter(spec)
        RequestStyle.PLAIN -> PlainAdapter(spec)
        RequestStyle.OPENAI -> OpenAiCompatibleAdapter(spec)
    }
}
