package com.mdfoysal.jarvis.core.json

/**
 * A dependency-free JSON model + parser + writer.
 *
 * JARVIS deliberately avoids pulling a JSON library into the Android app so that:
 *  - the core layer stays unit-testable on a plain JVM (no Robolectric / no Android SDK needed),
 *  - there are no dependency conflicts with the Android toolchain,
 *  - the exact same parsing rules are shared by every provider adapter.
 */
sealed class JsonValue {

    object Null : JsonValue()

    data class Bool(val value: Boolean) : JsonValue()

    data class Num(val value: Double) : JsonValue()

    data class Str(val value: String) : JsonValue()

    data class Arr(val items: List<JsonValue>) : JsonValue()

    data class Obj(val fields: Map<String, JsonValue>) : JsonValue()

    // ---------------------------------------------------------------- helpers

    val isNull: Boolean get() = this is Null

    fun asStringOrNull(): String? = when (this) {
        is Str -> value
        is Num -> if (value == Math.floor(value) && !value.isInfinite()) value.toLong().toString() else value.toString()
        is Bool -> value.toString()
        else -> null
    }

    fun asDoubleOrNull(): Double? = when (this) {
        is Num -> value
        is Str -> value.trim().toDoubleOrNull()
        is Bool -> if (value) 1.0 else 0.0
        else -> null
    }

    fun asIntOrNull(): Int? = asDoubleOrNull()?.toInt()

    fun asBooleanOrNull(): Boolean? = when (this) {
        is Bool -> value
        is Str -> when (value.lowercase().trim()) {
            "true", "1", "yes" -> true
            "false", "0", "no" -> false
            else -> null
        }
        is Num -> value != 0.0
        else -> null
    }

    fun asArrayOrNull(): List<JsonValue>? = (this as? Arr)?.items

    fun asObjectOrNull(): Map<String, JsonValue>? = (this as? Obj)?.fields

    /**
     * Field lookup that never throws. A literal field name wins; otherwise the name is
     * treated as a dotted path (`error.message`, `choices.0.delta.content`).
     */
    fun field(name: String): JsonValue? =
        (this as? Obj)?.fields?.get(name) ?: path(name)

    fun optString(name: String, fallback: String = ""): String = field(name)?.asStringOrNull() ?: fallback

    fun optDouble(name: String, fallback: Double = 0.0): Double = field(name)?.asDoubleOrNull() ?: fallback

    fun optInt(name: String, fallback: Int = 0): Int = field(name)?.asIntOrNull() ?: fallback

    fun optBoolean(name: String, fallback: Boolean = false): Boolean = field(name)?.asBooleanOrNull() ?: fallback

    fun optArray(name: String): List<JsonValue> = field(name)?.asArrayOrNull() ?: emptyList()

    fun optObject(name: String): Map<String, JsonValue> = field(name)?.asObjectOrNull() ?: emptyMap()

    /** Resolves a dotted path such as `error.message` or `candidates.0.content.parts`. */
    fun path(dottedPath: String): JsonValue? {
        var current: JsonValue = this
        for (segment in dottedPath.split('.')) {
            if (segment.isEmpty()) continue
            current = when (val c = current) {
                is Obj -> c.fields[segment] ?: return null
                is Arr -> {
                    val index = segment.toIntOrNull() ?: return null
                    c.items.getOrNull(index) ?: return null
                }
                else -> return null
            }
        }
        return current
    }

    companion object {
        fun of(value: String): JsonValue = Str(value)
        fun of(value: Boolean): JsonValue = Bool(value)
        fun of(value: Int): JsonValue = Num(value.toDouble())
        fun of(value: Double): JsonValue = Num(value)
        fun ofNullable(value: String?): JsonValue = if (value == null) Null else Str(value)
        fun arrayOf(items: List<JsonValue>): JsonValue = Arr(items)
        fun arrayOfStrings(items: List<String>): JsonValue = Arr(items.map { Str(it) })
        fun obj(vararg pairs: Pair<String, JsonValue>): JsonValue = Obj(linkedMapOf(*pairs))
    }
}

/** Thrown when a JSON payload cannot be parsed. */
class JsonParseException(message: String) : Exception(message)

/** Recursive-descent JSON parser. */
object Json {

    fun parse(text: String): JsonValue {
        val parser = Parser(text)
        val value = parser.parseValue()
        parser.skipWhitespace()
        if (!parser.atEnd()) {
            throw JsonParseException("Unexpected trailing content at index ${parser.index}")
        }
        return value
    }

    fun parseOrNull(text: String?): JsonValue? {
        if (text.isNullOrBlank()) return null
        return try {
            parse(text)
        } catch (e: JsonParseException) {
            null
        }
    }

    /** Serialises a [JsonValue] into compact JSON text. */
    fun encode(value: JsonValue): String {
        val sb = StringBuilder()
        write(value, sb)
        return sb.toString()
    }

    private fun write(value: JsonValue, sb: StringBuilder) {
        when (value) {
            is JsonValue.Null -> sb.append("null")
            is JsonValue.Bool -> sb.append(if (value.value) "true" else "false")
            is JsonValue.Num -> {
                val d = value.value
                if (d.isNaN() || d.isInfinite()) sb.append("null")
                else if (d == Math.floor(d) && Math.abs(d) < 9.007199254740992E15) sb.append(d.toLong().toString())
                else sb.append(d.toString())
            }
            is JsonValue.Str -> writeString(value.value, sb)
            is JsonValue.Arr -> {
                sb.append('[')
                value.items.forEachIndexed { index, item ->
                    if (index > 0) sb.append(',')
                    write(item, sb)
                }
                sb.append(']')
            }
            is JsonValue.Obj -> {
                sb.append('{')
                var first = true
                for ((key, item) in value.fields) {
                    if (!first) sb.append(',')
                    first = false
                    writeString(key, sb)
                    sb.append(':')
                    write(item, sb)
                }
                sb.append('}')
            }
        }
    }

    private fun writeString(value: String, sb: StringBuilder) {
        sb.append('"')
        for (ch in value) {
            when (ch) {
                '"' -> sb.append("\\\"")
                '\\' -> sb.append("\\\\")
                '\n' -> sb.append("\\n")
                '\r' -> sb.append("\\r")
                '\t' -> sb.append("\\t")
                '\b' -> sb.append("\\b")
                '\u000C' -> sb.append("\\f")
                else -> if (ch < ' ') {
                    sb.append("\\u").append(ch.code.toString(16).padStart(4, '0'))
                } else {
                    sb.append(ch)
                }
            }
        }
        sb.append('"')
    }

    private class Parser(private val text: String) {
        var index = 0

        fun atEnd(): Boolean = index >= text.length

        fun skipWhitespace() {
            while (index < text.length && text[index].isWhitespace()) index++
        }

        fun parseValue(): JsonValue {
            skipWhitespace()
            if (atEnd()) throw JsonParseException("Unexpected end of JSON input")
            return when (val ch = text[index]) {
                '{' -> parseObject()
                '[' -> parseArray()
                '"' -> JsonValue.Str(parseString())
                't', 'f' -> parseBoolean()
                'n' -> parseNull()
                else -> if (ch == '-' || ch.isDigit()) parseNumber()
                else throw JsonParseException("Unexpected character '$ch' at index $index")
            }
        }

        private fun parseObject(): JsonValue {
            expect('{')
            val fields = LinkedHashMap<String, JsonValue>()
            skipWhitespace()
            if (peek() == '}') { index++; return JsonValue.Obj(fields) }
            while (true) {
                skipWhitespace()
                if (peek() != '"') throw JsonParseException("Expected object key at index $index")
                val key = parseString()
                skipWhitespace()
                expect(':')
                val value = parseValue()
                fields[key] = value
                skipWhitespace()
                when (peek()) {
                    ',' -> index++
                    '}' -> { index++; return JsonValue.Obj(fields) }
                    else -> throw JsonParseException("Expected ',' or '}' at index $index")
                }
            }
        }

        private fun parseArray(): JsonValue {
            expect('[')
            val items = ArrayList<JsonValue>()
            skipWhitespace()
            if (peek() == ']') { index++; return JsonValue.Arr(items) }
            while (true) {
                val value = parseValue()
                items.add(value)
                skipWhitespace()
                when (peek()) {
                    ',' -> index++
                    ']' -> { index++; return JsonValue.Arr(items) }
                    else -> throw JsonParseException("Expected ',' or ']' at index $index")
                }
            }
        }

        private fun parseString(): String {
            expect('"')
            val sb = StringBuilder()
            while (true) {
                if (atEnd()) throw JsonParseException("Unterminated string")
                val ch = text[index++]
                when (ch) {
                    '"' -> return sb.toString()
                    '\\' -> {
                        if (atEnd()) throw JsonParseException("Unterminated escape sequence")
                        when (val esc = text[index++]) {
                            '"' -> sb.append('"')
                            '\\' -> sb.append('\\')
                            '/' -> sb.append('/')
                            'b' -> sb.append('\b')
                            'f' -> sb.append('\u000C')
                            'n' -> sb.append('\n')
                            'r' -> sb.append('\r')
                            't' -> sb.append('\t')
                            'u' -> {
                                if (index + 4 > text.length) throw JsonParseException("Invalid unicode escape")
                                val hex = text.substring(index, index + 4)
                                index += 4
                                val code = hex.toIntOrNull(16) ?: throw JsonParseException("Invalid unicode escape '$hex'")
                                sb.append(code.toChar())
                            }
                            else -> throw JsonParseException("Invalid escape character '$esc'")
                        }
                    }
                    else -> sb.append(ch)
                }
            }
        }

        private fun parseNumber(): JsonValue {
            val start = index
            if (peek() == '-') index++
            while (!atEnd() && (text[index].isDigit() || text[index] in ".eE+-")) index++
            val raw = text.substring(start, index)
            val d = raw.toDoubleOrNull() ?: throw JsonParseException("Invalid number '$raw'")
            return JsonValue.Num(d)
        }

        private fun parseBoolean(): JsonValue {
            if (text.startsWith("true", index)) { index += 4; return JsonValue.Bool(true) }
            if (text.startsWith("false", index)) { index += 5; return JsonValue.Bool(false) }
            throw JsonParseException("Invalid literal at index $index")
        }

        private fun parseNull(): JsonValue {
            if (text.startsWith("null", index)) { index += 4; return JsonValue.Null }
            throw JsonParseException("Invalid literal at index $index")
        }

        private fun peek(): Char = if (atEnd()) ' ' else text[index]

        private fun expect(expected: Char) {
            if (atEnd() || text[index] != expected) {
                throw JsonParseException("Expected '$expected' at index $index")
            }
            index++
        }
    }
}

/** Small fluent helper for building JSON payloads. */
class JsonObjectBuilder {
    private val fields = LinkedHashMap<String, JsonValue>()

    fun put(key: String, value: JsonValue): JsonObjectBuilder = apply { fields[key] = value }
    fun put(key: String, value: String): JsonObjectBuilder = apply { fields[key] = JsonValue.Str(value) }
    fun put(key: String, value: Int): JsonObjectBuilder = apply { fields[key] = JsonValue.Num(value.toDouble()) }
    fun put(key: String, value: Double): JsonObjectBuilder = apply { fields[key] = JsonValue.Num(value) }
    fun put(key: String, value: Boolean): JsonObjectBuilder = apply { fields[key] = JsonValue.Bool(value) }
    fun putNullable(key: String, value: String?): JsonObjectBuilder =
        apply { fields[key] = value?.let { JsonValue.Str(it) } ?: JsonValue.Null }

    fun build(): JsonValue = JsonValue.Obj(fields)
}
