package verify

import com.mdfoysal.jarvis.core.chat.ChatEngine
import com.mdfoysal.jarvis.core.json.Json
import com.mdfoysal.jarvis.core.providers.ChatMessage
import com.mdfoysal.jarvis.core.providers.ChatOptions
import com.mdfoysal.jarvis.core.providers.ProviderConfig
import com.mdfoysal.jarvis.core.providers.ProviderRegistry
import com.mdfoysal.jarvis.core.providers.Role
import com.mdfoysal.jarvis.core.providers.StreamEvent
import com.mdfoysal.jarvis.core.providers.adapters.ProviderAdapters
import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import java.io.File
import java.net.InetSocketAddress
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking

/**
 * End-to-end verification against a real local HTTP server that speaks the real
 * OpenAI / Anthropic / Gemini wire protocols (including SSE streaming).
 */
object IntegrationChecks {

    private const val VALID_KEY = "test-key"

    fun run(providersJsonPath: String) {
        val specs = ProviderRegistry.parse(File(providersJsonPath).readText())
        val server = startServer()
        val base = "http://127.0.0.1:${server.address.port}"
        try {
            openAiRoundTrip(specs, base)
            anthropicRoundTrip(specs, base)
            geminiRoundTrip(specs, base)
            streamingRoundTrip(specs, base)
            errorHandling(specs, base)
        } finally {
            server.stop(0)
        }
    }

    private fun startServer(): HttpServer {
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/") { exchange -> handle(exchange) }
        server.executor = null
        server.start()
        return server
    }

    private fun handle(exchange: HttpExchange) {
        val path = exchange.requestURI.path
        val method = exchange.requestMethod
        val headers = exchange.requestHeaders
        val body = exchange.requestBody.bufferedReader().use { it.readText() }

        when {
            path.endsWith("/chat/completions") -> {
                if (headers.getFirst("Authorization") != "Bearer $VALID_KEY") {
                    respond(exchange, 401, """{"error":{"message":"Incorrect API key provided","code":"invalid_api_key"}}""")
                    return
                }
                val json = Json.parseOrNull(body)
                val stream = json?.optBoolean("stream") ?: false
                if (stream) {
                    exchange.responseHeaders.add("Content-Type", "text/event-stream")
                    exchange.sendResponseHeaders(200, 0)
                    val out = exchange.responseBody.bufferedWriter()
                    out.write("data: {\"choices\":[{\"delta\":{\"content\":\"Hello\"}}]}\n\n")
                    out.write("data: {\"choices\":[{\"delta\":{\"content\":\" world\"}}]}\n\n")
                    out.write("data: {\"choices\":[{\"delta\":{},\"finish_reason\":\"stop\"}]}\n\n")
                    out.write("data: [DONE]\n\n")
                    out.flush()
                    exchange.close()
                } else {
                    respond(
                        exchange,
                        200,
                        """{"choices":[{"message":{"role":"assistant","content":"Pong from OpenAI"},"finish_reason":"stop"}],"usage":{"prompt_tokens":8,"completion_tokens":4}}"""
                    )
                }
            }

            path.endsWith("/messages") -> {
                if (headers.getFirst("x-api-key") != VALID_KEY) {
                    respond(exchange, 401, """{"type":"error","error":{"type":"authentication_error","message":"invalid x-api-key"}}""")
                    return
                }
                if (headers.getFirst("anthropic-version").isNullOrBlank()) {
                    respond(exchange, 400, """{"type":"error","error":{"type":"invalid_request_error","message":"missing anthropic-version"}}""")
                    return
                }
                respond(
                    exchange,
                    200,
                    """{"content":[{"type":"text","text":"Pong from Claude"}],"stop_reason":"end_turn","usage":{"input_tokens":6,"output_tokens":3}}"""
                )
            }

            path.contains(":generateContent") -> {
                if (headers.getFirst("x-goog-api-key") != VALID_KEY) {
                    respond(exchange, 401, """{"error":{"code":401,"message":"API key not valid","status":"UNAUTHENTICATED"}}""")
                    return
                }
                respond(
                    exchange,
                    200,
                    """{"candidates":[{"content":{"parts":[{"text":"Pong from Gemini"}]},"finishReason":"STOP"}],"usageMetadata":{"promptTokenCount":5,"candidatesTokenCount":3}}"""
                )
            }

            path.endsWith("/models") -> {
                respond(exchange, 200, """{"data":[{"id":"model-1"}]}""")
            }

            else -> {
                respond(exchange, 404, """{"error":{"message":"not found"}}""")
            }
        }
    }

    private fun respond(exchange: HttpExchange, code: Int, payload: String) {
        val bytes = payload.toByteArray(Charsets.UTF_8)
        exchange.responseHeaders.add("Content-Type", "application/json")
        exchange.sendResponseHeaders(code, bytes.size.toLong())
        exchange.responseBody.use { it.write(bytes) }
        exchange.close()
    }

    private fun config(specId: String, specs: List<com.mdfoysal.jarvis.core.providers.ProviderSpec>, base: String): ProviderConfig {
        val spec = specs.first { it.id == specId }
        return ProviderRegistry.defaultConfig(spec, "cfg-$specId", "cred", 0L).copy(
            baseUrl = base,
            chatPath = when (specId) {
                "anthropic" -> "/messages"
                "gemini" -> "/v1beta/models/{model}:generateContent"
                else -> "/v1/chat/completions"
            },
            testPath = "/v1/models"
        )
    }

    private fun openAiRoundTrip(specs: List<com.mdfoysal.jarvis.core.providers.ProviderSpec>, base: String) {
        TestKit.suite("Integration — OpenAI protocol over real HTTP")
        val spec = specs.first { it.id == "openai" }
        val adapter = ProviderAdapters.create(spec)
        val cfg = config("openai", specs, base)
        val engine = ChatEngine(JdkHttpTransport())
        val reply = runBlocking {
            engine.chat(cfg, adapter, VALID_KEY, listOf(ChatMessage(Role.USER, "ping")), ChatOptions())
        }
        TestKit.checkEquals("Pong from OpenAI", reply.text, "OpenAI reply received over HTTP")
        TestKit.checkEquals(8, reply.promptTokens, "OpenAI usage parsed over HTTP")
    }

    private fun anthropicRoundTrip(specs: List<com.mdfoysal.jarvis.core.providers.ProviderSpec>, base: String) {
        TestKit.suite("Integration — Anthropic protocol over real HTTP")
        val spec = specs.first { it.id == "anthropic" }
        val adapter = ProviderAdapters.create(spec)
        val cfg = config("anthropic", specs, base)
        val engine = ChatEngine(JdkHttpTransport())
        val reply = runBlocking {
            engine.chat(cfg, adapter, VALID_KEY, listOf(ChatMessage(Role.USER, "ping")), ChatOptions())
        }
        TestKit.checkEquals("Pong from Claude", reply.text, "Anthropic reply received over HTTP")
    }

    private fun geminiRoundTrip(specs: List<com.mdfoysal.jarvis.core.providers.ProviderSpec>, base: String) {
        TestKit.suite("Integration — Gemini protocol over real HTTP")
        val spec = specs.first { it.id == "gemini" }
        val adapter = ProviderAdapters.create(spec)
        val cfg = config("gemini", specs, base)
        val engine = ChatEngine(JdkHttpTransport())
        val reply = runBlocking {
            engine.chat(cfg, adapter, VALID_KEY, listOf(ChatMessage(Role.USER, "ping")), ChatOptions())
        }
        TestKit.checkEquals("Pong from Gemini", reply.text, "Gemini reply received over HTTP")
    }

    private fun streamingRoundTrip(specs: List<com.mdfoysal.jarvis.core.providers.ProviderSpec>, base: String) {
        TestKit.suite("Integration — SSE streaming over real HTTP")
        val spec = specs.first { it.id == "openai" }
        val adapter = ProviderAdapters.create(spec)
        val cfg = config("openai", specs, base)
        val engine = ChatEngine(JdkHttpTransport())
        val events = runBlocking {
            engine.stream(cfg, adapter, VALID_KEY, listOf(ChatMessage(Role.USER, "ping"))).toList()
        }
        val text = events.filterIsInstance<StreamEvent.Delta>().joinToString("") { it.text }
        TestKit.checkEquals("Hello world", text, "streamed deltas reassemble into the full answer")
        TestKit.check(events.any { it is StreamEvent.Completed }, "stream reports completion")
    }

    private fun errorHandling(specs: List<com.mdfoysal.jarvis.core.providers.ProviderSpec>, base: String) {
        TestKit.suite("Integration — provider error handling")
        val spec = specs.first { it.id == "openai" }
        val adapter = ProviderAdapters.create(spec)
        val cfg = config("openai", specs, base)
        val engine = ChatEngine(JdkHttpTransport())
        var caught: com.mdfoysal.jarvis.core.chat.ProviderException? = null
        runBlocking {
            try {
                engine.chat(cfg, adapter, "wrong-key", listOf(ChatMessage(Role.USER, "ping")), ChatOptions())
            } catch (e: com.mdfoysal.jarvis.core.chat.ProviderException) {
                caught = e
            }
        }
        TestKit.check(caught != null, "invalid key raises a provider exception")
        TestKit.check(caught?.error?.isAuthError == true, "invalid key is reported as an auth error")
        TestKit.checkContains(caught?.error?.message.orEmpty(), "Incorrect API key", "provider error message surfaces to the user")
    }
}
