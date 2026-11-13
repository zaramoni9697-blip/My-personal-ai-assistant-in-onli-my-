package com.mdfoysal.jarvis.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mdfoysal.jarvis.data.local.SettingsStore
import com.mdfoysal.jarvis.data.local.entity.MemoryEntity
import com.mdfoysal.jarvis.data.repository.AuthRepository
import com.mdfoysal.jarvis.data.repository.AuthResult
import com.mdfoysal.jarvis.data.repository.ChatRepository
import com.mdfoysal.jarvis.data.repository.ProviderRepository
import com.mdfoysal.jarvis.data.repository.SettingsRepository
import com.mdfoysal.jarvis.data.security.SessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** State of the settings screen. */
data class SettingsUiState(
    val settings: SettingsStore.Settings = SettingsStore.Settings(),
    val displayName: String = "",
    val email: String = "",
    val memories: List<MemoryEntity> = emptyList(),
    val isLoading: Boolean = true,
    val message: String? = null,
    val error: String? = null
)

class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val authRepository: AuthRepository,
    private val providerRepository: ProviderRepository,
    private val chatRepository: ChatRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _state = MutableStateFlow(SettingsUiState())
    val state: StateFlow<SettingsUiState> = _state.asStateFlow()

    private var userId: String = ""

    val settingsFlow: StateFlow<SettingsStore.Settings> = settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsStore.Settings())

    init {
        viewModelScope.launch {
            settingsRepository.settings.collect { settings ->
                _state.update { it.copy(settings = settings) }
            }
        }
        viewModelScope.launch {
            val session = sessionManager.session.first()
            if (session == null) return@launch
            userId = session.userId
            _state.update {
                it.copy(displayName = session.displayName, email = session.email, isLoading = false)
            }
            chatRepository.observeMemories(session.userId).collect { memories ->
                _state.update { it.copy(memories = memories) }
            }
        }
    }

    fun setLanguage(value: String) = viewModelScope.launch { settingsRepository.setLanguage(value) }
    fun setTtsEnabled(value: Boolean) = viewModelScope.launch { settingsRepository.setTtsEnabled(value) }
    fun setTtsRate(value: Float) = viewModelScope.launch { settingsRepository.setTtsRate(value) }
    fun setVoiceMode(value: Boolean) = viewModelScope.launch { settingsRepository.setVoiceMode(value) }
    fun setWakeWordEnabled(value: Boolean) = viewModelScope.launch {
        settingsRepository.setWakeWordEnabled(value)
    }
    fun setWakeWordPhrase(value: String) = viewModelScope.launch {
        settingsRepository.setWakeWordPhrase(value)
    }
    fun setMemoryEnabled(value: Boolean) = viewModelScope.launch {
        settingsRepository.setMemoryEnabled(value)
    }
    fun setServerUrl(value: String) = viewModelScope.launch { settingsRepository.setServerUrl(value) }
    fun setRemoteAuth(value: Boolean) = viewModelScope.launch { settingsRepository.setRemoteAuth(value) }
    fun setHapticEnabled(value: Boolean) = viewModelScope.launch { settingsRepository.setHapticEnabled(value) }

    fun addMemory(content: String) = viewModelScope.launch {
        chatRepository.addMemory(userId, content)
        _state.update { it.copy(message = "Memory saved") }
    }

    fun deleteMemory(id: String) = viewModelScope.launch { chatRepository.deleteMemory(id) }

    fun updateDisplayName(name: String) = viewModelScope.launch {
        authRepository.updateDisplayName(name)
        _state.update { it.copy(displayName = name, message = "Profile updated") }
    }

    fun logout(onDone: () -> Unit) = viewModelScope.launch {
        authRepository.logout()
        onDone()
    }

    fun deleteAccount(onDone: () -> Unit) = viewModelScope.launch {
        when (val result = authRepository.deleteAccount()) {
            is AuthResult.Success -> onDone()
            is AuthResult.Failure -> _state.update { it.copy(error = result.message) }
        }
    }

    fun clearMessages() = _state.update { it.copy(message = null, error = null) }
}
