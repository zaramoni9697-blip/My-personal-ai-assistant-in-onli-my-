package com.jarvis.ai.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SpeechRecognitionManager @Inject constructor(@ApplicationContext private val context: Context) {

    private var recognizer: SpeechRecognizer? = null
    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening
    private val _transcript = MutableStateFlow("")
    val transcript: StateFlow<String> = _transcript
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    fun isAvailable(): Boolean = SpeechRecognizer.isRecognitionAvailable(context)

    fun startListening(language: String = "auto", onResult: (String) -> Unit, onError: (String)->Unit = {}) {
        if (!isAvailable()) { onError("Speech recognition not available"); return }
        stopListening()
        recognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
            setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(p: Bundle?) { _isListening.value = true }
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(v: Float) {}
                override fun onBufferReceived(b: ByteArray?) {}
                override fun onEndOfSpeech() { _isListening.value = false }
                override fun onError(e: Int) { _isListening.value = false; val msg = errorCodeToString(e); _error.value = msg; onError(msg) }
                override fun onResults(r: Bundle?) {
                    _isListening.value = false
                    val res = r?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull() ?: ""
                    _transcript.value = res
                    if (res.isNotBlank()) onResult(res)
                }
                override fun onPartialResults(r: Bundle?) {
                    val partial = r?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull() ?: ""
                    _transcript.value = partial
                }
                override fun onEvent(t: Int, p: Bundle?) {}
            })
        }
        val locale = when (language) {
            "bn" -> "bn-BD"
            "en" -> "en-US"
            else -> Locale.getDefault().toLanguageTag()
        }
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, locale)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }
        try { recognizer?.startListening(intent) } catch (e: Exception) { onError(e.message ?: "Failed to start") }
    }

    fun stopListening() { try { recognizer?.stopListening() } catch (_: Exception) {}; _isListening.value = false }
    fun cancel() { try { recognizer?.cancel() } catch (_: Exception) {}; _isListening.value = false }
    fun destroy() { recognizer?.destroy(); recognizer = null }

    private fun errorCodeToString(code: Int): String = when(code) {
        SpeechRecognizer.ERROR_AUDIO -> "Audio error"
        SpeechRecognizer.ERROR_CLIENT -> "Client error"
        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Permission denied"
        SpeechRecognizer.ERROR_NETWORK -> "Network error"
        SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
        SpeechRecognizer.ERROR_NO_MATCH -> "No speech matched"
        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognizer busy"
        SpeechRecognizer.ERROR_SERVER -> "Server error"
        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech input"
        else -> "Error $code"
    }
}
