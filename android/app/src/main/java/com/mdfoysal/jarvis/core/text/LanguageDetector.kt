package com.mdfoysal.jarvis.core.text

/**
 * Automatic language detection for Bengali / English / mixed input.
 *
 * JARVIS replies in the language the user used. Bengali is detected through the Unicode
 * block U+0980–U+09FF; everything else that is alphabetic counts as Latin script.
 */
enum class AssistantLanguage {
    BENGALI,
    ENGLISH,
    MIXED
}

object LanguageDetector {

    private const val BENGALI_START = 0x0980
    private const val BENGALI_END = 0x09FF
    private const val BENGALI_EXTRA_START = 0x200C // zero-width non-joiner
    private const val BENGALI_EXTRA_END = 0x200D

    /** Detects the language of [text]. Empty input falls back to English. */
    fun detect(text: String): AssistantLanguage {
        if (text.isBlank()) return AssistantLanguage.ENGLISH
        var bengali = 0
        var latin = 0
        for (ch in text) {
            val code = ch.code
            when {
                code in BENGALI_START..BENGALI_END -> bengali++
                code in BENGALI_EXTRA_START..BENGALI_EXTRA_END -> Unit
                ch.isLetter() -> latin++
            }
        }
        val total = bengali + latin
        if (total == 0) return AssistantLanguage.ENGLISH
        val bengaliRatio = bengali.toDouble() / total
        return when {
            bengaliRatio >= 0.75 -> AssistantLanguage.BENGALI
            bengaliRatio <= 0.12 -> AssistantLanguage.ENGLISH
            else -> AssistantLanguage.MIXED
        }
    }

    /** `true` when the reply should be in Bengali. */
    fun prefersBengali(text: String): Boolean = detect(text) == AssistantLanguage.BENGALI

    /** Human readable label used in the UI status bar. */
    fun label(language: AssistantLanguage): String = when (language) {
        AssistantLanguage.BENGALI -> "বাংলা"
        AssistantLanguage.ENGLISH -> "English"
        AssistantLanguage.MIXED -> "বাংলা + EN"
    }
}
