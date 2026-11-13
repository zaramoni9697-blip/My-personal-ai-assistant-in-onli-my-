package com.jarvis.ai.network.provider

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.HeaderMap
import retrofit2.http.POST
import retrofit2.http.Url
import java.util.concurrent.TimeUnit

// --- Common models ---
data class ChatMessage(val role: String, val content: String)
data class ChatRequest(
    val messages: List<ChatMessage>,
    val model: String,
    val temperature: Double = 0.7,
    val maxTokens: Int = 2048,
    val stream: Boolean = false
)
data class ChatResponse(
    val success: Boolean,
    val content: String,
    val model: String?,
    val provider: String,
    val usage: TokenUsage? = null,
    val error: String? = null
)
data class TokenUsage(val promptTokens: Int?, val completionTokens: Int?, val totalTokens: Int?)

// Provider type enum
enum class ProviderType(val id: String, val displayName: String, val defaultBaseUrl: String, val defaultModel: String) {
    GEMINI("gemini", "Google Gemini", "https://generativelanguage.googleapis.com/v1beta/", "gemini-1.5-flash"),
    OPENAI("openai", "OpenAI", "https://api.openai.com/v1/", "gpt-4o-mini"),
    CLAUDE("claude", "Anthropic Claude", "https://api.anthropic.com/v1/", "claude-3-5-sonnet-20241022"),
    DEEPSEEK("deepseek", "DeepSeek", "https://api.deepseek.com/v1/", "deepseek-chat"),
    GROQ("groq", "Groq", "https://api.groq.com/openai/v1/", "llama-3.1-8b-instant"),
    OPENROUTER("openrouter", "OpenRouter", "https://openrouter.ai/api/v1/", "openai/gpt-4o-mini"),
    MISTRAL("mistral", "Mistral", "https://api.mistral.ai/v1/", "mistral-large-latest"),
    CUSTOM("custom", "Custom API", "", "custom-model")
}

// System prompt for JARVIS persona
const val JARVIS_SYSTEM_PROMPT = """
You are JARVIS AI, a friendly, intelligent, professional personal AI assistant created by Md Foysal.
- Personality: helpful, respectful, natural, a little humorous when appropriate.
- Languages: Bengali and English. Detect user language automatically and respond in same language.
- If asked who created you, answer: "I was created by Md Foysal." In Bengali: "আমাকে তৈরি করেছেন Md Foysal।"
- Never claim to be created by anyone else.
- Be concise but thorough. Help with coding, math, translation, summarization, writing, problem solving.
"""

// --- Adapter interface ---
interface ProviderAdapter {
    val providerType: ProviderType
    suspend fun chat(apiKey: String, model: String, baseUrl: String, messages: List<ChatMessage>): ChatResponse
    suspend fun testConnection(apiKey: String, model: String, baseUrl: String): Result<String>
}

// --- Base HTTP client ---
fun createHttpClient(): OkHttpClient = OkHttpClient.Builder()
    .connectTimeout(30, TimeUnit.SECONDS)
    .readTimeout(60, TimeUnit.SECONDS)
    .writeTimeout(30, TimeUnit.SECONDS)
    .addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC })
    .build()

// --- OpenAI-compatible adapter (works for OpenAI, DeepSeek, Groq, OpenRouter, Mistral, Custom) ---
interface OpenAICompatApi {
    @POST
    suspend fun chatCompletions(@Url url: String, @HeaderMap headers: Map<String,String>, @Body body: Map<String,Any>): Map<String,Any>
}

class OpenAICompatibleAdapter(override val providerType: ProviderType) : ProviderAdapter {
    private val gson = Gson()
    private val client = createHttpClient()

    override suspend fun chat(apiKey: String, model: String, baseUrl: String, messages: List<ChatMessage>): ChatResponse = withContext(Dispatchers.IO) {
        try {
            val retrofit = Retrofit.Builder().baseUrl(if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/").client(client).addConverterFactory(GsonConverterFactory.create()).build()
            val api = retrofit.create(OpenAICompatApi::class.java)
            val allMessages = listOf(ChatMessage("system", JARVIS_SYSTEM_PROMPT)) + messages
            val body = mapOf(
                "model" to model,
                "messages" to allMessages.map { mapOf("role" to it.role, "content" to it.content) },
                "temperature" to 0.7,
                "max_tokens" to 2048
            )
            val headers = mutableMapOf("Authorization" to "Bearer $apiKey", "Content-Type" to "application/json")
            if (providerType == ProviderType.OPENROUTER) {
                headers["HTTP-Referer"] = "https://jarvis-ai.app"
                headers["X-Title"] = "JARVIS AI"
            }
            val endpoint = if (baseUrl.contains("openai") || baseUrl.contains("groq") || baseUrl.contains("deepseek") || baseUrl.contains("openrouter") || baseUrl.contains("mistral") || providerType == ProviderType.CUSTOM) "chat/completions" else "chat/completions"
            @Suppress("UNCHECKED_CAST")
            val res = api.chatCompletions(endpoint, headers, body)
            val choices = res["choices"] as? List<Map<String,Any>>
            val message = (choices?.firstOrNull()?.get("message") as? Map<String,Any>)?.get("content") as? String ?: ""
            val usageMap = res["usage"] as? Map<String,Any>
            ChatResponse(true, message, model, providerType.id,
                usage = usageMap?.let { TokenUsage((it["prompt_tokens"] as? Double)?.toInt(), (it["completion_tokens"] as? Double)?.toInt(), (it["total_tokens"] as? Double)?.toInt()) })
        } catch (e: Exception) {
            ChatResponse(false, "", null, providerType.id, error = e.message ?: "Unknown error")
        }
    }

    override suspend fun testConnection(apiKey: String, model: String, baseUrl: String): Result<String> {
        val res = chat(apiKey, model, baseUrl, listOf(ChatMessage("user", "Hello, respond with just 'OK' if you can hear me.")))
        return if (res.success) Result.success("Connection successful: ${res.content.take(100)}") else Result.failure(Exception(res.error))
    }
}

// --- Gemini Adapter ---
class GeminiAdapter : ProviderAdapter {
    override val providerType = ProviderType.GEMINI
    private val client = createHttpClient()
    private val gson = Gson()

    override suspend fun chat(apiKey: String, model: String, baseUrl: String, messages: List<ChatMessage>): ChatResponse = withContext(Dispatchers.IO) {
        try {
            val base = if (baseUrl.isBlank()) ProviderType.GEMINI.defaultBaseUrl else baseUrl
            val normalizedBase = if (base.endsWith("/")) base else "$base/"
            val retrofit = Retrofit.Builder().baseUrl(normalizedBase).client(client).addConverterFactory(GsonConverterFactory.create()).build()
            val api = retrofit.create(GeminiApi::class.java)
            // Gemini uses contents array
            val geminiMessages = messages.map { mapOf("role" to if (it.role == "assistant") "model" else "user", "parts" to listOf(mapOf("text" to it.content))) }
            val body = mapOf(
                "contents" to geminiMessages,
                "systemInstruction" to mapOf("parts" to listOf(mapOf("text" to JARVIS_SYSTEM_PROMPT))),
                "generationConfig" to mapOf("temperature" to 0.7, "maxOutputTokens" to 2048)
            )
            val url = "models/$model:generateContent?key=$apiKey"
            val res: Map<String,Any> = api.generateContent(url, body)
            @Suppress("UNCHECKED_CAST")
            val candidates = res["candidates"] as? List<Map<String,Any>>
            val content = (candidates?.firstOrNull()?.get("content") as? Map<String,Any>)?.let { c ->
                (c["parts"] as? List<Map<String,Any>>)?.firstOrNull()?.get("text") as? String
            } ?: ""
            ChatResponse(true, content, model, providerType.id)
        } catch (e: Exception) {
            ChatResponse(false, "", null, providerType.id, error = e.message)
        }
    }
    override suspend fun testConnection(apiKey: String, model: String, baseUrl: String): Result<String> {
        val r = chat(apiKey, model, baseUrl, listOf(ChatMessage("user","Hi, reply OK")))
        return if (r.success) Result.success("Gemini connected: ${r.content.take(80)}") else Result.failure(Exception(r.error))
    }
}
interface GeminiApi { @POST suspend fun generateContent(@Url url: String, @Body body: Map<String,Any>): Map<String,Any> }

// --- Claude Adapter ---
class ClaudeAdapter : ProviderAdapter {
    override val providerType = ProviderType.CLAUDE
    private val client = createHttpClient()
    override suspend fun chat(apiKey: String, model: String, baseUrl: String, messages: List<ChatMessage>): ChatResponse = withContext(Dispatchers.IO) {
        try {
            val base = if (baseUrl.isBlank()) ProviderType.CLAUDE.defaultBaseUrl else baseUrl
            val nb = if (base.endsWith("/")) base else "$base/"
            val retrofit = Retrofit.Builder().baseUrl(nb).client(client).addConverterFactory(GsonConverterFactory.create()).build()
            val api = retrofit.create(ClaudeApi::class.java)
            val body = mapOf(
                "model" to model,
                "max_tokens" to 2048,
                "system" to JARVIS_SYSTEM_PROMPT,
                "messages" to messages.filter { it.role != "system" }.map { mapOf("role" to it.role, "content" to it.content) }
            )
            val headers = mapOf("x-api-key" to apiKey, "anthropic-version" to "2023-06-01", "Content-Type" to "application/json")
            val res: Map<String,Any> = api.createMessage("messages", headers, body)
            @Suppress("UNCHECKED_CAST")
            val contentArr = res["content"] as? List<Map<String,Any>>
            val text = contentArr?.firstOrNull()?.get("text") as? String ?: ""
            ChatResponse(true, text, model, providerType.id)
        } catch (e: Exception) { ChatResponse(false, "", null, providerType.id, error = e.message) }
    }
    override suspend fun testConnection(apiKey: String, model: String, baseUrl: String): Result<String> {
        val r = chat(apiKey, model, baseUrl, listOf(ChatMessage("user","Hello")))
        return if (r.success) Result.success("Claude connected") else Result.failure(Exception(r.error))
    }
}
interface ClaudeApi { @POST suspend fun createMessage(@Url url: String, @HeaderMap headers: Map<String,String>, @Body body: Map<String,Any>): Map<String,Any> }

// --- Factory ---
object ProviderAdapterFactory {
    fun get(type: ProviderType): ProviderAdapter = when(type) {
        ProviderType.GEMINI -> GeminiAdapter()
        ProviderType.CLAUDE -> ClaudeAdapter()
        else -> OpenAICompatibleAdapter(type)
    }
    fun fromId(id: String): ProviderAdapter {
        val t = ProviderType.values().find { it.id == id } ?: ProviderType.CUSTOM
        return get(t)
    }
}
