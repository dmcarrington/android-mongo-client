package com.dmc.mongoclient.domain.repo

import com.dmc.mongoclient.domain.model.DocumentRef
import com.dmc.mongoclient.domain.model.FindPage
import com.dmc.mongoclient.domain.model.FindRequest

interface DocumentRepository {

    suspend fun find(database: String, collection: String, request: FindRequest): FindPage

    /** Returns the new document's _id as a JSON-friendly string for display. */
    suspend fun insertOne(database: String, collection: String, json: String): String

    suspend fun replaceOne(database: String, collection: String, ref: DocumentRef, newJson: String)

    suspend fun deleteOne(database: String, collection: String, ref: DocumentRef)

    suspend fun estimatedCount(database: String, collection: String): Long

    /** Exact count for a filter. Can be slow on large collections without a supporting index. */
    suspend fun countDocuments(database: String, collection: String, filter: String): Long
}
