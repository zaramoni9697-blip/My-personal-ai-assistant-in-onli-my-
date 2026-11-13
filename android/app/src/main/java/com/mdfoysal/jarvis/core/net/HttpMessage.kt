package com.mdfoysal.jarvis.core.net

/**
 * A transport-agnostic HTTP request. The core layer never touches Android's OkHttp directly;
 * the transport is injected, which keeps the provider adapters unit-testable on a plain JVM.
 */
data class HttpRequest(
    val url: String,
    val method: String = "GET",
    val headers: Map<String, String> = emptyMap(),
    val body: String? = null,
    val timeoutMs: Long = 60_000
) {
    /** Query string portion of the URL (without the leading `?`). */
    val query: String get() = url.substringAfter('?', "")

    val path: String get() = url.substringBefore('?')
}

/** A transport-agnostic HTTP response. */
data class HttpResponse(
    val code: Int,
    val body: String,
    val headers: Map<String, String> = emptyMap(),
    val errorMessage: String? = null
) {
    val isSuccessful: Boolean get() = code in 200..299

    companion object {
        fun networkFailure(message: String): HttpResponse =
            HttpResponse(code = 0, body = "", errorMessage = message)
    }
}

/**
 * Pluggable HTTP transport.
 *
 * Android provides an OkHttp-backed implementation; the verification harness provides a
 * `HttpURLConnection`/JDK implementation so provider adapters can be exercised for real.
 */
interface HttpTransport {
    suspend fun execute(request: HttpRequest): HttpResponse

    /** Streaming transport: emits raw body chunks as they arrive. */
    suspend fun stream(request: HttpRequest): Sequence<String>
}
