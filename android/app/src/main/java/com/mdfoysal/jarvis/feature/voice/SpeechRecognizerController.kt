package com.mdfoysal.jarvis.feature.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import com.mdfoysal.jarvis.core.text.AssistantLanguage
import java.util.Locale

/**
 * Wraps Android's speech recogniser.
 *
 * Everything is explicit: the microphone is only used while [start] has been called, the
 * recogniser reports errors instead of failing silently, and the caller decides what happens
 * next (including stopping the microphone when the user starts speaking).
 */
class SpeechRecognizerController(
    private val context: Context,
    private val listener: Listener
) {

    interface Listener {
        fun onReady()
        fun onPartialResult(text: String)
        fun onFinalResult(text: String)
        fun onError(message: String)
        fun onEndOfSpeech()
        fun onRmsChanged(rms: Float)
    }

    private var recognizer: SpeechRecognizer? = null
    private var listening = false

    val isListening: Boolean get() = listening

    val isAvailable: Boolean
        get() = SpeechRecognizer.isRecognitionAvailable(context)

    /** Languages supported by the device recogniser (used by the settings screen). */
    fun supportedLanguages(): Set<Locale> = runCatching {
        SpeechRecognizer.getSupportedLanguages(context)
    }.getOrDefault(emptySet())

    /** Starts listening in the given language. Bengali and English are both supported. */
    fun start(language: AssistantLanguage = AssistantLanguage.ENGLISH) {
        if (!isAvailable) {
            listener.onError("Speech recognition is not available on this device")
            return
        }
        stop()
        val recognizer = this.recognizer ?: SpeechRecognizer.createSpeechRecognizer(context).also {
            this.recognizer = it
        }
        recognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) = listener.onReady()

            override fun onBeginningOfSpeech() = Unit

            override fun onRmsChanged(rmsdB: Float) = listener.onRmsChanged(rmsdB)

            override fun onBufferReceived(buffer: ByteArray?) = Unit

            override fun onEndOfSpeech() = listener.onEndOfSpeech()

            override fun onError(error: Int) {
                listening = false
                listener.onError(describeError(error))
            }

            override fun onResults(results: Bundle?) {
                listening = false
                val text = results
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull()
                    .orEmpty()
                listener.onFinalResult(text)
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val text = partialResults
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull()
                    .orEmpty()
                listener.onPartialResult(text)
            }

            override fun onEvent(eventType: Int, params: Bundle?) = Unit
        })

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, localeTag(language))
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
        }
        listening = true
        runCatching { recognizer.startListening(intent) }
            .onFailure { listening = false; listener.onError(it.message ?: "Could not start listening") }
    }

    /** Stops listening but keeps partial results. */
    fun stop() {
        runCatching { recognizer?.stopListening() }
    }

    /** Cancels listening and discards results. */
    fun cancel() {
        listening = false
        runCatching { recognizer?.cancel() }
    }

    fun destroy() {
        listening = false
        runCatching { recognizer?.destroy() }
        recognizer = null
    }

    private fun localeTag(language: AssistantLanguage): String = when (language) {
        AssistantLanguage.BENGALI -> "bn-BD"
        AssistantLanguage.ENGLISH -> "en-US"
        AssistantLanguage.MIXED -> Locale.getDefault().toLanguageTag()
    }

    private fun describeError(error: Int): String = when (error) {
        SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
        SpeechRecognizer.ERROR_CLIENT -> "Recognition client error"
        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission is required"
        SpeechRecognizer.ERROR_NETWORK -> "Network error while recognising speech"
        SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Speech recognition timed out"
        SpeechRecognizer.ERROR_NO_MATCH -> "I did not catch that — please try again"
        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "The recogniser is busy, please retry"
        SpeechRecognizer.ERROR_SERVER -> "The speech service returned an error"
        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech was detected"
        else -> "Speech recognition failed ($error)"
    }
}
