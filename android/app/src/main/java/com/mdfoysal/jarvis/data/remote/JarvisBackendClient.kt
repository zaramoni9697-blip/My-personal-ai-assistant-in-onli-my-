package com.mdfoysal.jarvis.data.remote

import com.mdfoysal.jarvis.core.json.Json
import com.mdfoysal.jarvis.core.json.JsonObjectBuilder
import com.mdfoysal.jarvis.core.json.JsonValue
import com.mdfoysal.jarvis.core.net.HttpRequest
import com.mdfoysal.jarvis.core.net.HttpTransport
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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
    )

    suspend fun login(email: String, password: String): Result<AuthResponse> = post(
        path = "/api/auth/login",
        body = JsonObjectBuilder()
            .put("email", email)
            .put("password", password)
            .build()
    )

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

    private suspend fun post(
        path: String,
        body: JsonValue,
        authToken: String? = null,
        method: String = "POST"
    ): Result<String> = withContext(Dispatchers.IO) {
        val base = baseUrlProvider().trim().trimEnd('/')
        if (base.isEmpty()) {
            return@withContext Result.failure(IllegalStateException("Backend URL is not configured"))
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
                ?: "Request failed (HTTP ${response.code})"
            return@withContext Result.failure(IllegalStateException(message))
        }
        Result.success(response.body)
    }
}
