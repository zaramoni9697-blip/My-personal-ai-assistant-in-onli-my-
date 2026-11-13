package com.mdfoysal.jarvis.core.chat

/**
 * Incremental Server-Sent-Events decoder.
 *
 * Feeds raw chunks in, gets back the `data:` payloads that are complete. Handles chunk
 * boundaries in the middle of a frame, comments, multi-line data fields and the `[DONE]`
 * sentinel used by OpenAI-compatible providers.
 */
class SseDecoder {

    private val buffer = StringBuilder()

    /** Appends a chunk and returns every complete SSE data payload it completed. */
    fun feed(chunk: String): List<String> {
        buffer.append(chunk)
        val payloads = ArrayList<String>()
        while (true) {
            val boundary = indexOfFrameBoundary()
            if (boundary < 0) break
            val frame = buffer.substring(0, boundary)
            buffer.delete(0, boundary + 2)
            extractData(frame)?.let { payloads.add(it) }
        }
        return payloads
    }

    /** Flushes a trailing frame that was not terminated by a blank line. */
    fun flush(): List<String> {
        if (buffer.isEmpty()) return emptyList()
        val frame = buffer.toString()
        buffer.setLength(0)
        val data = extractData(frame) ?: return emptyList()
        return listOf(data)
    }

    private fun indexOfFrameBoundary(): Int {
        var i = 0
        while (i < buffer.length - 1) {
            if (buffer[i] == '\n' && buffer[i + 1] == '\n') return i
            if (buffer[i] == '\r' && buffer[i + 1] == '\n' && i + 2 < buffer.length && buffer[i + 2] == '\n') return i
            i++
        }
        return -1
    }

    private fun extractData(frame: String): String? {
        val lines = frame.lines()
        val dataLines = ArrayList<String>()
        for (line in lines) {
            val trimmed = line.trimEnd('\r')
            when {
                trimmed.startsWith("data:") -> dataLines.add(trimmed.removePrefix("data:").trimStart())
                trimmed.startsWith(":") -> Unit // comment / keep-alive
                else -> Unit
            }
        }
        if (dataLines.isEmpty()) return null
        return dataLines.joinToString("\n")
    }
}
