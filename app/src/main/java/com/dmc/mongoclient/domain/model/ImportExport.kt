package com.dmc.mongoclient.domain.model

enum class ExportFormat { JSON, CSV }

enum class ImportFormat { JSON, CSV }

data class ImportResult(
    val inserted: Int,
    val errors: List<String>,
)

data class ExportResult(
    val exported: Int,
)
