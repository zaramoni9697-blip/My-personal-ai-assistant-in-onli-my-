package com.mdfoysal.jarvis.core.crypto

import java.security.SecureRandom
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * Password hashing for locally stored accounts.
 *
 * PBKDF2-HMAC-SHA256 with a per-user random salt and a configurable iteration count.
 * The encoded format keeps the parameters next to the hash so they can be upgraded later.
 */
object PasswordHasher {

    private const val ALGORITHM = "PBKDF2WithHmacSHA256"
    private const val ITERATIONS = 120_000
    private const val KEY_LENGTH_BITS = 256
    private const val SALT_LENGTH = 16

    private val random = SecureRandom()

    /** Encodes as `pbkdf2$sha256$<iterations>$<saltB64>$<hashB64>`. */
    fun hash(password: CharArray, iterations: Int = ITERATIONS): String {
        val salt = ByteArray(SALT_LENGTH).also { random.nextBytes(it) }
        val hash = pbkdf2(password, salt, iterations)
        return listOf(
            "pbkdf2",
            "sha256",
            iterations.toString(),
            Base64.getEncoder().encodeToString(salt),
            Base64.getEncoder().encodeToString(hash)
        ).joinToString("$")
    }

    fun verify(password: CharArray, encoded: String): Boolean {
        val parts = encoded.split("$")
        if (parts.size != 5 || parts[0] != "pbkdf2") return false
        val iterations = parts[2].toIntOrNull() ?: return false
        val salt = try {
            Base64.getDecoder().decode(parts[3])
        } catch (e: IllegalArgumentException) {
            return false
        }
        val expected = try {
            Base64.getDecoder().decode(parts[4])
        } catch (e: IllegalArgumentException) {
            return false
        }
        val actual = pbkdf2(password, salt, iterations)
        if (actual.size != expected.size) return false
        var diff = 0
        for (i in actual.indices) diff = diff or (actual[i].toInt() xor expected[i].toInt())
        return diff == 0
    }

    private fun pbkdf2(password: CharArray, salt: ByteArray, iterations: Int): ByteArray {
        val spec = PBEKeySpec(password, salt, iterations, KEY_LENGTH_BITS)
        val factory = SecretKeyFactory.getInstance(ALGORITHM)
        return factory.generateSecret(spec).encoded
    }
}
