package com.jarvis.ai.voice

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TextToSpeechManager @Inject constructor(@ApplicationContext private val context: Context) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking
    private var initDone = false

    fun init() {
        if (tts == null) tts = TextToSpeech(context, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            initDone = true
            tts?.language = Locale.getDefault()
            tts?.setSpeechRate(1.0f)
            tts?.setPitch(1.0f)
            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(id: String?) { _isSpeaking.value = true }
                override fun onDone(id: String?) { _isSpeaking.value = false }
                override fun onError(id: String?) { _isSpeaking.value = false }
            })
        }
    }

    fun speak(text: String, languageTag: String? = null) {
        if (!initDone) init()
        val locale = when {
            languageTag?.startsWith("bn") == true -> Locale("bn", "BD")
            languageTag == "en" -> Locale.US
            else -> detectLocale(text)
        }
        tts?.language = locale
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "jarvis_${System.currentTimeMillis()}")
    }

    fun stop() { tts?.stop(); _isSpeaking.value = false }
    fun shutdown() { tts?.shutdown(); tts = null }

    fun setRate(rate: Float) { tts?.setSpeechRate(rate.coerceIn(0.5f, 2.0f)) }
    fun setPitch(pitch: Float) { tts?.setPitch(pitch.coerceIn(0.5f, 2.0f)) }

    fun isLanguageAvailable(locale: Locale): Boolean {
        return tts?.isLanguageAvailable(locale) ?: TextToSpeech.LANG_NOT_SUPPORTED
    } >= TextToSpeech.LANG_AVAILABLE

    private fun detectLocale(text: String): Locale {
        // Simple heuristic: Bengali Unicode range
        val hasBengali = text.any { it.code in 0x0980..0x09FF }
        return if (hasBengali) Locale("bn", "BD") else Locale.US
    }
}
