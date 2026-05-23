package com.dmc.mongoclient.domain.model

/**
 * A key in an index definition. `value` is normally `1` (ascending),
 * `-1` (descending), or a string like `"2dsphere"`, `"text"`, `"hashed"`.
 */
data class IndexKey(val field: String, val value: Any)

data class IndexInfo(
    val name: String,
    val keys: List<IndexKey>,
    val unique: Boolean,
    val sparse: Boolean,
    val ttlSeconds: Long?,
    val partialFilterExpression: String?,
    val raw: String,
)
