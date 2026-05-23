package com.dmc.mongoclient.data.mongo

import org.bson.Document
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class IndexParserTest {

    @Test
    fun `parses default _id index`() {
        val raw = Document.parse("""{ "v": 2, "key": { "_id": 1 }, "name": "_id_" }""")
        val info = IndexParser.parse(raw)
        assertEquals("_id_", info.name)
        assertEquals(1, info.keys.size)
        assertEquals("_id", info.keys[0].field)
        assertFalse(info.unique)
        assertFalse(info.sparse)
        assertNull(info.ttlSeconds)
        assertNull(info.partialFilterExpression)
    }

    @Test
    fun `parses compound index`() {
        val raw = Document.parse(
            """{ "v": 2, "key": { "lastName": 1, "firstName": -1 }, "name": "name_idx" }""",
        )
        val info = IndexParser.parse(raw)
        assertEquals(2, info.keys.size)
        assertEquals("lastName", info.keys[0].field)
        assertEquals("firstName", info.keys[1].field)
    }

    @Test
    fun `parses unique index flag`() {
        val raw = Document.parse(
            """{ "v": 2, "key": { "email": 1 }, "name": "email_unique", "unique": true }""",
        )
        val info = IndexParser.parse(raw)
        assertTrue(info.unique)
    }

    @Test
    fun `parses TTL index`() {
        val raw = Document.parse(
            """{ "v": 2, "key": { "createdAt": 1 }, "name": "ttl_idx", "expireAfterSeconds": 3600 }""",
        )
        val info = IndexParser.parse(raw)
        assertEquals(3600L, info.ttlSeconds)
    }

    @Test
    fun `parses partial filter expression`() {
        val raw = Document.parse(
            """{ "v": 2, "key": { "email": 1 }, "name": "p", "partialFilterExpression": { "verified": true } }""",
        )
        val info = IndexParser.parse(raw)
        assertNotNull(info.partialFilterExpression)
        assertTrue("partial JSON mentions field", info.partialFilterExpression!!.contains("verified"))
    }

    @Test
    fun `parses non-numeric key like 2dsphere`() {
        val raw = Document.parse(
            """{ "v": 2, "key": { "loc": "2dsphere" }, "name": "geo_idx" }""",
        )
        val info = IndexParser.parse(raw)
        assertEquals("2dsphere", info.keys[0].value.toString())
    }

    @Test
    fun `raw JSON is preserved`() {
        val raw = Document.parse("""{ "v": 2, "key": { "_id": 1 }, "name": "_id_" }""")
        val info = IndexParser.parse(raw)
        assertTrue(info.raw.contains("\"name\""))
        assertTrue(info.raw.contains("_id_"))
    }
}
