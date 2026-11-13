package com.mdfoysal.jarvis.feature.chat

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mdfoysal.jarvis.core.persona.DeviceAction
import com.mdfoysal.jarvis.core.persona.DeviceCommand
import com.mdfoysal.jarvis.core.persona.DeviceIntentParser
import com.mdfoysal.jarvis.core.persona.JarvisPersona
import com.mdfoysal.jarvis.core.providers.ProviderConfig
import com.mdfoysal.jarvis.core.text.AssistantLanguage
import com.mdfoysal.jarvis.core.text.LanguageDetector
import com.mdfoysal.jarvis.data.local.entity.MessageEntity
import com.mdfoysal.jarvis.data.repository.AuthRepository
import com.mdfoysal.jarvis.data.repository.ChatRepository
import com.mdfoysal.jarvis.data.repository.ProviderRepository
import com.mdfoysal.jarvis.data.repository.SettingsRepository
import com.mdfoysal.jarvis.data.security.SessionManager
import com.mdfoysal.jarvis.feature.device.DeviceController
import com.mdfoysal.jarvis.feature.device.DeviceResult
import com.mdfoysal.jarvis.feature.voice.SpeechRecognizerController
import com.mdfoysal.jarvis.feature.voice.TextToSpeechController
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

/** What JARVIS is doing right now. */
enum class AssistantState { IDLE, LISTENING, THINKING, SPEAKING }

/** UI state of the dashboard. */
data class ChatUiState(
    val messages: List<MessageEntity> = emptyList(),
    val input: String = "",
    val assistantState: AssistantState = AssistantState.IDLE,
    val partialSpeech: String = "",
    val amplitude: Float = 0f,
    val error: String? = null,
    val activeProvider: ProviderConfig? = null,
    val hasProvider: Boolean = false,
    val language: AssistantLanguage = AssistantLanguage.ENGLISH,
    val pendingCommand: DeviceCommand? = null,
    val deviceResult: DeviceResult? = null,
    val conversationId: String? = null
)

class ChatViewModel(
    private val chatRepository: ChatRepository,
    private val providerRepository: ProviderRepository,
    private val authRepository: AuthRepository,
    private val settingsRepository: SettingsRepository,
    private val sessionManager: SessionManager,
    context: Context
) : ViewModel() {

    private val appContext = context.applicationContext
    private val deviceController = DeviceController(appContext)

    private val _state = MutableStateFlow(ChatUiState())
    val state: StateFlow<ChatUiState> = _state.asStateFlow()

    private var userId: String = ""
    private var displayName: String = ""

    private val speechRecognizer = SpeechRecognizerController(
        appContext,
        object : SpeechRecognizerController.Listener {
            override fun onReady() {
                _state.update { it.copy(assistantState = AssistantState.LISTENING) }
            }

            override fun onPartialResult(text: String) {
                _state.update { it.copy(partialSpeech = text) }
            }

            override fun onFinalResult(text: String) {
                _state.update { it.copy(partialSpeech = "", assistantState = AssistantState.IDLE) }
                if (text.isNotBlank()) {
                    onInputChange(text)
                    send()
                }
            }

            override fun onError(message: String) {
                _state.update {
                    it.copy(assistantState = AssistantState.IDLE, partialSpeech = "", error = message)
                }
            }

            override fun onEndOfSpeech() = Unit

            override fun onRmsChanged(rms: Float) {
                val normalized = ((rms + 2f) / 12f).coerceIn(0f, 1f)
                _state.update { it.copy(amplitude = normalized) }
            }
        }
    )

    private val textToSpeech = TextToSpeechController(
        appContext,
        object : TextToSpeechController.Listener {
            override fun onReady(available: Boolean) {
                if (!available) {
                    _state.update { it.copy(error = "Text-to-speech is not available on this device") }
                }
            }

            override fun onStart() {
                _state.update { it.copy(assistantState = AssistantState.SPEAKING) }
            }

            override fun onDone() {
                _state.update { it.copy(assistantState = AssistantState.IDLE) }
            }

            override fun onError(message: String) {
                _state.update { it.copy(error = message, assistantState = AssistantState.IDLE) }
            }
        }
    )

    init {
        viewModelScope.launch {
            val session = sessionManager.session.first()
            if (session == null) return@launch
            userId = session.userId
            displayName = session.displayName

            val settings = settingsRepository.settings.first()
            textToSpeech.setRate(settings.ttsRate)
            textToSpeech.setPitch(settings.ttsPitch)
            textToSpeech.initialize()

            val provider = providerRepository.activeConfig(session.userId)
            _state.update {
                it.copy(
                    activeProvider = provider,
                    hasProvider = provider != null,
                    language = languageFrom(settings.language)
                )
            }

            val conversation = chatRepository.ensureConversation(session.userId, provider?.id)
            _state.update { it.copy(conversationId = conversation.id) }
            chatRepository.observeMessages(conversation.id).collect { messages ->
                _state.update { it.copy(messages = messages) }
            }
        }

        viewModelScope.launch {
            providerRepository.observeActive(userId).collect { active ->
                _state.update { it.copy(activeProvider = active, hasProvider = active != null) }
            }
        }
    }

    fun onInputChange(value: String) = _state.update { it.copy(input = value) }

    fun clearError() = _state.update { it.copy(error = null) }

    fun clearDeviceResult() = _state.update { it.copy(deviceResult = null) }

    /** Sends the current input to JARVIS. */
    fun send() {
        val text = _state.value.input.trim()
        if (text.isEmpty() || _state.value.assistantState == AssistantState.THINKING) return
        _state.update { it.copy(input = "", error = null) }
        viewModelScope.launch { processMessage(text, null) }
    }

    /** Sends a message that was captured by the microphone. */
    fun sendVoice(text: String) {
        if (text.isBlank()) return
        viewModelScope.launch { processMessage(text.trim(), null) }
    }

    /** Sends a message together with an image (Gemini and other multimodal providers). */
    fun sendWithImage(text: String, imageBase64: String, mimeType: String) {
        viewModelScope.launch { processMessage(text.trim().ifBlank { "Describe this image" }, imageBase64, mimeType) }
    }

    private suspend fun processMessage(text: String, imageBase64: String?, mimeType: String? = null) {
        val conversationId = _state.value.conversationId ?: return
        chatRepository.persistUserMessage(conversationId, text, imageBase64, mimeType)

        val language = LanguageDetector.detect(text)
        _state.update { it.copy(language = language, assistantState = AssistantState.THINKING) }

        // 1. Phone commands are handled locally, with an explicit confirmation.
        val command = DeviceIntentParser.parse(text)
        if (command.isKnown && command.action != DeviceAction.UNKNOWN) {
            _state.update { it.copy(pendingCommand = command, assistantState = AssistantState.IDLE) }
            return
        }

        // 2. Identity questions are answered locally, instantly and correctly.
        val local = JarvisPersona.localReply(text)
        if (local != null) {
            chatRepository.persistAssistantMessage(conversationId, local.text)
            speak(local.text)
            return
        }

        // 3. Everything else goes to the configured provider.
        val provider = _state.value.activeProvider
        if (provider == null) {
            val message = "No AI provider is configured yet. Open the provider setup to add one."
            chatRepository.persistAssistantMessage(conversationId, message)
            _state.update { it.copy(assistantState = AssistantState.IDLE, error = message) }
            return
        }
        val apiKey = providerRepository.apiKeyFor(provider)
        if (apiKey.isNullOrBlank()) {
            val message = "The API key for '${provider.nickname}' is missing. Please add it in the provider setup."
            chatRepository.persistAssistantMessage(conversationId, message)
            _state.update { it.copy(assistantState = AssistantState.IDLE, error = message) }
            return
        }

        val messages = chatRepository.buildPromptMessages(userId, conversationId, displayName)
        val enriched = if (imageBase64 != null && messages.isNotEmpty()) {
            messages.dropLast(1) + messages.last().copy(
                text = text,
                imageBase64 = imageBase64,
                imageMimeType = mimeType
            )
        } else {
            messages
        }

        chatRepository.complete(
            userId = userId,
            conversationId = conversationId,
            userName = displayName,
            providerConfig = provider,
            apiKey = apiKey
        ).fold(
            onSuccess = { answer ->
                chatRepository.persistAssistantMessage(conversationId, answer)
                speak(answer)
            },
            onFailure = { error ->
                val message = error.message ?: "Something went wrong"
                chatRepository.persistAssistantMessage(conversationId, message, errorMessage = message)
                _state.update { it.copy(assistantState = AssistantState.IDLE, error = message) }
            }
        )
    }

    /** Executes the confirmed device command. */
    fun confirmCommand() {
        val command = _state.value.pendingCommand ?: return
        _state.update { it.copy(pendingCommand = null) }
        viewModelScope.launch {
            val result = deviceController.execute(command)
            val conversationId = _state.value.conversationId
            if (conversationId != null) {
                chatRepository.persistAssistantMessage(
                    conversationId,
                    if (result.success) result.message else "I could not do that: ${result.message}"
                )
            }
            _state.update { it.copy(deviceResult = result) }
            if (result.success) {
                speak(result.message)
            }
        }
    }

    fun dismissCommand() {
        _state.update { it.copy(pendingCommand = null) }
    }

    // ------------------------------------------------------------------ voice

    /** Starts listening through the microphone. */
    fun startListening() {
        if (_state.value.assistantState == AssistantState.SPEAKING) {
            stopSpeaking()
        }
        val settings = runCatching { settingsRepository.settings.first() }.getOrNull()
        val language = languageFrom(settings?.language ?: "auto")
        _state.update { it.copy(assistantState = AssistantState.LISTENING, partialSpeech = "", error = null) }
        speechRecognizer.start(language)
    }

    fun stopListening() {
        speechRecognizer.stop()
        _state.update { it.copy(assistantState = AssistantState.IDLE) }
    }

    /** Stops the ongoing speech (the "stop speaking" button / barge-in). */
    fun stopSpeaking() {
        textToSpeech.stop()
        _state.update { it.copy(assistantState = AssistantState.IDLE) }
    }

    fun speak(text: String) {
        val settings = runCatching { settingsRepository.settings.first() }.getOrNull()
        if (settings?.ttsEnabled == false) {
            _state.update { it.copy(assistantState = AssistantState.IDLE) }
            return
        }
        val language = _state.value.language
        textToSpeech.speak(text, language)
    }

    /** Clears the current conversation (messages are deleted from the device). */
    fun clearConversation() {
        val conversationId = _state.value.conversationId ?: return
        viewModelScope.launch {
            chatRepository.clearMessages(conversationId)
        }
    }

    override fun onCleared() {
        super.onCleared()
        speechRecognizer.destroy()
        textToSpeech.shutdown()
    }

    private fun languageFrom(preference: String): AssistantLanguage = when (preference) {
        "bn" -> AssistantLanguage.BENGALI
        "en" -> AssistantLanguage.ENGLISH
        else -> AssistantLanguage.MIXED
    }
}
