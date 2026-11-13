package com.mdfoysal.jarvis.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "jarvis_settings")

/** User-facing application settings, persisted with Jetpack DataStore. */
class SettingsStore(private val context: Context) {

    private object Keys {
        val LANGUAGE = stringPreferencesKey("language")            // "auto" | "bn" | "en"
        val TTS_ENABLED = booleanPreferencesKey("tts_enabled")
        val TTS_RATE = floatPreferencesKey("tts_rate")
        val TTS_PITCH = floatPreferencesKey("tts_pitch")
        val VOICE_MODE = booleanPreferencesKey("voice_mode")
        val WAKE_WORD_ENABLED = booleanPreferencesKey("wake_word_enabled")
        val WAKE_WORD_PHRASE = stringPreferencesKey("wake_word_phrase")
        val MEMORY_ENABLED = booleanPreferencesKey("memory_enabled")
        val ACTIVE_PROVIDER_ID = stringPreferencesKey("active_provider_id")
        val SERVER_URL = stringPreferencesKey("server_url")
        val REMOTE_AUTH = booleanPreferencesKey("remote_auth")
        val HAPTIC_ENABLED = booleanPreferencesKey("haptic_enabled")
        val THEME_MODE = stringPreferencesKey("theme_mode")        // "dark" | "system"
        val ONBOARDED = booleanPreferencesKey("onboarded")
    }

    data class Settings(
        val language: String = "auto",
        val ttsEnabled: Boolean = true,
        val ttsRate: Float = 1.0f,
        val ttsPitch: Float = 1.0f,
        val voiceMode: Boolean = false,
        val wakeWordEnabled: Boolean = false,
        val wakeWordPhrase: String = DEFAULT_WAKE_WORD,
        val memoryEnabled: Boolean = true,
        val activeProviderId: String? = null,
        val serverUrl: String = "",
        val remoteAuth: Boolean = false,
        val hapticEnabled: Boolean = true,
        val themeMode: String = "dark",
        val onboarded: Boolean = false
    )

    val settings: Flow<Settings> = context.settingsDataStore.data.map { prefs ->
        Settings(
            language = prefs[Keys.LANGUAGE] ?: "auto",
            ttsEnabled = prefs[Keys.TTS_ENABLED] ?: true,
            ttsRate = prefs[Keys.TTS_RATE] ?: 1.0f,
            ttsPitch = prefs[Keys.TTS_PITCH] ?: 1.0f,
            voiceMode = prefs[Keys.VOICE_MODE] ?: false,
            wakeWordEnabled = prefs[Keys.WAKE_WORD_ENABLED] ?: false,
            wakeWordPhrase = prefs[Keys.WAKE_WORD_PHRASE] ?: DEFAULT_WAKE_WORD,
            memoryEnabled = prefs[Keys.MEMORY_ENABLED] ?: true,
            activeProviderId = prefs[Keys.ACTIVE_PROVIDER_ID],
            serverUrl = prefs[Keys.SERVER_URL] ?: "",
            remoteAuth = prefs[Keys.REMOTE_AUTH] ?: false,
            hapticEnabled = prefs[Keys.HAPTIC_ENABLED] ?: true,
            themeMode = prefs[Keys.THEME_MODE] ?: "dark",
            onboarded = prefs[Keys.ONBOARDED] ?: false
        )
    }

    suspend fun setLanguage(value: String) = put(Keys.LANGUAGE, value)
    suspend fun setTtsEnabled(value: Boolean) = put(Keys.TTS_ENABLED, value)
    suspend fun setTtsRate(value: Float) = put(Keys.TTS_RATE, value)
    suspend fun setTtsPitch(value: Float) = put(Keys.TTS_PITCH, value)
    suspend fun setVoiceMode(value: Boolean) = put(Keys.VOICE_MODE, value)
    suspend fun setWakeWordEnabled(value: Boolean) = put(Keys.WAKE_WORD_ENABLED, value)
    suspend fun setWakeWordPhrase(value: String) = put(Keys.WAKE_WORD_PHRASE, value)
    suspend fun setMemoryEnabled(value: Boolean) = put(Keys.MEMORY_ENABLED, value)
    suspend fun setActiveProviderId(value: String?) {
        context.settingsDataStore.edit { prefs ->
            if (value == null) prefs.remove(Keys.ACTIVE_PROVIDER_ID) else prefs[Keys.ACTIVE_PROVIDER_ID] = value
        }
    }

    suspend fun setServerUrl(value: String) = put(Keys.SERVER_URL, value)
    suspend fun setRemoteAuth(value: Boolean) = put(Keys.REMOTE_AUTH, value)
    suspend fun setHapticEnabled(value: Boolean) = put(Keys.HAPTIC_ENABLED, value)
    suspend fun setThemeMode(value: String) = put(Keys.THEME_MODE, value)
    suspend fun setOnboarded(value: Boolean) = put(Keys.ONBOARDED, value)

    private suspend fun <T> put(key: Preferences.Key<T>, value: T) {
        context.settingsDataStore.edit { prefs -> prefs[key] = value }
    }

    companion object {
        const val DEFAULT_WAKE_WORD = "সবসময় প্রস্তুত"
    }
}
