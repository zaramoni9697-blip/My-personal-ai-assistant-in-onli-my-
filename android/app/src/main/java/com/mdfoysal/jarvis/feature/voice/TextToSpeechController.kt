package com.mdfoysal.jarvis.feature.voice

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import com.mdfoysal.jarvis.core.text.AssistantLanguage
import java.util.Locale

/**
 * Wraps Android's text-to-speech engine.
 *
 * Bengali voices are not installed on every device, so the controller reports the problem
 * instead of silently speaking with a wrong-language voice.
 */
class TextToSpeechController(
    context: Context,
    private val listener: Listener
) {

    interface Listener {
        fun onReady(available: Boolean)
        fun onStart()
        fun onDone()
        fun onError(message: String)
    }

    private var engine: TextToSpeech? = null
    private var ready = false
    private var pendingText: String? = null
    private var pendingLanguage = AssistantLanguage.ENGLISH

    var rate: Float = 1.0f
    var pitch: Float = 1.0f

    val isSpeaking: Boolean get() = ready && engine?.isSpeaking == true

    /** Initialises the engine. Safe to call more than once. */
    fun initialize() {
        if (engine != null) return
        engine = TextToSpeech(context) { status ->
            ready = status == TextToSpeech.SUCCESS
            if (ready) {
                val result = engine?.setLanguage(localeFor(AssistantLanguage.ENGLISH))
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    listener.onReady(false)
                } else {
                    listener.onReady(true)
                }
                applyRateAndPitch()
            } else {
                listener.onReady(false)
            }
        }
        engine?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) = listener.onStart()
            override fun onDone(utteranceId: String?) = listener.onDone()
            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) = listener.onError("Speech synthesis failed")
            override fun onError(utteranceId: String?, errorCode: Int) =
                listener.onError("Speech synthesis failed ($errorCode)")
        })
    }

    /** Speaks [text] in the given language. */
    fun speak(text: String, language: AssistantLanguage = AssistantLanguage.ENGLISH) {
        if (text.isBlank()) return
        if (engine == null) {
            pendingText = text
            pendingLanguage = language
            initialize()
            return
        }
        if (!ready) {
            listener.onError("Text-to-speech is not ready yet")
            return
        }
        val result = engine?.setLanguage(localeFor(language))
        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            listener.onError(
                "No ${if (language == AssistantLanguage.BENGALI) "Bengali" else "English"} voice is " +
                    "installed on this device. Install the voice data in Android settings."
            )
            return
        }
        applyRateAndPitch()
        engine?.speak(text, TextToSpeech.QUEUE_FLUSH, null, UTTERANCE_ID)
    }

    /** Stops the current utterance (the "stop speaking" button). */
    fun stop() {
        runCatching { engine?.stop() }
        listener.onDone()
    }

    fun setRate(value: Float) {
        rate = value
        applyRateAndPitch()
    }

    fun setPitch(value: Float) {
        pitch = value
        applyRateAndPitch()
    }

    fun shutdown() {
        runCatching { engine?.stop() }
        runCatching { engine?.shutdown() }
        engine = null
        ready = false
    }

    private fun applyRateAndPitch() {
        engine?.setSpeechRate(rate.coerceIn(0.4f, 2.0f))
        engine?.setPitch(pitch.coerceIn(0.5f, 2.0f))
    }

    private fun localeFor(language: AssistantLanguage): Locale = when (language) {
        AssistantLanguage.BENGALI -> Locale("bn", "BD")
        AssistantLanguage.ENGLISH -> Locale.US
        AssistantLanguage.MIXED -> Locale.getDefault()
    }

    companion object {
        private const val UTTERANCE_ID = "jarvis-utterance"
    }
}
