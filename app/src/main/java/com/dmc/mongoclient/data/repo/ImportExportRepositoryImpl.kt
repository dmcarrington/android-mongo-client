package com.dmc.mongoclient.data.repo

import com.dmc.mongoclient.data.csv.CsvCodec
import com.dmc.mongoclient.data.mongo.JsonFormat
import com.dmc.mongoclient.data.mongo.MongoClientHolder
import com.dmc.mongoclient.domain.model.ExportResult
import com.dmc.mongoclient.domain.model.ImportResult
import com.dmc.mongoclient.domain.repo.ImportExportRepository
import com.mongodb.kotlin.client.coroutine.MongoClient
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStream
import java.io.OutputStreamWriter
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.withContext
import org.bson.Document
import org.bson.json.JsonMode
import org.bson.json.JsonWriterSettings

@Singleton
class ImportExportRepositoryImpl @Inject constructor(
    private val clientHolder: MongoClientHolder,
) : ImportExportRepository {

    override suspend fun exportJson(
        database: String,
        collection: String,
        filter: String,
        output: OutputStream,
    ): ExportResult = withContext(Dispatchers.IO) {
        val docs = fetchAll(database, collection, filter)
        BufferedWriter(OutputStreamWriter(output, Charsets.UTF_8)).use { w ->
            w.write("[\n")
            docs.forEachIndexed { i, doc ->
                if (i > 0) w.write(",\n")
                w.write(doc.toJson(EXPORT_JSON))
            }
            w.write("\n]\n")
        }
        ExportResult(exported = docs.size)
    }

    override suspend fun exportCsv(
        database: String,
        collection: String,
        filter: String,
        output: OutputStream,
    ): ExportResult = withContext(Dispatchers.IO) {
        val docs = fetchAll(database, collection, filter)
        // Column union across the result set, preserving first-seen order.
        val columns = LinkedHashSet<String>()
        docs.forEach { columns.addAll(it.keys) }
        val orderedColumns = columns.toList()

        BufferedWriter(OutputStreamWriter(output, Charsets.UTF_8)).use { w ->
            CsvCodec.writeRow(w, orderedColumns)
            docs.forEach { doc ->
                val row = orderedColumns.map { col -> renderCell(doc[col]) }
                CsvCodec.writeRow(w, row)
            }
        }
        ExportResult(exported = docs.size)
    }

    override suspend fun importJson(
        database: String,
        collection: String,
        input: InputStream,
    ): ImportResult = withContext(Dispatchers.IO) {
        val text = input.bufferedReader(Charsets.UTF_8).use { it.readText() }
        val docs = mutableListOf<Document>()
        val errors = mutableListOf<String>()

        val firstNonWs = text.indexOfFirst { !it.isWhitespace() }
        if (firstNonWs == -1) return@withContext ImportResult(0, listOf("File is empty"))

        when (text[firstNonWs]) {
            '[' -> {
                // JSON array — parse via BSON Document, which handles top-level arrays
                // by parsing each element with Document.parse after a minor unwrap.
                try {
                    splitJsonArray(text.substring(firstNonWs)).forEachIndexed { i, frag ->
                        runCatching { Document.parse(frag) }
                            .onSuccess { docs.add(it) }
                            .onFailure { errors.add("Element ${i + 1}: ${it.message}") }
                    }
                } catch (e: Exception) {
                    errors.add("Failed to split JSON array: ${e.message}")
                }
            }
            '{' -> {
                // NDJSON — one doc per non-empty line.
                text.lineSequence().forEachIndexed { idx, raw ->
                    val line = raw.trim()
                    if (line.isEmpty()) return@forEachIndexed
                    runCatching { Document.parse(line) }
                        .onSuccess { docs.add(it) }
                        .onFailure { errors.add("Line ${idx + 1}: ${it.message}") }
                }
            }
            else -> errors.add("Unrecognised JSON: must start with '[' or '{'")
        }

        if (docs.isEmpty()) return@withContext ImportResult(0, errors)
        insertAll(database, collection, docs, errors)
    }

    override suspend fun importCsv(
        database: String,
        collection: String,
        input: InputStream,
    ): ImportResult = withContext(Dispatchers.IO) {
        val rows = BufferedReader(InputStreamReader(input, Charsets.UTF_8)).use { CsvCodec.read(it) }
        if (rows.isEmpty()) return@withContext ImportResult(0, listOf("File is empty"))
        val headers = rows.first()
        if (headers.isEmpty()) return@withContext ImportResult(0, listOf("Header row is empty"))

        val errors = mutableListOf<String>()
        val docs = rows.drop(1).mapIndexedNotNull { idx, row ->
            if (row.all { it.isEmpty() }) return@mapIndexedNotNull null
            try {
                val doc = Document()
                headers.forEachIndexed { col, key ->
                    val cell = row.getOrNull(col).orEmpty()
                    if (cell.isNotEmpty()) doc[key] = inferCellValue(cell)
                }
                doc
            } catch (e: Exception) {
                errors.add("Row ${idx + 2}: ${e.message}")
                null
            }
        }
        if (docs.isEmpty()) return@withContext ImportResult(0, errors)
        insertAll(database, collection, docs, errors)
    }

    // ---------- helpers ----------

    private suspend fun fetchAll(database: String, collection: String, filter: String): List<Document> {
        val col = requireClient().getDatabase(database).getCollection<Document>(collection)
        val filterDoc = if (filter.isBlank() || filter.trim() == "{}") Document() else Document.parse(filter)
        return col.find(filterDoc).toList()
    }

    private suspend fun insertAll(
        database: String,
        collection: String,
        docs: List<Document>,
        existingErrors: List<String>,
    ): ImportResult {
        val col = requireClient().getDatabase(database).getCollection<Document>(collection)
        val errors = existingErrors.toMutableList()
        // Insert one-at-a-time so a single bad doc (e.g. dup _id) doesn't abort the batch.
        var inserted = 0
        docs.forEachIndexed { idx, doc ->
            try {
                col.insertOne(doc)
                inserted++
            } catch (t: Throwable) {
                errors.add("Insert ${idx + 1}: ${t.message ?: t::class.java.simpleName}")
            }
        }
        return ImportResult(inserted = inserted, errors = errors)
    }

    private fun renderCell(value: Any?): String = when (value) {
        null -> ""
        is String -> value
        is Number, is Boolean -> value.toString()
        is Document -> value.toJson(EXPORT_JSON_FLAT)
        is List<*> -> Document("v", value).toJson(EXPORT_JSON_FLAT).removePrefix("{\"v\": ").removeSuffix("}")
        else -> value.toString()
    }

    /** Tries number then boolean then null literals, else returns the raw string. */
    private fun inferCellValue(text: String): Any {
        when (text.lowercase()) {
            "true" -> return true
            "false" -> return false
            "null" -> return ""  // Document.put with null is awkward; treat null literal as empty string
        }
        text.toLongOrNull()?.let { return it }
        text.toDoubleOrNull()?.let { return it }
        // If the cell happens to be valid JSON, parse it (so nested-object CSV round-trips work).
        if (text.startsWith("{") && text.endsWith("}")) {
            runCatching { return Document.parse(text) }
        }
        return text
    }

    private fun requireClient(): MongoClient =
        clientHolder.activeClient() ?: error("No active MongoClient")

    /**
     * Splits a top-level JSON array string into its element substrings. Brace-aware,
     * string-literal-aware, escape-aware. Avoids pulling in a streaming JSON library.
     */
    private fun splitJsonArray(text: String): List<String> {
        require(text.startsWith("[")) { "Not an array" }
        val out = mutableListOf<String>()
        var depth = 0
        var inString = false
        var escape = false
        var start = -1
        for (i in 1 until text.length) {
            val c = text[i]
            if (inString) {
                when {
                    escape -> escape = false
                    c == '\\' -> escape = true
                    c == '"' -> inString = false
                }
                continue
            }
            when (c) {
                '"' -> inString = true
                '{', '[' -> {
                    if (depth == 0) start = i
                    depth++
                }
                '}', ']' -> {
                    depth--
                    if (depth == 0 && start != -1) {
                        out.add(text.substring(start, i + 1))
                        start = -1
                    } else if (depth == -1 && c == ']') {
                        return out
                    }
                }
            }
        }
        return out
    }

    private companion object {
        val EXPORT_JSON: JsonWriterSettings = JsonWriterSettings.builder()
            .outputMode(JsonMode.RELAXED)
            .indent(true)
            .indentCharacters("  ")
            .build()
        val EXPORT_JSON_FLAT: JsonWriterSettings = JsonWriterSettings.builder()
            .outputMode(JsonMode.RELAXED)
            .indent(false)
            .build()
    }
}
