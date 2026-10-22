package com.fredrickemmanuel.studyzen.data.ai

/**
 * A small, forgiving JSON reader for what AI models write. They often wrap JSON in ```json fences,
 * add a sentence before it, or leave a trailing comma, which strict parsers reject. Values come
 * back as Map<String, Any?>, List<Any?>, String, Double, Boolean or null.
 */
object MiniJson {

    /** Finds the first JSON object or array in [text] and reads it. Null when there is none. */
    fun extract(text: String): Any? {
        val start = text.indexOfFirst { it == '{' || it == '[' }
        if (start < 0) return null
        return try {
            Reader(text, start).readValue()
        } catch (e: ParseError) {
            null
        }
    }

    private class ParseError(message: String) : Exception(message)

    private class Reader(val s: String, var i: Int) {
        fun readValue(): Any? {
            skip()
            if (i >= s.length) throw ParseError("end")
            return when (val c = s[i]) {
                '{' -> readObject()
                '[' -> readArray()
                '"' -> readString()
                't', 'f', 'n' -> readWord()
                else -> if (c == '-' || c.isDigit()) readNumber() else throw ParseError("unexpected $c")
            }
        }

        private fun skip() {
            while (i < s.length && s[i].isWhitespace()) i++
        }

        private fun readObject(): Map<String, Any?> {
            val map = LinkedHashMap<String, Any?>()
            i++ // {
            while (true) {
                skip()
                if (i >= s.length) throw ParseError("unterminated object")
                if (s[i] == '}') { i++; return map }
                if (s[i] == ',') { i++; continue }
                if (s[i] != '"') throw ParseError("expected key")
                val key = readString()
                skip()
                if (i >= s.length || s[i] != ':') throw ParseError("expected colon")
                i++
                map[key] = readValue()
            }
        }

        private fun readArray(): List<Any?> {
            val list = ArrayList<Any?>()
            i++ // [
            while (true) {
                skip()
                if (i >= s.length) throw ParseError("unterminated array")
                if (s[i] == ']') { i++; return list }
                if (s[i] == ',') { i++; continue }
                list += readValue()
            }
        }

        private fun readString(): String {
            val out = StringBuilder()
            i++ // opening quote
            while (i < s.length) {
                val c = s[i++]
                when (c) {
                    '"' -> return out.toString()
                    '\\' -> {
                        if (i >= s.length) throw ParseError("bad escape")
                        when (val e = s[i++]) {
                            'n' -> out.append('\n')
                            't' -> out.append('\t')
                            'r' -> out.append('\r')
                            'b' -> out.append('\b')
                            'f' -> out.append('\u000C')
                            'u' -> {
                                if (i + 4 > s.length) throw ParseError("bad unicode")
                                val code = s.substring(i, i + 4).toIntOrNull(16) ?: throw ParseError("bad unicode")
                                out.append(code.toChar())
                                i += 4
                            }
                            else -> out.append(e) // \" \\ \/
                        }
                    }
                    else -> out.append(c)
                }
            }
            throw ParseError("unterminated string")
        }

        private fun readWord(): Any? {
            for ((word, value) in listOf("true" to true, "false" to false, "null" to null)) {
                if (s.startsWith(word, i)) { i += word.length; return value }
            }
            throw ParseError("bad word")
        }

        private fun readNumber(): Double {
            val start = i
            while (i < s.length && (s[i].isDigit() || s[i] in "+-.eE")) i++
            return s.substring(start, i).toDoubleOrNull() ?: throw ParseError("bad number")
        }
    }
}

// --- Small helpers for reading the parsed value -------------------------------------------------

@Suppress("UNCHECKED_CAST")
internal fun Any?.asMap(): Map<String, Any?>? = this as? Map<String, Any?>

@Suppress("UNCHECKED_CAST")
internal fun Any?.asList(): List<Any?>? = this as? List<Any?>

internal fun Any?.asText(): String? = when (this) {
    is String -> this.trim().takeIf { it.isNotEmpty() }
    is Double -> if (this % 1.0 == 0.0) this.toLong().toString() else this.toString()
    is Boolean -> this.toString()
    else -> null
}

internal fun Any?.asInt(): Int? = when (this) {
    is Double -> this.toInt()
    is String -> this.trim().toIntOrNull()
    else -> null
}

/** A list of non-blank strings (a single string becomes a one-item list). */
internal fun Any?.asTextList(): List<String> = when (this) {
    is List<*> -> this.mapNotNull { it.asText() }
    is String -> listOfNotNull(this.asText())
    else -> emptyList()
}
