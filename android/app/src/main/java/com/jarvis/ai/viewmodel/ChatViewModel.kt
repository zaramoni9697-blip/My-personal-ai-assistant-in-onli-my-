package com.jarvis.ai.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jarvis.ai.data.local.db.ConversationEntity
import com.jarvis.ai.data.local.db.MessageEntity
import com.jarvis.ai.data.local.datastore.PreferencesManager
import com.jarvis.ai.data.repository.ChatRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val repo: ChatRepository,
    private val prefs: PreferencesManager
) : ViewModel() {

    private val _userId = MutableStateFlow<String?>(null)
    private val _conversationId = MutableStateFlow<String?>(null)

    val conversations: StateFlow<List<ConversationEntity>> = _userId.filterNotNull().flatMapLatest { repo.observeConversations(it) }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val messages: StateFlow<List<MessageEntity>> = _conversationId.filterNotNull().flatMapLatest { repo.observeMessages(it) }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isThinking = MutableStateFlow(false)
    val isThinking: StateFlow<Boolean> = _isThinking
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    fun init(userId: String) { _userId.value = userId }

    suspend fun createConversation(title: String = "New Chat"): String {
        val uid = _userId.value ?: error("No user"); return ""
        // handled below with repo
    }

    fun selectConversation(id: String) { _conversationId.value = id }

    fun newConversation(title: String = "New Chat") {
        val uid = _userId.value ?: return
        viewModelScope.launch {
            val c = repo.createConversation(uid, title, null)
            _conversationId.value = c.id
        }
    }

    fun sendMessage(text: String) {
        val uid = _userId.value ?: return
        val cid = _conversationId.value ?: run {
            viewModelScope.launch {
                val c = repo.createConversation(uid, text.take(30), null)
                _conversationId.value = c.id
                actuallySend(uid, c.id, text)
            }
            return
        }
        viewModelScope.launch { actuallySend(uid, cid, text) }
    }

    private suspend fun actuallySend(uid: String, cid: String, text: String) {
        _isThinking.value = true
        _error.value = null
        val res = repo.sendMessage(uid, cid, text)
        if (res.isFailure) _error.value = res.exceptionOrNull()?.message
        _isThinking.value = false
    }

    fun clearError() { _error.value = null }
    fun deleteConversation(id: String) { viewModelScope.launch { repo.deleteConversation(id) } }
}
