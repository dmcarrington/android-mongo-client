package com.dmc.mongoclient.domain.repo

import com.dmc.mongoclient.domain.model.ExportResult
import com.dmc.mongoclient.domain.model.ImportResult
import java.io.InputStream
import java.io.OutputStream

/**
 * Stream-based import/export so the UI can route file URIs through SAF
 * (ContentResolver.openInputStream / openOutputStream) without the repo
 * needing to know about Android Uris.
 */
interface ImportExportRepository {

    /** Exports documents matching `filter` as a JSON array of RELAXED-mode docs. */
    suspend fun exportJson(
        database: String,
        collection: String,
        filter: String,
        output: OutputStream,
    ): ExportResult

    /** Exports as CSV. Column union = all distinct top-level keys across the result set. */
    suspend fun exportCsv(
        database: String,
        collection: String,
        filter: String,
        output: OutputStream,
    ): ExportResult

    /** Imports JSON. Auto-detects between a top-level `[...]` array and NDJSON (one doc per line). */
    suspend fun importJson(
        database: String,
        collection: String,
        input: InputStream,
    ): ImportResult

    /** Imports CSV with the first row as headers; basic type inference for cells. */
    suspend fun importCsv(
        database: String,
        collection: String,
        input: InputStream,
    ): ImportResult
}
