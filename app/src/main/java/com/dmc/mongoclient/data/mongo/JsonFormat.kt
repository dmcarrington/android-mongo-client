package com.dmc.mongoclient.data.mongo

import org.bson.Document
import org.bson.json.JsonMode
import org.bson.json.JsonWriterSettings

/**
 * Shared JSON serialiser. Uses MongoDB Extended JSON v2 in *relaxed* mode so
 * common types render readably:
 *   - `ObjectId` → `{"$oid":"..."}`
 *   - `Date`     → `{"$date":"2024-..."}`
 *   - `Long`/`Int` → plain number
 *
 * Round-trips back through `Document.parse(...)` cleanly because the driver
 * understands the same shape.
 */
object JsonFormat {

    val pretty: JsonWriterSettings = JsonWriterSettings.builder()
        .outputMode(JsonMode.RELAXED)
        .indent(true)
        .indentCharacters("  ")
        .build()

    fun render(document: Document): String = document.toJson(pretty)

    /**
     * Parses `text` as a JSON document. Throws `IllegalArgumentException` with
     * a message suitable for inline display when the input is not a valid
     * JSON object.
     */
    fun parseObject(text: String): Document {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) throw IllegalArgumentException("Empty input")
        if (!trimmed.startsWith("{")) {
            throw IllegalArgumentException("Document must be a JSON object (starts with '{')")
        }
        return try {
            Document.parse(trimmed)
        } catch (e: Throwable) {
            throw IllegalArgumentException(e.message ?: "Invalid JSON", e)
        }
    }
}
