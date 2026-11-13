package verify

import com.mdfoysal.jarvis.core.json.Json
import com.mdfoysal.jarvis.core.providers.AiProviderAdapter
import com.mdfoysal.jarvis.core.providers.ChatMessage
import com.mdfoysal.jarvis.core.providers.ChatOptions
import com.mdfoysal.jarvis.core.providers.ProviderConfig
import com.mdfoysal.jarvis.core.providers.ProviderRegistry
import com.mdfoysal.jarvis.core.providers.Role
import com.mdfoysal.jarvis.core.providers.adapters.ProviderAdapters
import java.io.File

object ProviderChecks {

    fun run(providersJsonPath: String) {
        val specs = ProviderRegistry.parse(File(providersJsonPath).readText())
        TestKit.suite("Provider adapters — request shapes")
        specs.forEach { spec ->
            val adapter = ProviderAdapters.create(spec)
            val config = ProviderRegistry.defaultConfig(spec, "cfg-${spec.id}", "cred-${spec.id}", 0L)
            val request = adapter.buildChatRequest(
                config,
                listOf(
                    ChatMessage(Role.SYSTEM, "You are JARVIS AI, created by Md Foysal."),
                    ChatMessage(Role.USER, "Hello")
                ),
                ChatOptions(temperature = 0.4, maxTokens = 256)
            )
            TestKit.check(request.method == "POST", "${spec.id}: chat request uses POST")
            TestKit.check(request.url.startsWith("https://") || spec.id == "custom", "${spec.id}: chat URL is absolute")
            TestKit.check(request.body != null && request.body!!.isNotEmpty(), "${spec.id}: chat request has a JSON body")
            TestKit.check(request.headers["Content-Type"] == "application/json", "${spec.id}: sets JSON content type")

            val authorised = adapter.applyAuth(config, "TEST-KEY", request)
            when (spec.authType) {
                com.mdfoysal.jarvis.core.providers.AuthType.BEARER ->
                    TestKit.checkEquals("Bearer TEST-KEY", authorised.headers["Authorization"], "${spec.id}: bearer auth applied")
                com.mdfoysal.jarvis.core.providers.AuthType.X_API_KEY ->
                    TestKit.checkEquals("TEST-KEY", authorised.headers["x-api-key"], "${spec.id}: x-api-key auth applied")
                com.mdfoysal.jarvis.core.providers.AuthType.HEADER ->
                    TestKit.checkEquals("TEST-KEY", authorised.headers[spec.apiKeyHeader], "${spec.id}: header auth applied")
                else -> Unit
            }

            val test = adapter.buildTestRequest(config)
            TestKit.check(test.method == "GET", "${spec.id}: test request uses GET")
        }

        specificShapeChecks(specs)
        responseParsingChecks(specs)
    }

    private fun adapterFor(specId: String, specs: List<com.mdfoysal.jarvis.core.providers.ProviderSpec>): AiProviderAdapter =
        ProviderAdapters.create(specs.first { it.id == specId })

    private fun specificShapeChecks(specs: List<com.mdfoysal.jarvis.core.providers.ProviderSpec>) {
        TestKit.suite("Provider adapters — protocol specifics")

        val openai = adapterFor("openai", specs)
        val openaiCfg = ProviderRegistry.defaultConfig(specs.first { it.id == "openai" }, "c1", "k1", 0L)
        val openaiBody = Json.parse(
            openai.buildChatRequest(openaiCfg, listOf(ChatMessage(Role.USER, "hi")), ChatOptions()).body!!
        )
        TestKit.checkEquals("gpt-4o-mini", openaiBody.optString("model"), "OpenAI: model id sent")
        TestKit.checkEquals("user", openaiBody.path("messages.0.role")?.asStringOrNull(), "OpenAI: role sent")
        TestKit.checkEquals("hi", openaiBody.path("messages.0.content")?.asStringOrNull(), "OpenAI: content sent")

        val gemini = adapterFor("gemini", specs)
        val geminiCfg = ProviderRegistry.defaultConfig(specs.first { it.id == "gemini" }, "c2", "k2", 0L)
        val geminiRequest = gemini.buildChatRequest(
            geminiCfg,
            listOf(
                ChatMessage(Role.SYSTEM, "sys"),
                ChatMessage(Role.USER, "hello gemini"),
                ChatMessage(Role.ASSISTANT, "hi")
            ),
            ChatOptions()
        )
        val geminiBody = Json.parse(geminiRequest.body!!)
        TestKit.check(
            geminiRequest.url.contains("/models/gemini-2.0-flash:generateContent"),
            "Gemini: model is interpolated into the endpoint"
        )
        TestKit.checkEquals("sys", geminiBody.path("systemInstruction.parts.0.text")?.asStringOrNull(), "Gemini: system instruction mapped")
        TestKit.checkEquals("user", geminiBody.path("contents.0.role")?.asStringOrNull(), "Gemini: user role mapped")
        TestKit.checkEquals("model", geminiBody.path("contents.1.role")?.asStringOrNull(), "Gemini: assistant role mapped to 'model'")

        val anthropic = adapterFor("anthropic", specs)
        val anthropicCfg = ProviderRegistry.defaultConfig(specs.first { it.id == "anthropic" }, "c3", "k3", 0L)
        val anthropicRequest = anthropic.buildChatRequest(
            anthropicCfg,
            listOf(ChatMessage(Role.SYSTEM, "sys"), ChatMessage(Role.USER, "hi")),
            ChatOptions()
        )
        val anthropicBody = Json.parse(anthropicRequest.body!!)
        TestKit.checkEquals("sys", anthropicBody.optString("system"), "Anthropic: system prompt in its own field")
        TestKit.check(anthropicBody.optInt("max_tokens") > 0, "Anthropic: max_tokens always present")
        TestKit.checkEquals("text", anthropicBody.path("messages.0.content.0.type")?.asStringOrNull(), "Anthropic: content blocks used")

        val deepseek = adapterFor("deepseek", specs)
        val reasonerCfg = ProviderRegistry.defaultConfig(specs.first { it.id == "deepseek" }, "c4", "k4", 0L)
            .copy(model = "deepseek-reasoner")
        val reasonerBody = Json.parse(
            deepseek.buildChatRequest(reasonerCfg, listOf(ChatMessage(Role.USER, "hi")), ChatOptions(temperature = 0.9)).body!!
        )
        TestKit.check(reasonerBody.field("temperature") == null, "DeepSeek: temperature dropped for the reasoner model")

        val custom = adapterFor("custom", specs)
        val customCfg = ProviderRegistry.defaultConfig(specs.first { it.id == "custom" }, "c5", "k5", 0L)
            .copy(
                baseUrl = "https://llm.internal.example",
                chatPath = "/v1/chat/completions",
                testPath = "/v1/models",
                model = "internal-model",
                requestStyle = com.mdfoysal.jarvis.core.providers.RequestStyle.PLAIN
            )
        val customRequest = custom.buildChatRequest(customCfg, listOf(ChatMessage(Role.USER, "ping")), ChatOptions())
        TestKit.checkEquals("https://llm.internal.example/v1/chat/completions", customRequest.url, "Custom: endpoint composed from base + path")
        TestKit.checkEquals(
            "internal-model",
            Json.parse(customRequest.body!!).optString("model"),
            "Custom: model id sent"
        )
    }

    private fun responseParsingChecks(specs: List<com.mdfoysal.jarvis.core.providers.ProviderSpec>) {
        TestKit.suite("Provider adapters — response parsing")

        val openai = adapterFor("openai", specs)
        val openaiReply = openai.parseReply(
            Json.parse("""{"choices":[{"message":{"role":"assistant","content":"Hello there"},"finish_reason":"stop"}],"usage":{"prompt_tokens":11,"completion_tokens":5}}""")
        )
        TestKit.checkEquals("Hello there", openaiReply.text, "OpenAI: reply text parsed")
        TestKit.checkEquals("stop", openaiReply.stopReason, "OpenAI: finish reason parsed")
        TestKit.checkEquals(11, openaiReply.promptTokens, "OpenAI: prompt tokens parsed")

        val anthropic = adapterFor("anthropic", specs)
        val anthropicReply = anthropic.parseReply(
            Json.parse("""{"content":[{"type":"text","text":"Hi "},{"type":"text","text":"there"}],"stop_reason":"end_turn","usage":{"input_tokens":7,"output_tokens":3}}""")
        )
        TestKit.checkEquals("Hi there", anthropicReply.text, "Anthropic: text blocks joined")
        TestKit.checkEquals("end_turn", anthropicReply.stopReason, "Anthropic: stop reason parsed")

        val gemini = adapterFor("gemini", specs)
        val geminiReply = gemini.parseReply(
            Json.parse("""{"candidates":[{"content":{"parts":[{"text":"Hello "},{"text":"Gemini"}]},"finishReason":"STOP"}],"usageMetadata":{"promptTokenCount":9,"candidatesTokenCount":4}}""")
        )
        TestKit.checkEquals("Hello Gemini", geminiReply.text, "Gemini: parts joined")
        TestKit.checkEquals("STOP", geminiReply.stopReason, "Gemini: finish reason parsed")

        TestKit.suite("Provider adapters — error mapping")
        val openaiError = openai.parseError(401, """{"error":{"message":"Incorrect API key provided","code":"invalid_api_key","type":"invalid_request_error"}}""")
        TestKit.checkEquals(401, openaiError.status, "OpenAI: error status mapped")
        TestKit.check(openaiError.isAuthError, "OpenAI: auth error detected")
        TestKit.checkContains(openaiError.message, "Incorrect API key", "OpenAI: error message mapped")

        val anthropicError = anthropic.parseError(429, """{"type":"error","error":{"type":"rate_limit_error","message":"Too many requests"}}""")
        TestKit.check(anthropicError.isRateLimited, "Anthropic: rate limit detected")
        TestKit.check(anthropicError.retryable, "Anthropic: rate limit marked retryable")

        val geminiError = gemini.parseError(400, """{"error":{"code":400,"message":"API key not valid","status":"INVALID_ARGUMENT"}}""")
        TestKit.checkContains(geminiError.message, "API key not valid", "Gemini: error message mapped")
    }
}
