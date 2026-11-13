package com.mdfoysal.jarvis.feature.providers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mdfoysal.jarvis.core.providers.AuthType
import com.mdfoysal.jarvis.core.providers.ProviderConfig
import com.mdfoysal.jarvis.core.providers.ProviderSpec
import com.mdfoysal.jarvis.core.providers.RequestStyle
import com.mdfoysal.jarvis.data.repository.ConnectionTestResult
import com.mdfoysal.jarvis.data.repository.ProviderDraft
import com.mdfoysal.jarvis.data.repository.ProviderRepository
import com.mdfoysal.jarvis.data.repository.SettingsRepository
import com.mdfoysal.jarvis.data.security.SessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** State of the provider list. */
data class ProvidersUiState(
    val specs: List<ProviderSpec> = emptyList(),
    val configs: List<ProviderConfig> = emptyList(),
    val activeConfigId: String? = null,
    val isLoading: Boolean = true,
    val error: String? = null
)

/** State of the provider editor form. */
data class ProviderFormState(
    val configId: String? = null,
    val providerId: String = "openai",
    val nickname: String = "",
    val baseUrl: String = "",
    val chatPath: String = "",
    val testPath: String = "",
    val model: String = "",
    val apiKey: String = "",
    val apiKeyVisible: Boolean = false,
    val authType: AuthType = AuthType.BEARER,
    val apiKeyHeader: String = "Authorization",
    val apiKeyQueryParam: String? = null,
    val extraHeaders: Map<String, String> = emptyMap(),
    val streaming: Boolean = true,
    val temperature: String = "",
    val maxTokens: String = "",
    val requestStyle: RequestStyle = RequestStyle.OPENAI,
    val responseTextPath: String = "",
    val isSaving: Boolean = false,
    val isTesting: Boolean = false,
    val error: String? = null,
    val message: String? = null
)

class ProvidersViewModel(
    private val providerRepository: ProviderRepository,
    private val settingsRepository: SettingsRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _state = MutableStateFlow(ProvidersUiState())
    val state: StateFlow<ProvidersUiState> = _state.asStateFlow()

    private val _form = MutableStateFlow(ProviderFormState())
    val form: StateFlow<ProviderFormState> = _form.asStateFlow()

    private var userId: String? = null

    init {
        viewModelScope.launch {
            val session = sessionManager.session.first()
            userId = session?.userId
            _state.update { it.copy(specs = providerRepository.specs()) }
            if (session != null) {
                collectConfigurations(session.userId)
            }
        }
    }

    private fun collectConfigurations(ownerId: String) {
        viewModelScope.launch {
            providerRepository.observeConfigs(ownerId).collect { configs ->
                _state.update { current ->
                    current.copy(
                        configs = configs,
                        activeConfigId = configs.firstOrNull { it.isActive }?.id,
                        isLoading = false
                    )
                }
            }
        }
    }

    /** Loads a provider into the form (pass `null` to create a new configuration). */
    fun loadForEditing(providerId: String, configId: String?) {
        val ownerId = userId ?: return
        viewModelScope.launch {
            val spec = providerRepository.specFor(providerId)
            val existing = configId?.let { providerRepository.configById(it) }
            if (existing != null) {
                _form.value = ProviderFormState(
                    configId = existing.id,
                    providerId = existing.providerId,
                    nickname = existing.nickname,
                    baseUrl = existing.baseUrl,
                    chatPath = existing.chatPath,
                    testPath = existing.testPath,
                    model = existing.model,
                    apiKey = "",
                    authType = existing.authType,
                    apiKeyHeader = existing.apiKeyHeader,
                    apiKeyQueryParam = existing.apiKeyQueryParam,
                    extraHeaders = existing.extraHeaders,
                    streaming = existing.streaming,
                    temperature = existing.temperature?.toString().orEmpty(),
                    maxTokens = existing.maxTokens?.toString().orEmpty(),
                    requestStyle = existing.requestStyle,
                    responseTextPath = existing.responseTextPath.orEmpty()
                )
            } else if (spec != null) {
                _form.value = ProviderFormState(
                    providerId = spec.id,
                    nickname = spec.label,
                    baseUrl = spec.defaultBaseUrl,
                    chatPath = spec.defaultChatPath,
                    testPath = spec.defaultTestPath,
                    model = spec.defaultModel,
                    authType = spec.authType,
                    apiKeyHeader = spec.apiKeyHeader,
                    apiKeyQueryParam = spec.apiKeyQueryParam,
                    extraHeaders = spec.extraHeaders,
                    streaming = spec.supportsStreaming,
                    requestStyle = spec.requestStyle
                )
            }
        }
    }

    fun onProviderSelected(providerId: String) {
        val spec = providerRepository.specFor(providerId) ?: return
        val current = _form.value
        if (current.configId != null) {
            // Keep the user's custom values when switching provider on an existing config.
            _form.update {
                it.copy(
                    providerId = providerId,
                    authType = spec.authType,
                    apiKeyHeader = spec.apiKeyHeader,
                    apiKeyQueryParam = spec.apiKeyQueryParam,
                    requestStyle = spec.requestStyle
                )
            }
        } else {
            _form.value = ProviderFormState(
                providerId = spec.id,
                nickname = spec.label,
                baseUrl = spec.defaultBaseUrl,
                chatPath = spec.defaultChatPath,
                testPath = spec.defaultTestPath,
                model = spec.defaultModel,
                authType = spec.authType,
                apiKeyHeader = spec.apiKeyHeader,
                apiKeyQueryParam = spec.apiKeyQueryParam,
                extraHeaders = spec.extraHeaders,
                streaming = spec.supportsStreaming,
                requestStyle = spec.requestStyle
            )
        }
    }

    fun onNicknameChange(value: String) = _form.update { it.copy(nickname = value, error = null) }
    fun onBaseUrlChange(value: String) = _form.update { it.copy(baseUrl = value, error = null) }
    fun onChatPathChange(value: String) = _form.update { it.copy(chatPath = value, error = null) }
    fun onTestPathChange(value: String) = _form.update { it.copy(testPath = value, error = null) }
    fun onModelChange(value: String) = _form.update { it.copy(model = value, error = null) }
    fun onApiKeyChange(value: String) = _form.update { it.copy(apiKey = value, error = null) }
    fun onAuthTypeChange(value: AuthType) = _form.update { it.copy(authType = value) }
    fun onRequestStyleChange(value: RequestStyle) = _form.update { it.copy(requestStyle = value) }
    fun onTemperatureChange(value: String) = _form.update { it.copy(temperature = value) }
    fun onMaxTokensChange(value: String) = _form.update { it.copy(maxTokens = value) }
    fun onResponseTextPathChange(value: String) = _form.update { it.copy(responseTextPath = value) }
    fun toggleApiKeyVisibility() = _form.update { it.copy(apiKeyVisible = !it.apiKeyVisible) }
    fun toggleStreaming() = _form.update { it.copy(streaming = !it.streaming) }
    fun clearMessages() = _form.update { it.copy(error = null, message = null) }

    /** Saves the configuration (creates or updates). */
    fun save() {
        val ownerId = userId ?: return
        val current = _form.value
        if (current.isSaving) return
        _form.update { it.copy(isSaving = true, error = null) }
        viewModelScope.launch {
            val draft = current.toDraft()
            val result = current.configId?.let { configId ->
                val existing = providerRepository.configById(configId)
                if (existing == null) {
                    Result.failure(IllegalStateException("This configuration no longer exists"))
                } else {
                    providerRepository.update(ownerId, existing, draft)
                }
            } ?: providerRepository.create(ownerId, draft)

            result.fold(
                onSuccess = {
                    _form.update { state ->
                        state.copy(isSaving = false, message = "Configuration saved", apiKey = "")
                    }
                },
                onFailure = { error ->
                    _form.update { it.copy(isSaving = false, error = error.message ?: "Could not save") }
                }
            )
        }
    }

    /** Runs a real connection test with the values currently in the form. */
    fun testConnection() {
        val ownerId = userId ?: return
        val current = _form.value
        if (current.isTesting) return
        _form.update { it.copy(isTesting = true, error = null, message = null) }
        viewModelScope.launch {
            val draft = current.toDraft()
            val config = current.configId?.let { providerRepository.configById(it) }
                ?: ProviderConfig(
                    id = "draft",
                    providerId = draft.providerId,
                    nickname = draft.nickname,
                    baseUrl = draft.baseUrl,
                    chatPath = draft.chatPath,
                    testPath = draft.testPath,
                    model = draft.model,
                    authType = draft.authType,
                    apiKeyHeader = draft.apiKeyHeader,
                    apiKeyQueryParam = draft.apiKeyQueryParam,
                    extraHeaders = draft.extraHeaders,
                    streaming = draft.streaming,
                    temperature = draft.temperature,
                    maxTokens = draft.maxTokens,
                    isActive = false,
                    createdAt = System.currentTimeMillis(),
                    credentialId = "draft",
                    requestStyle = draft.requestStyle,
                    responseTextPath = draft.responseTextPath
                )
            // For a brand new configuration the key only exists in the form, so it is
            // stored temporarily in the vault and removed again after the test.
            val temporaryId = "test_${System.currentTimeMillis()}"
            val testConfig = if (current.configId == null) {
                providerRepository.tempCredential(temporaryId, draft.apiKey)
                config.copy(credentialId = temporaryId)
            } else {
                if (draft.apiKey.isNotBlank()) {
                    providerRepository.tempCredential(config.credentialId, draft.apiKey)
                }
                config
            }

            when (val result = providerRepository.testConnection(testConfig)) {
                is ConnectionTestResult.Success -> _form.update {
                    it.copy(isTesting = false, message = result.message)
                }
                is ConnectionTestResult.Failure -> _form.update {
                    it.copy(isTesting = false, error = result.message)
                }
            }
            if (current.configId == null) {
                providerRepository.deleteTemporaryCredential(temporaryId)
            }
        }
    }

    /** Deletes a configuration. */
    fun delete(config: ProviderConfig) {
        viewModelScope.launch {
            providerRepository.delete(config)
        }
    }

    /** Makes a configuration the active one. */
    fun setActive(config: ProviderConfig) {
        val ownerId = userId ?: return
        viewModelScope.launch {
            providerRepository.setActive(ownerId, config.id)
        }
    }
}

private fun ProviderFormState.toDraft(): ProviderDraft = ProviderDraft(
    providerId = providerId,
    nickname = nickname,
    baseUrl = baseUrl,
    chatPath = chatPath,
    testPath = testPath,
    model = model,
    apiKey = apiKey,
    authType = authType,
    apiKeyHeader = apiKeyHeader,
    apiKeyQueryParam = apiKeyQueryParam,
    extraHeaders = extraHeaders,
    streaming = streaming,
    temperature = temperature.toDoubleOrNull(),
    maxTokens = maxTokens.toIntOrNull(),
    requestStyle = requestStyle,
    responseTextPath = responseTextPath.ifBlank { null }
)
