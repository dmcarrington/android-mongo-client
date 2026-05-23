package com.dmc.mongoclient.data.mongo

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ConnectionStringParserTest {

    private val parser = ConnectionStringParser()

    @Test
    fun `parses standard mongodb URI`() {
        val r = parser.parse("mongodb://user:secret@host:27017/db")
        assertTrue(r is ParseResult.Ok)
        r as ParseResult.Ok
        assertFalse(r.isSrv)
        assertEquals("mongodb://user:****@host:27017/db", r.redactedForDisplay)
        assertEquals("host:27017", r.host)
    }

    @Test
    fun `parses mongodb+srv URI`() {
        val r = parser.parse("mongodb+srv://user:secret@cluster.example.net/")
        assertTrue(r is ParseResult.Ok)
        r as ParseResult.Ok
        assertTrue(r.isSrv)
        assertEquals("mongodb+srv://user:****@cluster.example.net/", r.redactedForDisplay)
    }

    @Test
    fun `rejects missing scheme`() {
        val r = parser.parse("user:pass@host/db")
        assertTrue(r is ParseResult.Error)
    }

    @Test
    fun `rejects malformed URI`() {
        val r = parser.parse("mongodb://")
        assertTrue(r is ParseResult.Error)
    }

    @Test
    fun `redacts password with special chars`() {
        val r = parser.parse("mongodb://u:p%40ss%21word@h:27017/")
        assertTrue(r is ParseResult.Ok)
        r as ParseResult.Ok
        assertEquals("mongodb://u:****@h:27017/", r.redactedForDisplay)
    }

    @Test
    fun `redactPassword leaves non-password URI alone`() {
        val u = "mongodb://host:27017/db"
        assertEquals(u, parser.redactPassword(u))
    }
}
