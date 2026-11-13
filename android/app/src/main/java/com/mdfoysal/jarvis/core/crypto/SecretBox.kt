package com.mdfoysal.jarvis.core.crypto

import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Authenticated encryption for locally stored credentials.
 *
 * The key material itself never lives in this class — on Android it is produced by the
 * Android Keystore (see `KeystoreKeyProvider`) and handed in per operation. The core code is
 * plain Java crypto so it can be unit-tested on the JVM.
 */
object SecretBox {

    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val IV_LENGTH = 12
    private const val TAG_LENGTH_BITS = 128
    private const val KEY_LENGTH = 32 // AES-256

    private val random = SecureRandom()

    fun randomKey(): ByteArray = ByteArray(KEY_LENGTH).also { random.nextBytes(it) }

    /** Encrypts [plaintext]; returns base64 of `iv || ciphertext||tag`. */
    fun encrypt(plaintext: String, key: ByteArray): String {
        require(key.size == KEY_LENGTH) { "AES-256 key required" }
        val iv = ByteArray(IV_LENGTH).also { random.nextBytes(it) }
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(TAG_LENGTH_BITS, iv))
        val encrypted = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
        val combined = ByteArray(iv.size + encrypted.size)
        System.arraycopy(iv, 0, combined, 0, iv.size)
        System.arraycopy(encrypted, 0, combined, iv.size, encrypted.size)
        return Base64.getEncoder().encodeToString(combined)
    }

    /** Decrypts a payload produced by [encrypt]. Throws on tampering (GCM auth tag). */
    fun decrypt(payload: String, key: ByteArray): String {
        require(key.size == KEY_LENGTH) { "AES-256 key required" }
        val combined = Base64.getDecoder().decode(payload)
        require(combined.size > IV_LENGTH) { "Ciphertext too short" }
        val iv = combined.copyOfRange(0, IV_LENGTH)
        val payloadBytes = combined.copyOfRange(IV_LENGTH, combined.size)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(TAG_LENGTH_BITS, iv))
        return String(cipher.doFinal(payloadBytes), Charsets.UTF_8)
    }

    /** Decrypts without throwing; returns `null` when the payload is invalid. */
    fun decryptOrNull(payload: String, key: ByteArray): String? = try {
        decrypt(payload, key)
    } catch (e: Exception) {
        null
    }

    fun keyFrom(spec: SecretKey): ByteArray = spec.encoded
}
