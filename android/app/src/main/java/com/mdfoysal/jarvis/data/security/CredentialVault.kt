package com.mdfoysal.jarvis.data.security

import android.content.Context
import com.mdfoysal.jarvis.core.crypto.SecretBox

/**
 * Encrypted store for API keys.
 *
 * Every key is encrypted with AES-256-GCM using a data-encryption key that is itself wrapped
 * by the Android Keystore (see [KeystoreKeyProvider]). Nothing sensitive is ever written to
 * the database, to logs or to the backup agent.
 */
class CredentialVault(
    context: Context,
    private val keyProvider: KeystoreKeyProvider = KeystoreKeyProvider()
) {

    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    @Volatile
    private var cachedKey: ByteArray? = null

    private fun dataKey(): ByteArray? {
        cachedKey?.let { return it }
        val wrapped = prefs.getString(KEY_WRAPPED_DEK, null) ?: return null
        val raw = keyProvider.unwrap(wrapped) ?: return null
        cachedKey = raw
        return raw
    }

    private fun ensureDataKey(): ByteArray {
        dataKey()?.let { return it }
        val raw = SecretBox.randomKey()
        prefs.edit().putString(KEY_WRAPPED_DEK, keyProvider.wrap(raw)).apply()
        cachedKey = raw
        return raw
    }

    /** Stores (or replaces) the API key for [credentialId]. */
    fun saveApiKey(credentialId: String, apiKey: String) {
        val encrypted = SecretBox.encrypt(apiKey, ensureDataKey())
        prefs.edit().putString(entryKey(credentialId), encrypted).apply()
    }

    /** Reads the API key for [credentialId] or `null` when it is not stored. */
    fun getApiKey(credentialId: String): String? {
        val encrypted = prefs.getString(entryKey(credentialId), null) ?: return null
        val key = dataKey() ?: return null
        return SecretBox.decryptOrNull(encrypted, key)
    }

    fun hasCredential(credentialId: String): Boolean = prefs.contains(entryKey(credentialId))

    fun delete(credentialId: String) {
        prefs.edit().remove(entryKey(credentialId)).apply()
    }

    /** Removes every stored credential (used when an account is deleted). */
    fun clearAll() {
        prefs.edit().clear().apply()
        cachedKey = null
    }

    /** Rotates the data-encryption key: every stored key is re-encrypted with a fresh DEK. */
    fun rotateKey() {
        val existing = prefs.all
            .filterKeys { it.startsWith(ENTRY_PREFIX) }
            .mapValues { (_, value) -> value as? String }
            .filterValues { it != null }
        val newKey = SecretBox.randomKey()
        val editor = prefs.edit()
        existing.forEach { (key, value) ->
            val oldKey = dataKey()
            val plain = oldKey?.let { SecretBox.decryptOrNull(value!!, it) }
            if (plain != null) {
                editor.putString(key, SecretBox.encrypt(plain, newKey))
            }
        }
        editor.putString(KEY_WRAPPED_DEK, keyProvider.wrap(newKey)).apply()
        cachedKey = newKey
    }

    private fun entryKey(credentialId: String) = "$ENTRY_PREFIX$credentialId"

    companion object {
        private const val PREFS_NAME = "jarvis_secure_credentials"
        private const val KEY_WRAPPED_DEK = "wrapped_dek"
        private const val ENTRY_PREFIX = "cred::"
    }
}
