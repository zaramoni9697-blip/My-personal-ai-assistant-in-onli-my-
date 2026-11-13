package com.mdfoysal.jarvis.core.persona

import com.mdfoysal.jarvis.core.text.AssistantLanguage
import com.mdfoysal.jarvis.core.text.LanguageDetector

/**
 * JARVIS' personality and identity.
 *
 * The creator is fixed: **Md Foysal**. Identity questions are answered locally (instantly and
 * correctly) instead of being delegated to a remote model that could hallucinate.
 */
object JarvisPersona {

    const val APP_NAME = "JARVIS AI"
    const val CREATOR = "Md Foysal"
    const val CREATOR_BENGALI = "Md Foysal"

    /** Builds the system prompt for the configured provider. */
    fun systemPrompt(
        language: AssistantLanguage = AssistantLanguage.ENGLISH,
        memory: List<String> = emptyList(),
        userName: String? = null
    ): String {
        val sb = StringBuilder()
        sb.append("You are JARVIS AI, a personal AI assistant created by ")
            .append(CREATOR)
            .append(".\n")
        sb.append("Personality: friendly, intelligent, professional, helpful, respectful, ")
            .append("natural in conversation, and occasionally witty.\n")
        sb.append("Language rules:\n")
        sb.append("- Reply in Bangla (Bengali script) when the user writes in Bangla.\n")
        sb.append("- Reply in English when the user writes in English.\n")
        sb.append("- If the user mixes Bangla and English, mirror that mix naturally.\n")
        sb.append("- Keep answers concise, well structured and actionable.\n")
        sb.append("Identity rules (never break these):\n")
        sb.append("- You were created by $CREATOR. Never claim another creator.\n")
        sb.append("- If asked who created you, answer: \"I was created by $CREATOR.\" / ")
            .append("\"আমাকে তৈরি করেছেন $CREATOR_BENGALI।\"\n")
        sb.append("Capabilities: general chat, coding help, maths, knowledge questions, ")
            .append("translation, summarisation, writing help and problem solving.\n")
        sb.append("When the user asks for an Android phone action, state the action clearly ")
            .append("so the assistant layer can execute it after confirmation.\n")
        userName?.takeIf { it.isNotBlank() }?.let { sb.append("The user's name is $it.\n") }
        if (memory.isNotEmpty()) {
            sb.append("What you remember about this user (use only when relevant):\n")
            memory.forEach { sb.append("- ").append(it).append('\n') }
        }
        return sb.toString()
    }

    /** Result of a locally-handled question. */
    data class LocalReply(val text: String, val language: AssistantLanguage)

    /**
     * Answers identity / greeting / courtesy questions without calling a provider.
     * Returns `null` when the question needs a real model.
     */
    fun localReply(rawText: String): LocalReply? {
        val text = normalize(rawText)
        if (text.isEmpty()) return null

        if (containsAny(text, CREATOR_QUESTION_EN) || containsAny(text, CREATOR_QUESTION_BN)) {
            val bengali = LanguageDetector.prefersBengali(rawText)
            return LocalReply(
                text = if (bengali) "আমাকে তৈরি করেছেন $CREATOR_BENGALI।" else "I was created by $CREATOR.",
                language = if (bengali) AssistantLanguage.BENGALI else AssistantLanguage.ENGLISH
            )
        }

        if (containsAny(text, IDENTITY_EN) || containsAny(text, IDENTITY_BN)) {
            val bengali = LanguageDetector.prefersBengali(rawText)
            return LocalReply(
                text = if (bengali) {
                    "আমি $APP_NAME — $CREATOR_BENGALI-এর তৈরি একটি Personal AI Assistant।"
                } else {
                    "I'm $APP_NAME, a personal AI assistant created by $CREATOR."
                },
                language = if (bengali) AssistantLanguage.BENGALI else AssistantLanguage.ENGLISH
            )
        }

        if (containsAny(text, GREETING_EN)) {
            return LocalReply(
                text = "Hello! I'm ready. What can I help you with?",
                language = AssistantLanguage.ENGLISH
            )
        }

        if (containsAny(text, GREETING_BN)) {
            return LocalReply(
                text = "হ্যালো! আমি প্রস্তুত। কীভাবে সাহায্য করতে পারি?",
                language = AssistantLanguage.BENGALI
            )
        }

        if (containsAny(text, THANKS_EN)) {
            return LocalReply(
                text = "Any time! I'm here whenever you need me.",
                language = AssistantLanguage.ENGLISH
            )
        }

        if (containsAny(text, THANKS_BN)) {
            return LocalReply(
                text = "সবসময় আছি! আর কিছু দরকার হলে বলুন।",
                language = AssistantLanguage.BENGALI
            )
        }

        return null
    }

    private fun normalize(text: String): String =
        text.lowercase()
            // Keep letters, digits, combining marks (Bengali vowel signs) and format chars (ZWJ/ZWNJ).
            .replace(Regex("[^\\p{L}\\p{N}\\p{M}\\p{Cf}\\s]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()

    private fun containsAny(text: String, needles: List<String>): Boolean =
        needles.any { text.contains(it) }

    private val CREATOR_QUESTION_EN = listOf(
        "who created you", "who made you", "who built you", "who is your creator",
        "who designed you", "who developed you", "your creator", "who owns you"
    )

    private val CREATOR_QUESTION_BN = listOf(
        "কে তৈরি করেছে", "কে বানিয়েছে", "তোমাকে কে", "তোমাকে কি", "আমাকে কে",
        "কে তৈরি করেছেন", "তোমার নির্মাতা", "কে বানিয়েছেন"
    )

    private val IDENTITY_EN = listOf("who are you", "what are you", "introduce yourself", "your name")
    private val IDENTITY_BN = listOf("তুমি কে", "তোমি কে", "আপনি কে", "নিজের পরিচয়")

    private val GREETING_EN = listOf("hello", "hi there", "hey jarvis", "good morning", "good evening", "good afternoon")
    private val GREETING_BN = listOf("হ্যালো", "সালাম", "আসসালামু আলাইকুম", "শুভ সকাল", "শুভ সন্ধ্যা")

    private val THANKS_EN = listOf("thank you", "thanks", "thx", "appreciate it")
    private val THANKS_BN = listOf("ধন্যবাদ", "থ্যাংকস", "আপনাকে ধন্যবাদ")
}
