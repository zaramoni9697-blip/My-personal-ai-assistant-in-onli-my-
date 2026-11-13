package com.mdfoysal.jarvis.data.repository

import com.mdfoysal.jarvis.core.crypto.PasswordHasher
import com.mdfoysal.jarvis.core.json.Json
import com.mdfoysal.jarvis.data.local.SettingsStore
import com.mdfoysal.jarvis.data.local.dao.ConversationDao
import com.mdfoysal.jarvis.data.local.dao.MemoryDao
import com.mdfoysal.jarvis.data.local.dao.MessageDao
import com.mdfoysal.jarvis.data.local.dao.ProviderConfigDao
import com.mdfoysal.jarvis.data.local.dao.UserDao
import com.mdfoysal.jarvis.data.local.entity.UserEntity
import com.mdfoysal.jarvis.data.remote.JarvisBackendClient
import com.mdfoysal.jarvis.data.security.CredentialVault
import com.mdfoysal.jarvis.data.security.Session
import com.mdfoysal.jarvis.data.security.SessionManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.util.UUID

/** Result of an authentication operation. */
sealed class AuthResult {
    data class Success(val session: Session, val message: String? = null) : AuthResult()
    data class Failure(val message: String) : AuthResult()
}

/**
 * Authentication for JARVIS.
 *
 * Two modes are supported and both are real:
 *  - **Local accounts** (default): the account lives on the device, the password is stored as a
 *    PBKDF2-HMAC-SHA256 hash and the session is kept in a private DataStore file.
 *  - **Backend accounts**: the account lives in the JARVIS backend and the app keeps the
 *    issued bearer token. The user's provider keys still stay on the device.
 */
class AuthRepository(
    private val userDao: UserDao,
    private val sessionManager: SessionManager,
    private val settingsStore: SettingsStore,
    private val credentialVault: CredentialVault,
    private val providerConfigDao: ProviderConfigDao,
    private val conversationDao: ConversationDao,
    private val messageDao: MessageDao,
    private val memoryDao: MemoryDao,
    private val backendClient: JarvisBackendClient?
) {

    val session: Flow<Session?> = sessionManager.session

    suspend fun currentSession(): Session? = sessionManager.session.first()

    suspend fun signUp(email: String, password: String, displayName: String): AuthResult {
        val validation = validateCredentials(email, password)
        if (validation != null) return AuthResult.Failure(validation)

        val settings = settingsStore.settings.first()
        if (settings.remoteAuth && backendClient != null) {
            return backendClient.register(email, password, displayName)
                .fold(
                    onSuccess = { response ->
                        val session = Session(
                            userId = response.userId,
                            email = response.email,
                            displayName = response.displayName.ifBlank { displayName },
                            token = response.token,
                            remote = true
                        )
                        sessionManager.start(session)
                        AuthResult.Success(session, "Account created")
                    },
                    onFailure = { AuthResult.Failure(it.message ?: "Could not create the account") }
                )
        }

        if (userDao.findByEmail(email.trim()) != null) {
            return AuthResult.Failure("An account with this email already exists")
        }
        val now = System.currentTimeMillis()
        val user = UserEntity(
            id = UUID.randomUUID().toString(),
            email = email.trim().lowercase(),
            displayName = displayName.trim().ifBlank { email.trim().substringBefore('@') },
            passwordHash = PasswordHasher.hash(password.toCharArray()),
            createdAt = now,
            lastLoginAt = now
        )
        userDao.insert(user)
        val session = Session(
            userId = user.id,
            email = user.email,
            displayName = user.displayName,
            token = null,
            remote = false
        )
        sessionManager.start(session)
        return AuthResult.Success(session, "Welcome, ${user.displayName}")
    }

    suspend fun signIn(email: String, password: String): AuthResult {
        val validation = validateCredentials(email, password)
        if (validation != null) return AuthResult.Failure(validation)

        val settings = settingsStore.settings.first()
        if (settings.remoteAuth && backendClient != null) {
            return backendClient.login(email, password)
                .fold(
                    onSuccess = { response ->
                        val session = Session(
                            userId = response.userId,
                            email = response.email,
                            displayName = response.displayName,
                            token = response.token,
                            remote = true
                        )
                        sessionManager.start(session)
                        AuthResult.Success(session)
                    },
                    onFailure = { AuthResult.Failure(it.message ?: "Sign in failed") }
                )
        }

        val user = userDao.findByEmail(email.trim().lowercase())
            ?: return AuthResult.Failure("No account found for this email")
        if (!PasswordHasher.verify(password.toCharArray(), user.passwordHash)) {
            return AuthResult.Failure("Incorrect password")
        }
        userDao.touchLogin(user.id, System.currentTimeMillis())
        val session = Session(
            userId = user.id,
            email = user.email,
            displayName = user.displayName,
            token = null,
            remote = false
        )
        sessionManager.start(session)
        return AuthResult.Success(session)
    }

    /**
     * Starts the password reset flow.
     *
     * Backend accounts receive a reset token by mail; local accounts cannot be recovered
     * (the password is only stored as a hash), so the caller is told to remove the local
     * profile and create a new one.
     */
    suspend fun requestPasswordReset(email: String): AuthResult {
        val settings = settingsStore.settings.first()
        if (settings.remoteAuth && backendClient != null) {
            return backendClient.requestPasswordReset(email.trim())
                .fold(
                    onSuccess = { AuthResult.Success(sessionPlaceholder(), "Reset instructions sent") },
                    onFailure = { AuthResult.Failure(it.message ?: "Could not start the reset") }
                )
        }
        val user = userDao.findByEmail(email.trim().lowercase())
            ?: return AuthResult.Failure("No local account found for this email")
        return AuthResult.Failure(
            "This account is stored on this device, so its password cannot be recovered. " +
                "Remove the local account and create a new one to continue."
        )
    }

    /** Removes the local account and every artefact that belongs to it. */
    suspend fun deleteLocalAccount(userId: String) {
        val configs = providerConfigDao.listForUser(userId)
        configs.forEach { credentialVault.delete(it.credentialId) }
        providerConfigDao.deleteAllForUser(userId)
        conversationDao.listForUser(userId).forEach { conversation ->
            messageDao.deleteForConversation(conversation.id)
        }
        conversationDao.deleteAllForUser(userId)
        memoryDao.deleteAllForUser(userId)
        userDao.deleteById(userId)
        sessionManager.clear()
    }

    /** Deletes the signed-in account (local or backend). */
    suspend fun deleteAccount(): AuthResult {
        val session = currentSession() ?: return AuthResult.Failure("Not signed in")
        if (session.remote && backendClient != null) {
            val result = backendClient.deleteAccount(session.token.orEmpty())
            if (result.isFailure) {
                return AuthResult.Failure(result.exceptionOrNull()?.message ?: "Could not delete the account")
            }
        }
        deleteLocalAccount(session.userId)
        return AuthResult.Success(session, "Account deleted")
    }

    suspend fun logout() {
        sessionManager.clear()
    }

    suspend fun updateDisplayName(displayName: String) {
        val session = currentSession() ?: return
        if (!session.remote) {
            userDao.updateDisplayName(session.userId, displayName)
        }
        sessionManager.updateProfile(displayName)
    }

    private fun validateCredentials(email: String, password: String): String? {
        if (!EMAIL_PATTERN.matches(email.trim())) {
            return "Enter a valid email address"
        }
        if (password.length < MIN_PASSWORD_LENGTH) {
            return "Password must be at least $MIN_PASSWORD_LENGTH characters"
        }
        return null
    }

    private fun sessionPlaceholder() = Session("", "", "", null, false)

    companion object {
        const val MIN_PASSWORD_LENGTH = 8
        private val EMAIL_PATTERN = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
    }
}

/** Reads a field from a JSON response body (helper for the UI layer). */
fun parseMessage(raw: String): String =
    Json.parseOrNull(raw)?.optString("message").orEmpty()
