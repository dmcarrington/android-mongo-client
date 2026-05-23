package com.dmc.mongoclient.data.mongo

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class JsonFormatTest {

    @Test
    fun `parses a simple object`() {
        val doc = JsonFormat.parseObject("""{ "name": "alice", "age": 30 }""")
        assertEquals("alice", doc.getString("name"))
        assertEquals(30, doc.getInteger("age"))
    }

    @Test
    fun `rejects empty input`() {
        try {
            JsonFormat.parseObject("   ")
            fail("Expected IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message?.contains("Empty") == true)
        }
    }

    @Test
    fun `rejects JSON array at top level`() {
        try {
            JsonFormat.parseObject("[1, 2, 3]")
            fail("Expected IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message?.contains("object") == true)
        }
    }

    @Test
    fun `rejects malformed JSON with message`() {
        try {
            JsonFormat.parseObject("""{ "name": "alice" """)
            fail("Expected IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            assertTrue("error has a message", !e.message.isNullOrBlank())
        }
    }

    @Test
    fun `renders pretty with indent`() {
        val doc = JsonFormat.parseObject("""{"a":1,"b":[1,2]}""")
        val out = JsonFormat.render(doc)
        assertTrue("multi-line output", out.contains('\n'))
        assertTrue("indented with two spaces", out.contains("  \"a\""))
    }
}
