package verify

import com.mdfoysal.jarvis.core.net.HttpRequest
import com.mdfoysal.jarvis.core.net.HttpResponse
import com.mdfoysal.jarvis.core.net.HttpTransport
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.runBlocking

/**
 * A real HTTP transport built on `HttpURLConnection`.
 *
 * Used by the verification suite so provider adapters are exercised over an actual socket
 * against a local HTTP server (not a mocked object graph).
 */
class JdkHttpTransport(
    private val streamHandler: ((HttpRequest) -> Unit)? = null
) : HttpTransport {

    override suspend fun execute(request: HttpRequest): HttpResponse = runBlocking {
        executeBlocking(request)
    }

    override suspend fun stream(request: HttpRequest): Sequence<String> = runBlocking {
        streamBlocking(request).asSequence()
    }

    fun executeBlocking(request: HttpRequest): HttpResponse {
        val connection = open(request)
        return try {
            request.body?.let { payload ->
                connection.outputStream.use { it.write(payload.toByteArray(Charsets.UTF_8)) }
            }
            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val text = stream?.bufferedReader()?.use { it.readText() } ?: ""
            HttpResponse(code = code, body = text)
        } catch (e: Exception) {
            HttpResponse.networkFailure(e.message ?: e.javaClass.simpleName)
        } finally {
            connection.disconnect()
        }
    }

    fun streamBlocking(request: HttpRequest): List<String> {
        val connection = open(request)
        return try {
            request.body?.let { payload ->
                connection.outputStream.use { it.write(payload.toByteArray(Charsets.UTF_8)) }
            }
            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val chunks = ArrayList<String>()
            stream?.let { input ->
                BufferedReader(InputStreamReader(input)).use { reader ->
                    var line = reader.readLine()
                    while (line != null) {
                        chunks.add(line + "\n")
                        line = reader.readLine()
                    }
                }
            }
            if (code !in 200..299) {
                throw IllegalStateException("HTTP $code: ${chunks.joinToString("")}")
            }
            chunks
        } finally {
            connection.disconnect()
        }
    }

    private fun open(request: HttpRequest): HttpURLConnection {
        streamHandler?.invoke(request)
        val connection = URL(request.url).openConnection() as HttpURLConnection
        connection.requestMethod = request.method
        connection.connectTimeout = request.timeoutMs.toInt()
        connection.readTimeout = request.timeoutMs.toInt()
        connection.doInput = true
        if (request.body != null) connection.doOutput = true
        request.headers.forEach { (key, value) -> connection.setRequestProperty(key, value) }
        return connection
    }
}
