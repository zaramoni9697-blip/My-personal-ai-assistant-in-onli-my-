package verify

import com.mdfoysal.jarvis.core.net.HttpRequest
import com.mdfoysal.jarvis.core.net.HttpResponse
import com.mdfoysal.jarvis.core.net.HttpTransport
import com.mdfoysal.jarvis.data.remote.JarvisBackendClient
import kotlinx.coroutines.runBlocking

object BackendClientChecks {
    fun run() = runBlocking {
        TestKit.suite("Backend authentication client")
        var sent: HttpRequest? = null
        var reply = HttpResponse(200, """{"token":"test-token","user":{"id":"user-1","email":"test@example.org","displayName":"Test"}}""")
        val transport = object : HttpTransport {
            override suspend fun execute(request: HttpRequest): HttpResponse { sent = request; return reply }
            override suspend fun stream(request: HttpRequest): Sequence<String> = emptySequence()
        }
        val client = JarvisBackendClient(transport) { "https://example.org" }
        val result = client.login("test@example.org", "password123").getOrThrow()
        TestKit.checkEquals("user-1", result.userId, "login parses user id")
        TestKit.checkEquals("test-token", result.token, "login parses token")
        TestKit.checkEquals("https://example.org/api/auth/login", sent?.url, "login posts to backend")
        val signup = client.register("test@example.org", "password123", "Test").getOrThrow()
        TestKit.checkEquals("Test", signup.displayName, "registration parses display name")
        reply = HttpResponse(200, """{"user":{"id":"user-1"}}""")
        TestKit.check(client.login("test@example.org", "password123").isFailure, "incomplete auth response rejected")
        reply = HttpResponse.networkFailure("Offline")
        TestKit.checkEquals("Offline", client.login("test@example.org", "password123").exceptionOrNull()?.message, "network failure surfaced")
        sent = null
        val insecure = JarvisBackendClient(transport) { "http://example.org" }
        TestKit.check(insecure.login("test@example.org", "password123").isFailure, "insecure backend rejected")
        TestKit.check(sent == null, "insecure credentials never sent")
    }
}
