package com.jarvis.ai.data.local.datastore

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "jarvis_prefs")

@Singleton
class PreferencesManager @Inject constructor(@ApplicationContext private val context: Context) {

    object Keys {
        val CURRENT_USER_ID = stringPreferencesKey("current_user_id")
        val CURRENT_USER_EMAIL = stringPreferencesKey("current_user_email")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val LANGUAGE = stringPreferencesKey("language") // auto, en, bn
        val WAKE_WORD_ENABLED = booleanPreferencesKey("wake_word_enabled")
        val WAKE_WORD = stringPreferencesKey("wake_word")
        val VOICE_ENABLED = booleanPreferencesKey("voice_enabled")
        val TTS_RATE = floatPreferencesKey("tts_rate")
        val TTS_PITCH = floatPreferencesKey("tts_pitch")
        val ACTIVE_PROVIDER_ID = stringPreferencesKey("active_provider_id")
    }

    val currentUserId: Flow<String?> = context.dataStore.data.map { it[Keys.CURRENT_USER_ID] }
    val currentUserEmail: Flow<String?> = context.dataStore.data.map { it[Keys.CURRENT_USER_EMAIL] }
    val wakeWordEnabled: Flow<Boolean> = context.dataStore.data.map { it[Keys.WAKE_WORD_ENABLED] ?: false }
    val language: Flow<String> = context.dataStore.data.map { it[Keys.LANGUAGE] ?: "auto" }

    suspend fun setCurrentUser(id: String, email: String) {
        context.dataStore.edit { it[Keys.CURRENT_USER_ID] = id; it[Keys.CURRENT_USER_EMAIL] = email }
    }
    suspend fun clearCurrentUser() { context.dataStore.edit { it.remove(Keys.CURRENT_USER_ID); it.remove(Keys.CURRENT_USER_EMAIL) } }
    suspend fun set(key: Preferences.Key<String>, value: String) { context.dataStore.edit { it[key] = value } }
    suspend fun setBoolean(key: Preferences.Key<Boolean>, value: Boolean) { context.dataStore.edit { it[key] = value } }
    suspend fun <T> setGeneric(key: Preferences.Key<T>, value: T) { context.dataStore.edit { it[key] = value } }
}
