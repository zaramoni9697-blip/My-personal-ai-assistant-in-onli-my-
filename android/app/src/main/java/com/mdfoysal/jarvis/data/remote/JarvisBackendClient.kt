package com.mdfoysal.jarvis.data.remote

import com.mdfoysal.jarvis.core.json.Json
import com.mdfoysal.jarvis.core.json.JsonObjectBuilder
import com.mdfoysal.jarvis.core.json.JsonValue
import com.mdfoysal.jarvis.core.net.HttpRequest
import com.mdfoysal.jarvis.core.net.HttpTransport
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URI

/**
 * Client for the optional JARVIS backend (`web/server`).
 *
 * It is only used when the user enables "Backend accounts" — otherwise JARVIS works fully
 * offline with local accounts. API keys are never sent to the backend by this client.
 */
class JarvisBackendClient(
    private val transport: HttpTransport,
    private val baseUrlProvider: () -> String
) {

    data class AuthResponse(
        val userId: String,
        val email: String,
        val displayName: String,
        val token: String
    )

    suspend fun register(
        email: String,
        password: String,
        displayName: String
    ): Result<AuthResponse> = post(
        path = "/api/auth/register",
        body = JsonObjectBuilder()
            .put("email", email)
            .put("password", password)
            .put("displayName", displayName)
            .build()
    ).mapCatching(::parseAuthResponse)

    suspend fun login(email: String, password: String): Result<AuthResponse> = post(
        path = "/api/auth/login",
        body = JsonObjectBuilder()
            .put("email", email)
            .put("password", password)
            .build()
    ).mapCatching(::parseAuthResponse)

    suspend fun requestPasswordReset(email: String): Result<String> = post(
        path = "/api/auth/forgot-password",
        body = JsonObjectBuilder().put("email", email).build()
    )

    suspend fun resetPassword(email: String, token: String, newPassword: String): Result<String> = post(
        path = "/api/auth/reset-password",
        body = JsonObjectBuilder()
            .put("email", email)
            .put("token", token)
            .put("newPassword", newPassword)
            .build()
    )

    suspend fun deleteAccount(token: String): Result<String> = post(
        path = "/api/auth/account",
        body = JsonValue.Null,
        authToken = token,
        method = "DELETE"
    )

    private fun parseAuthResponse(body: String): AuthResponse {
        val json = Json.parseOrNull(body) ?: error("Invalid authentication response")
        val id = json.optString("user.id")
        val email = json.optString("user.email")
        val name = json.optString("user.displayName")
        val token = json.optString("token")
        require(id.isNotBlank() && email.isNotBlank() && token.isNotBlank()) {
            "Incomplete authentication response"
        }
        return AuthResponse(id, email, name, token)
    }

    private suspend fun post(
        path: String,
        body: JsonValue,
        authToken: String? = null,
        method: String = "POST"
    ): Result<String> = withContext(Dispatchers.IO) {
        val base = baseUrlProvider().trim().trimEnd('/')
        val uri = runCatching { URI(base) }.getOrNull()
        if (uri == null || uri.scheme != "https" || uri.host.isNullOrBlank() ||
            uri.userInfo != null || uri.query != null || uri.fragment != null
        ) {
            return@withContext Result.failure(IllegalStateException("Configure a valid HTTPS backend URL"))
        }
        val headers = LinkedHashMap<String, String>()
        headers["Content-Type"] = "application/json"
        headers["Accept"] = "application/json"
        authToken?.let { headers["Authorization"] = "Bearer $it" }

        val request = HttpRequest(
            url = "$base$path",
            method = method,
            headers = headers,
            body = if (body is JsonValue.Null) null else Json.encode(body)
        )
        val response = transport.execute(request)
        if (!response.isSuccessful) {
            val parsed = Json.parseOrNull(response.body)
            val message = parsed?.optString("error")?.ifBlank { null }
                ?: parsed?.optString("message")
                ?: response.errorMessage?.takeIf { it.isNotBlank() }
                ?: "Request failed (HTTP ${response.code})"
            return@withContext Result.failure(IllegalStateException(message))
        }
        Result.success(response.body)
    }
}
