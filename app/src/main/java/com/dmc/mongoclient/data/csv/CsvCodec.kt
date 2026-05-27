package com.dmc.mongoclient.data.csv

import java.io.Reader
import java.io.Writer

/**
 * Minimal RFC 4180-ish CSV codec. Comma delimiter, double-quote enclosure,
 * `""` for embedded quotes, supports newlines inside quoted fields.
 * No header semantics — callers handle that themselves.
 */
object CsvCodec {

    fun writeRow(writer: Writer, row: List<String>) {
        row.forEachIndexed { i, cell ->
            if (i > 0) writer.write(",")
            writer.write(escape(cell))
        }
        writer.write("\r\n")
    }

    fun read(reader: Reader): List<List<String>> {
        val rows = ArrayList<List<String>>()
        val cur = StringBuilder()
        var row = ArrayList<String>()
        var inQuotes = false
        var lookahead = -1

        fun nextChar(): Int = if (lookahead != -1) {
            val c = lookahead; lookahead = -1; c
        } else reader.read()

        fun pushCell() { row.add(cur.toString()); cur.setLength(0) }
        fun pushRow() { pushCell(); rows.add(row); row = ArrayList() }

        while (true) {
            val c = nextChar()
            if (c == -1) {
                // Don't emit an empty trailing row if the file ended cleanly on a newline.
                if (cur.isNotEmpty() || row.isNotEmpty()) pushRow()
                break
            }
            val ch = c.toChar()
            if (inQuotes) {
                if (ch == '"') {
                    val peek = reader.read()
                    if (peek == '"'.code) cur.append('"') else { inQuotes = false; lookahead = peek }
                } else {
                    cur.append(ch)
                }
            } else {
                when (ch) {
                    ',' -> pushCell()
                    '\n' -> pushRow()
                    '\r' -> {
                        // Treat \r\n as one newline; lone \r as a newline too.
                        val peek = reader.read()
                        if (peek != '\n'.code && peek != -1) lookahead = peek
                        pushRow()
                    }
                    '"' -> if (cur.isEmpty()) inQuotes = true else cur.append('"')
                    else -> cur.append(ch)
                }
            }
        }
        return rows
    }

    private fun escape(s: String): String {
        val needsQuoting = s.any { it == ',' || it == '"' || it == '\n' || it == '\r' }
        if (!needsQuoting) return s
        return "\"" + s.replace("\"", "\"\"") + "\""
    }
}
