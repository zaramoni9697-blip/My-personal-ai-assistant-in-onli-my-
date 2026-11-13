package com.mdfoysal.jarvis.data.remote

import com.mdfoysal.jarvis.core.net.HttpRequest
import com.mdfoysal.jarvis.core.net.HttpResponse
import com.mdfoysal.jarvis.core.net.HttpTransport
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * OkHttp backed [HttpTransport].
 *
 * This is the only place where the app talks to the network. Redirects are disabled so that
 * an API key is never replayed to an unexpected host, and error bodies are always read so the
 * provider error mapper can produce a useful message.
 */
class OkHttpTransport(
    private val client: OkHttpClient = defaultClient()
) : HttpTransport {

    override suspend fun execute(request: HttpRequest): HttpResponse = withContext(Dispatchers.IO) {
        try {
            client.newCall(toOkHttp(request)).execute().use { response ->
                HttpResponse(
                    code = response.code,
                    body = response.body?.string().orEmpty(),
                    headers = response.headers.toMap()
                        .mapValues { (_, values) -> values.joinToString(", ") }
                )
            }
        } catch (e: IOException) {
            HttpResponse.networkFailure(e.message ?: "Network unavailable")
        } catch (e: Exception) {
            HttpResponse.networkFailure(e.message ?: e.javaClass.simpleName)
        }
    }

    override suspend fun stream(request: HttpRequest): Sequence<String> = withContext(Dispatchers.IO) {
        val response: Response = client.newCall(toOkHttp(request)).execute()
        if (!response.isSuccessful) {
            val body = response.body?.string().orEmpty()
            response.close()
            throw IOException("HTTP ${response.code}: $body")
        }
        val source = response.body?.source()
        val lines = ArrayList<String>()
        try {
            while (!source!!.exhausted()) {
                val line = source.readUtf8Line() ?: break
                lines.add(line + "\n")
            }
        } finally {
            response.close()
        }
        lines.asSequence()
    }

    private fun toOkHttp(request: HttpRequest): Request {
        val builder = Request.Builder().url(request.url)
        request.headers.forEach { (key, value) -> builder.header(key, value) }
        val body = request.body
        when {
            body != null -> builder.method(
                request.method,
                body.toRequestBody(JSON_MEDIA_TYPE)
            )
            request.method == "GET" || request.method == "HEAD" -> builder.method(request.method, null)
            else -> builder.method(request.method, "".toRequestBody(JSON_MEDIA_TYPE))
        }
        return builder.build()
    }

    companion object {
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

        fun defaultClient(): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .followRedirects(false)
            .followSslRedirects(false)
            .build()
    }
}
