package com.mdfoysal.jarvis.data.security

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.sessionStore: DataStore<Preferences> by preferencesDataStore(name = "jarvis_session")

/** The signed-in session. */
data class Session(
    val userId: String,
    val email: String,
    val displayName: String,
    val token: String?,
    val remote: Boolean
) {
    val isSignedIn: Boolean get() = userId.isNotBlank()
}

/**
 * Persists the active session (local accounts and backend-issued tokens).
 * The token is only present for remote (backend) sign-in.
 */
class SessionManager(private val context: Context) {

    private object Keys {
        val USER_ID = stringPreferencesKey("user_id")
        val EMAIL = stringPreferencesKey("email")
        val DISPLAY_NAME = stringPreferencesKey("display_name")
        val TOKEN = stringPreferencesKey("token")
        val REMOTE = stringPreferencesKey("remote")
    }

    val session: Flow<Session?> = context.sessionStore.data.map { prefs ->
        val userId = prefs[Keys.USER_ID]
        if (userId.isNullOrBlank()) {
            null
        } else {
            Session(
                userId = userId,
                email = prefs[Keys.EMAIL].orEmpty(),
                displayName = prefs[Keys.DISPLAY_NAME].orEmpty(),
                token = prefs[Keys.TOKEN],
                remote = prefs[Keys.REMOTE] == "1"
            )
        }
    }

    suspend fun start(session: Session) {
        context.sessionStore.edit { prefs ->
            prefs[Keys.USER_ID] = session.userId
            prefs[Keys.EMAIL] = session.email
            prefs[Keys.DISPLAY_NAME] = session.displayName
            prefs[Keys.REMOTE] = if (session.remote) "1" else "0"
            session.token?.let { prefs[Keys.TOKEN] = it } ?: prefs.remove(Keys.TOKEN)
        }
    }

    suspend fun updateProfile(displayName: String) {
        context.sessionStore.edit { prefs -> prefs[Keys.DISPLAY_NAME] = displayName }
    }

    suspend fun clear() {
        context.sessionStore.edit { it.clear() }
    }
}
