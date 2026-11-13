package com.jarvis.ai.data.repository

import com.jarvis.ai.data.local.db.ConversationDao
import com.jarvis.ai.data.local.db.ConversationEntity
import com.jarvis.ai.data.local.db.MessageDao
import com.jarvis.ai.data.local.db.MessageEntity
import com.jarvis.ai.network.provider.ChatMessage
import com.jarvis.ai.network.provider.ProviderAdapterFactory
import com.jarvis.ai.network.provider.ProviderType
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatRepository @Inject constructor(
    private val convDao: ConversationDao,
    private val msgDao: MessageDao,
    private val providerRepo: ProviderRepository
) {
    fun observeConversations(userId: String): Flow<List<ConversationEntity>> = convDao.observeForUser(userId)
    fun observeMessages(convId: String): Flow<List<MessageEntity>> = msgDao.observeForConversation(convId)

    suspend fun createConversation(userId: String, title: String, providerId: String?): ConversationEntity {
        val c = ConversationEntity(userId = userId, title = title.take(80), providerId = providerId)
        convDao.insert(c); return c
    }

    suspend fun deleteConversation(id: String) { convDao.deleteById(id) }

    suspend fun sendMessage(
        userId: String,
        conversationId: String,
        userMessage: String,
        onDelta: ((String)->Unit)? = null
    ): Result<MessageEntity> {
        // save user message
        val userMsg = MessageEntity(conversationId = conversationId, role = "user", content = userMessage)
        msgDao.insert(userMsg)
        convDao.touch(conversationId)

        // get active provider
        val provider = providerRepo.getActive(userId)
            ?: return Result.failure(Exception("No active AI provider. Please configure a provider first."))

        val apiKey = providerRepo.getDecryptedKey(provider.id)
            ?: return Result.failure(Exception("API key missing for ${provider.displayName}"))

        val type = ProviderType.values().find { it.id == provider.providerType } ?: ProviderType.CUSTOM
        val adapter = ProviderAdapterFactory.get(type)

        // load history
        val history = msgDao.getForConversation(conversationId)
            .filter { it.role != "system" }
            .map { ChatMessage(it.role, it.content) }

        // inject persona handling for creator identity
        val enrichedHistory = handleCreatorIdentity(history, userMessage)

        val baseUrl = provider.baseUrl?.ifBlank { null } ?: type.defaultBaseUrl
        val response = adapter.chat(apiKey, provider.modelId, baseUrl, enrichedHistory)

        return if (response.success) {
            val assistantMsg = MessageEntity(
                conversationId = conversationId, role = "assistant",
                content = response.content, providerType = provider.providerType, modelId = provider.modelId
            )
            msgDao.insert(assistantMsg)
            convDao.touch(conversationId)
            Result.success(assistantMsg)
        } else {
            val errMsg = MessageEntity(conversationId = conversationId, role = "assistant", content = "⚠️ Error: ${response.error ?: "Unknown"} \nTry switching provider or check your API configuration.")
            msgDao.insert(errMsg)
            Result.failure(Exception(response.error))
        }
    }

    private fun handleCreatorIdentity(history: List<ChatMessage>, lastUserMsg: String): List<ChatMessage> {
        val lower = lastUserMsg.lowercase()
        val isCreatorQuestion = lower.contains("who created you") || lower.contains("who made you") ||
                lower.contains("তোমাকে কে তৈরি করেছে") || lower.contains("you were created by whom")
        // Adapter already includes system prompt; no need to mutate history unless needed.
        return history
    }

    suspend fun clearHistory(conversationId: String) = msgDao.deleteForConversation(conversationId)
}
