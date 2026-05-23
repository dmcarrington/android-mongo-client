package com.dmc.mongoclient.data.mongo

/**
 * Lightweight JSON tokeniser for syntax highlighting. Not a parser — it
 * doesn't validate structure, just identifies coloured spans inside a string.
 * Tolerant of partially-edited input (the editor calls this on every
 * keystroke).
 */
enum class JsonTokenKind { KEY, STRING, NUMBER, KEYWORD }

data class JsonToken(val start: Int, val end: Int, val kind: JsonTokenKind)

fun tokenizeJson(text: String): List<JsonToken> {
    val tokens = ArrayList<JsonToken>(text.length / 8 + 4)
    val n = text.length
    var i = 0
    while (i < n) {
        when (val c = text[i]) {
            '"' -> i = consumeString(text, i, n, tokens)
            '-' -> {
                if (i + 1 < n && text[i + 1].isDigit()) i = consumeNumber(text, i, n, tokens)
                else i++
            }
            in '0'..'9' -> i = consumeNumber(text, i, n, tokens)
            in 'a'..'z', in 'A'..'Z' -> i = consumeIdentifier(text, i, n, tokens)
            else -> {
                // braces, brackets, commas, colons, whitespace — left unstyled
                i++
            }
        }
    }
    return tokens
}

private fun consumeString(text: String, start: Int, n: Int, tokens: MutableList<JsonToken>): Int {
    var i = start + 1 // consume opening quote
    while (i < n) {
        val ch = text[i]
        when {
            ch == '\\' && i + 1 < n -> i += 2
            ch == '"' -> {
                i++
                break
            }
            else -> i++
        }
    }
    // Determine if this string is a key by looking ahead for a colon.
    var j = i
    while (j < n && text[j].isWhitespace()) j++
    val kind = if (j < n && text[j] == ':') JsonTokenKind.KEY else JsonTokenKind.STRING
    tokens.add(JsonToken(start, i, kind))
    return i
}

private fun consumeNumber(text: String, start: Int, n: Int, tokens: MutableList<JsonToken>): Int {
    var i = start
    if (text[i] == '-') i++
    while (i < n) {
        val ch = text[i]
        if (ch.isDigit() || ch == '.' || ch == 'e' || ch == 'E' || ch == '+' || ch == '-') {
            i++
        } else break
    }
    tokens.add(JsonToken(start, i, JsonTokenKind.NUMBER))
    return i
}

private fun consumeIdentifier(text: String, start: Int, n: Int, tokens: MutableList<JsonToken>): Int {
    var i = start
    while (i < n && text[i].isLetter()) i++
    val word = text.substring(start, i)
    if (word == "true" || word == "false" || word == "null") {
        tokens.add(JsonToken(start, i, JsonTokenKind.KEYWORD))
    }
    // Other identifiers (unquoted things inside a value) get no styling.
    return i
}
