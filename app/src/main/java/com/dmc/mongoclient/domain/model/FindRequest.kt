package com.dmc.mongoclient.domain.model

import org.bson.BsonValue

data class FindRequest(
    val filter: String,
    val projection: String?,
    val sort: String?,
    val skip: Int,
    val limit: Int,
)

/**
 * One document as displayed in the UI. The `_id` is held as a BsonValue so we
 * can round-trip it to subsequent driver calls (replaceById, deleteById)
 * without serialising through JSON and risking type drift.
 */
data class DocumentRef(
    val id: BsonValue,
    val prettyJson: String,
)

data class FindPage(
    val documents: List<DocumentRef>,
    val skip: Int,
    val limit: Int,
    /** True if the underlying cursor returned exactly `limit` rows — suggests there might be more. */
    val mightHaveMore: Boolean,
)
