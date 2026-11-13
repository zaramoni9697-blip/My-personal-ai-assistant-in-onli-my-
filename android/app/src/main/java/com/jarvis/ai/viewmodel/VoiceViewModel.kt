package com.jarvis.ai.viewmodel

import androidx.lifecycle.ViewModel
import com.jarvis.ai.voice.SpeechRecognitionManager
import com.jarvis.ai.voice.TextToSpeechManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class VoiceViewModel @Inject constructor(
    val stt: SpeechRecognitionManager,
    val tts: TextToSpeechManager
) : ViewModel() {

    val isListening = stt.isListening
    val isSpeaking = tts.isSpeaking

    private val _lastTranscript = MutableStateFlow("")
    val lastTranscript: StateFlow<String> = _lastTranscript

    private val _voiceEnabled = MutableStateFlow(true)
    val voiceEnabled: StateFlow<Boolean> = _voiceEnabled

    fun toggleVoice(v: Boolean) { _voiceEnabled.value = v }

    fun startListening(lang: String = "auto", onResult: (String)->Unit) {
        stt.startListening(lang, onResult = { text -> _lastTranscript.value = text; onResult(text) })
    }
    fun stopListening() = stt.stopListening()
    fun speak(text: String) { if (_voiceEnabled.value) tts.speak(text) }
    fun stopSpeaking() = tts.stop()

    override fun onCleared() { stt.destroy(); tts.shutdown() }
}
