package com.jarvis.ai.data.repository

import com.jarvis.ai.data.local.db.UserDao
import com.jarvis.ai.data.local.db.UserEntity
import com.jarvis.ai.data.local.datastore.PreferencesManager
import com.jarvis.ai.data.security.CryptoManager
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton
import java.util.regex.Pattern

@Singleton
class AuthRepository @Inject constructor(
    private val userDao: UserDao,
    private val prefs: PreferencesManager,
    private val crypto: CryptoManager
) {
    private val emailPattern = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\$")

    suspend fun signUp(email: String, password: String, displayName: String): Result<UserEntity> {
        if (!emailPattern.matcher(email).matches()) return Result.failure(Exception("Invalid email format"))
        if (password.length < 6) return Result.failure(Exception("Password must be at least 6 characters"))
        if (displayName.isBlank()) return Result.failure(Exception("Display name required"))
        if (userDao.getByEmail(email) != null) return Result.failure(Exception("Account already exists"))

        val user = UserEntity(email = email, displayName = displayName)
        userDao.insert(user)
        // Store password hash securely (simple demo - production should use bcrypt on backend)
        crypto.saveString("pwd_${user.id}", hash(password))
        prefs.setCurrentUser(user.id, user.email)
        return Result.success(user)
    }

    suspend fun login(email: String, password: String): Result<UserEntity> {
        val user = userDao.getByEmail(email) ?: return Result.failure(Exception("Account not found"))
        val stored = crypto.getString("pwd_${user.id}") ?: return Result.failure(Exception("Invalid credentials"))
        if (stored != hash(password)) return Result.failure(Exception("Incorrect password"))
        prefs.setCurrentUser(user.id, user.email)
        return Result.success(user)
    }

    suspend fun forgotPassword(email: String): Result<String> {
        val user = userDao.getByEmail(email) ?: return Result.failure(Exception("No account with this email"))
        // In production: send reset email via Firebase. Here simulate success.
        return Result.success("Password reset instructions sent to $email (simulated)")
    }

    suspend fun logout() { prefs.clearCurrentUser() }

    suspend fun getCurrentUser(): UserEntity? {
        val id = prefs.currentUserId.first() ?: return null
        return userDao.getById(id)
    }

    suspend fun deleteAccount(userId: String) {
        userDao.deleteById(userId)
        crypto.remove("pwd_$userId")
        prefs.clearCurrentUser()
    }

    private fun hash(s: String): String = s.reversed() + s.length // placeholder; use real hash in prod
}
