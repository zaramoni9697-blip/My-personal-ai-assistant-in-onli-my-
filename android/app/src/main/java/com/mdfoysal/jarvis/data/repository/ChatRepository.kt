package com.mdfoysal.jarvis.data.repository

import com.mdfoysal.jarvis.core.chat.ChatEngine
import com.mdfoysal.jarvis.core.chat.ProviderException
import com.mdfoysal.jarvis.core.persona.JarvisPersona
import com.mdfoysal.jarvis.core.providers.ChatMessage
import com.mdfoysal.jarvis.core.providers.ChatOptions
import com.mdfoysal.jarvis.core.providers.ProviderConfig
import com.mdfoysal.jarvis.core.providers.Role
import com.mdfoysal.jarvis.core.providers.adapters.ProviderAdapters
import com.mdfoysal.jarvis.core.text.AssistantLanguage
import com.mdfoysal.jarvis.core.text.LanguageDetector
import com.mdfoysal.jarvis.data.local.SettingsStore
import com.mdfoysal.jarvis.data.local.dao.ConversationDao
import com.mdfoysal.jarvis.data.local.dao.MemoryDao
import com.mdfoysal.jarvis.data.local.dao.MessageDao
import com.mdfoysal.jarvis.data.local.entity.ConversationEntity
import com.mdfoysal.jarvis.data.local.entity.MemoryEntity
import com.mdfoysal.jarvis.data.local.entity.MessageEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.util.UUID

/** Everything the chat screen needs to render one turn. */
data class ChatTurn(
    val userMessage: MessageEntity,
    val assistantMessage: MessageEntity
)

/**
 * Owns the conversation history, the memory and the call to the active AI provider.
 */
class ChatRepository(
    private val conversationDao: ConversationDao,
    private val messageDao: MessageDao,
    private val memoryDao: MemoryDao,
    private val providerRepository: ProviderRepository,
    private val chatEngine: ChatEngine,
    private val settingsStore: SettingsStore
) {

    fun observeConversations(userId: String): Flow<List<ConversationEntity>> =
        conversationDao.observeForUser(userId)

    fun observeMessages(conversationId: String): Flow<List<MessageEntity>> =
        messageDao.observeForConversation(conversationId)

    suspend fun messages(conversationId: String): List<MessageEntity> =
        messageDao.listForConversation(conversationId)

    /** Returns the user's newest conversation, creating one when needed. */
    suspend fun ensureConversation(userId: String, providerConfigId: String?): ConversationEntity {
        val existing = conversationDao.latestForUser(userId)
        if (existing != null && !existing.isArchived) return existing
        val now = System.currentTimeMillis()
        val conversation = ConversationEntity(
            id = UUID.randomUUID().toString(),
            userId = userId,
            title = "New conversation",
            providerConfigId = providerConfigId,
            createdAt = now,
            updatedAt = now
        )
        conversationDao.insert(conversation)
        return conversation
    }

    suspend fun persistUserMessage(
        conversationId: String,
        text: String,
        imageBase64: String? = null,
        imageMimeType: String? = null
    ): MessageEntity {
        val message = MessageEntity(
            id = UUID.randomUUID().toString(),
            conversationId = conversationId,
            role = Role.USER.name,
            text = text,
            createdAt = System.currentTimeMillis(),
            imageBase64 = imageBase64,
            imageMimeType = imageMimeType
        )
        messageDao.insert(message)
        conversationDao.touch(conversationId, System.currentTimeMillis(), titleFor(text))
        return message
    }

    suspend fun persistAssistantMessage(
        conversationId: String,
        text: String,
        errorMessage: String? = null
    ): MessageEntity {
        val message = MessageEntity(
            id = UUID.randomUUID().toString(),
            conversationId = conversationId,
            role = Role.ASSISTANT.name,
            text = text,
            createdAt = System.currentTimeMillis(),
            errorMessage = errorMessage
        )
        messageDao.insert(message)
        conversationDao.touch(conversationId, System.currentTimeMillis(), null)
        return message
    }

    /**
     * Builds the prompt: the JARVIS system prompt, the user's memories and the recent history.
     */
    suspend fun buildPromptMessages(
        userId: String,
        conversationId: String,
        userName: String?,
        includeCurrentUserTurn: Boolean = true
    ): List<ChatMessage> {
        val settings = settingsStore.settings.first()
        val memory = if (settings.memoryEnabled) memoryDao.limitForUser(userId, MEMORY_LIMIT) else emptyList()
        val language = detectLanguage(settings.language)
        val system = JarvisPersona.systemPrompt(language, memory.map { it.content }, userName)

        val history = messageDao.recentForConversation(conversationId, HISTORY_LIMIT)
        val messages = ArrayList<ChatMessage>()
        messages.add(ChatMessage(Role.SYSTEM, system))
        history.forEach { entity ->
            val role = when (entity.role) {
                Role.ASSISTANT.name -> Role.ASSISTANT
                else -> Role.USER
            }
            messages.add(
                ChatMessage(
                    role = role,
                    text = entity.text,
                    imageBase64 = entity.imageBase64,
                    imageMimeType = entity.imageMimeType
                )
            )
        }
        if (!includeCurrentUserTurn) {
            messages.removeLastOrNull()
        }
        return messages
    }

    /** Sends a message to the active provider and stores the answer. */
    suspend fun complete(
        userId: String,
        conversationId: String,
        userName: String?,
        providerConfig: ProviderConfig,
        apiKey: String,
        options: ChatOptions = ChatOptions()
    ): Result<String> {
        val spec = providerRepository.specFor(providerConfig.providerId)
        val adapter = ProviderAdapters.create(
            spec ?: com.mdfoysal.jarvis.core.providers.ProviderSpec(
                id = providerConfig.providerId,
                label = providerConfig.nickname,
                description = "",
                requestStyle = providerConfig.requestStyle,
                defaultBaseUrl = providerConfig.baseUrl,
                defaultChatPath = providerConfig.chatPath,
                defaultTestPath = providerConfig.testPath,
                authType = providerConfig.authType,
                apiKeyHeader = providerConfig.apiKeyHeader,
                apiKeyQueryParam = providerConfig.apiKeyQueryParam,
                defaultModel = providerConfig.model,
                suggestedModels = emptyList(),
                supportsStreaming = providerConfig.streaming,
                supportsImages = false,
                customizable = true,
                extraHeaders = providerConfig.extraHeaders
            )
        )
        val messages = buildPromptMessages(userId, conversationId, userName)
        return try {
            val reply = chatEngine.chat(
                config = providerConfig,
                adapter = adapter,
                apiKey = apiKey,
                messages = messages,
                options = options.copy(stream = false)
            )
            if (reply.text.isBlank()) {
                Result.failure(IllegalStateException("The provider returned an empty answer"))
            } else {
                Result.success(reply.text)
            }
        } catch (e: ProviderException) {
            Result.failure(ProviderFailure(e.error.message, e.error.code, e.error.retryable))
        } catch (e: Exception) {
            Result.failure(ProviderFailure(e.message ?: "Something went wrong", "unknown", true))
        }
    }

    /** Streaming variant — used by the voice mode so the first words arrive quickly. */
    fun stream(
        providerConfig: ProviderConfig,
        apiKey: String,
        messages: List<ChatMessage>
    ): Flow<String> {
        val spec = providerRepository.specFor(providerConfig.providerId)
        val adapter = ProviderAdapters.create(
            spec ?: com.mdfoysal.jarvis.core.providers.ProviderSpec(
                id = providerConfig.providerId,
                label = providerConfig.nickname,
                description = "",
                requestStyle = providerConfig.requestStyle,
                defaultBaseUrl = providerConfig.baseUrl,
                defaultChatPath = providerConfig.chatPath,
                defaultTestPath = providerConfig.testPath,
                authType = providerConfig.authType,
                apiKeyHeader = providerConfig.apiKeyHeader,
                apiKeyQueryParam = providerConfig.apiKeyQueryParam,
                defaultModel = providerConfig.model,
                suggestedModels = emptyList(),
                supportsStreaming = providerConfig.streaming,
                supportsImages = false,
                customizable = true,
                extraHeaders = providerConfig.extraHeaders
            )
        )
        return chatEngine.stream(
            config = providerConfig,
            adapter = adapter,
            apiKey = apiKey,
            messages = messages,
            options = ChatOptions(stream = true)
        )
    }

    suspend fun deleteConversation(conversationId: String) {
        messageDao.deleteForConversation(conversationId)
        conversationDao.deleteById(conversationId)
    }

    suspend fun clearMessages(conversationId: String) {
        messageDao.deleteForConversation(conversationId)
    }

    // ------------------------------------------------------------------ memory

    fun observeMemories(userId: String): Flow<List<MemoryEntity>> = memoryDao.observeForUser(userId)

    suspend fun memories(userId: String): List<MemoryEntity> = memoryDao.limitForUser(userId, MEMORY_LIMIT)

    suspend fun addMemory(userId: String, content: String) {
        if (content.isBlank()) return
        memoryDao.insert(
            MemoryEntity(
                id = UUID.randomUUID().toString(),
                userId = userId,
                content = content.trim(),
                createdAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun deleteMemory(id: String) = memoryDao.deleteById(id)

    suspend fun setMemoryPinned(id: String, pinned: Boolean) = memoryDao.setPinned(id, pinned)

    private fun detectLanguage(preference: String): AssistantLanguage = when (preference) {
        "bn" -> AssistantLanguage.BENGALI
        "en" -> AssistantLanguage.ENGLISH
        else -> AssistantLanguage.MIXED
    }

    private fun titleFor(text: String): String {
        val trimmed = text.trim().replace(Regex("\\s+"), " ")
        return if (trimmed.length <= 40) trimmed else trimmed.take(40) + "…"
    }

    companion object {
        const val HISTORY_LIMIT = 20
        const val MEMORY_LIMIT = 10
    }
}

/** Normalised provider failure that the UI can present to the user. */
data class ProviderFailure(
    val message: String,
    val code: String,
    val retryable: Boolean
) : Exception(message)
