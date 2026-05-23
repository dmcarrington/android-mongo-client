package com.dmc.mongoclient.data.mongo

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class JsonSyntaxTest {

    @Test
    fun `recognises a key followed by a string value`() {
        val text = """{ "name": "alice" }"""
        val tokens = tokenizeJson(text)
        val kinds = tokens.map { it.kind }
        assertEquals(listOf(JsonTokenKind.KEY, JsonTokenKind.STRING), kinds)
    }

    @Test
    fun `tokenises numbers including negative, decimal and exponent`() {
        val text = """{ "a": 42, "b": -3.14, "c": 1.5e10 }"""
        val numbers = tokenizeJson(text).filter { it.kind == JsonTokenKind.NUMBER }
            .map { text.substring(it.start, it.end) }
        assertEquals(listOf("42", "-3.14", "1.5e10"), numbers)
    }

    @Test
    fun `recognises true false null as keywords`() {
        val text = """{ "x": true, "y": false, "z": null }"""
        val keywords = tokenizeJson(text).filter { it.kind == JsonTokenKind.KEYWORD }
            .map { text.substring(it.start, it.end) }
        assertEquals(listOf("true", "false", "null"), keywords)
    }

    @Test
    fun `handles escaped quotes inside strings`() {
        val text = """{ "msg": "she said \"hi\"" }"""
        val strings = tokenizeJson(text).filter { it.kind == JsonTokenKind.STRING }
        assertEquals(1, strings.size)
        assertEquals("\"she said \\\"hi\\\"\"", text.substring(strings[0].start, strings[0].end))
    }

    @Test
    fun `treats ObjectId extended form as nested key + string`() {
        val text = """{ "_id": { "${'$'}oid": "abc123" } }"""
        val tokens = tokenizeJson(text)
        val keys = tokens.filter { it.kind == JsonTokenKind.KEY }.map { text.substring(it.start, it.end) }
        assertTrue("\"_id\" is a key", "\"_id\"" in keys)
        assertTrue("\"\$oid\" is a key", "\"\$oid\"" in keys)
    }

    @Test
    fun `tolerates partially typed input without crashing`() {
        tokenizeJson("""{ "name": "ali""")
        tokenizeJson("""{ "x": tru""")
        tokenizeJson("")
        // No assertions — just exercises the lexer with malformed input.
    }
}
