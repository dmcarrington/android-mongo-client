package com.dmc.mongoclient.data.csv

import java.io.StringReader
import java.io.StringWriter
import org.junit.Assert.assertEquals
import org.junit.Test

class CsvCodecTest {

    @Test
    fun `writes simple row`() {
        val out = StringWriter()
        CsvCodec.writeRow(out, listOf("a", "b", "c"))
        assertEquals("a,b,c\r\n", out.toString())
    }

    @Test
    fun `quotes fields containing commas`() {
        val out = StringWriter()
        CsvCodec.writeRow(out, listOf("a", "b,c", "d"))
        assertEquals("a,\"b,c\",d\r\n", out.toString())
    }

    @Test
    fun `escapes embedded quotes by doubling`() {
        val out = StringWriter()
        CsvCodec.writeRow(out, listOf("she said \"hi\""))
        assertEquals("\"she said \"\"hi\"\"\"\r\n", out.toString())
    }

    @Test
    fun `quotes fields with newlines`() {
        val out = StringWriter()
        CsvCodec.writeRow(out, listOf("line1\nline2"))
        assertEquals("\"line1\nline2\"\r\n", out.toString())
    }

    @Test
    fun `reads simple csv`() {
        val rows = CsvCodec.read(StringReader("a,b,c\r\n1,2,3\r\n"))
        assertEquals(listOf(listOf("a", "b", "c"), listOf("1", "2", "3")), rows)
    }

    @Test
    fun `reads quoted fields with commas`() {
        val rows = CsvCodec.read(StringReader("a,\"b,c\",d\r\n"))
        assertEquals(listOf(listOf("a", "b,c", "d")), rows)
    }

    @Test
    fun `reads doubled quotes as single quote`() {
        val rows = CsvCodec.read(StringReader("\"she said \"\"hi\"\"\"\r\n"))
        assertEquals(listOf(listOf("she said \"hi\"")), rows)
    }

    @Test
    fun `reads newline inside quoted field`() {
        val rows = CsvCodec.read(StringReader("\"line1\nline2\",x\r\n"))
        assertEquals(listOf(listOf("line1\nline2", "x")), rows)
    }

    @Test
    fun `tolerates LF only line endings`() {
        val rows = CsvCodec.read(StringReader("a,b\n1,2\n"))
        assertEquals(listOf(listOf("a", "b"), listOf("1", "2")), rows)
    }

    @Test
    fun `round trips through write then read`() {
        val original = listOf(
            listOf("name", "note", "count"),
            listOf("alice", "uses, commas", "1"),
            listOf("bob", "has \"quotes\"", "2"),
            listOf("carol", "has\nnewlines", "3"),
        )
        val out = StringWriter()
        original.forEach { CsvCodec.writeRow(out, it) }
        val parsed = CsvCodec.read(StringReader(out.toString()))
        assertEquals(original, parsed)
    }
}
