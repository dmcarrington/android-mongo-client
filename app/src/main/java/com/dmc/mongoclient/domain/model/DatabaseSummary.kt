package com.dmc.mongoclient.domain.model

data class DatabaseSummary(
    val name: String,
    val sizeOnDisk: Long?,
    val empty: Boolean,
)

data class CollectionSummary(
    val name: String,
    val type: CollectionType,
    val estimatedDocumentCount: Long?,
)

enum class CollectionType { COLLECTION, VIEW, TIMESERIES, OTHER }
