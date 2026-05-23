package com.dmc.mongoclient.domain.repo

import com.dmc.mongoclient.domain.model.CollectionSummary
import com.dmc.mongoclient.domain.model.DatabaseSummary
import com.dmc.mongoclient.domain.model.IndexInfo

interface BrowseRepository {

    /** @param includeSystem when false, strips admin/local/config. */
    suspend fun listDatabases(includeSystem: Boolean): List<DatabaseSummary>

    suspend fun listCollections(database: String): List<CollectionSummary>

    suspend fun listIndexes(database: String, collection: String): List<IndexInfo>

    suspend fun dropDatabase(database: String)

    suspend fun dropCollection(database: String, collection: String)
}
