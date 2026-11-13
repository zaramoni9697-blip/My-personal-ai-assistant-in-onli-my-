package com.jarvis.ai.data.security

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CryptoManager @Inject constructor(@ApplicationContext private val context: Context) {

    private val masterKey: MasterKey by lazy {
        MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
    }

    private val prefs: SharedPreferences by lazy {
        EncryptedSharedPreferences.create(
            context,
            "jarvis_secure_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun saveApiKey(providerId: String, apiKey: String) {
        prefs.edit().putString("api_key_$providerId", apiKey).apply()
    }

    fun getApiKey(providerId: String): String? = prefs.getString("api_key_$providerId", null)

    fun removeApiKey(providerId: String) {
        prefs.edit().remove("api_key_$providerId").apply()
    }

    fun saveString(key: String, value: String) { prefs.edit().putString(key, value).apply() }
    fun getString(key: String, def: String? = null): String? = prefs.getString(key, def)
    fun remove(key: String) { prefs.edit().remove(key).apply() }
    fun clearAll() { prefs.edit().clear().apply() }

    // For DB encryption wrapper (simplified - Room encryption via SQLCipher would be separate)
    fun encryptForDb(plain: String): String {
        // In production use SQLCipher. Here we store in EncryptedSharedPreferences
        // and keep DB reference id. This is a placeholder for real encryption.
        return plain // EncryptedSharedPreferences already secures keys; DB stores reference
    }
}
