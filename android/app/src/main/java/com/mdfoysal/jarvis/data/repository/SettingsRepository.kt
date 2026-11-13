package com.mdfoysal.jarvis.data.repository

import com.mdfoysal.jarvis.data.local.SettingsStore
import kotlinx.coroutines.flow.Flow

/** Application settings exposed to the UI layer. */
class SettingsRepository(private val store: SettingsStore) {

    val settings: Flow<SettingsStore.Settings> = store.settings

    suspend fun setLanguage(value: String) = store.setLanguage(value)
    suspend fun setTtsEnabled(value: Boolean) = store.setTtsEnabled(value)
    suspend fun setTtsRate(value: Float) = store.setTtsRate(value)
    suspend fun setTtsPitch(value: Float) = store.setTtsPitch(value)
    suspend fun setVoiceMode(value: Boolean) = store.setVoiceMode(value)
    suspend fun setWakeWordEnabled(value: Boolean) = store.setWakeWordEnabled(value)
    suspend fun setWakeWordPhrase(value: String) = store.setWakeWordPhrase(value)
    suspend fun setMemoryEnabled(value: Boolean) = store.setMemoryEnabled(value)
    suspend fun setServerUrl(value: String) = store.setServerUrl(value)
    suspend fun setRemoteAuth(value: Boolean) = store.setRemoteAuth(value)
    suspend fun setHapticEnabled(value: Boolean) = store.setHapticEnabled(value)
    suspend fun setThemeMode(value: String) = store.setThemeMode(value)
    suspend fun setOnboarded(value: Boolean) = store.setOnboarded(value)
}
