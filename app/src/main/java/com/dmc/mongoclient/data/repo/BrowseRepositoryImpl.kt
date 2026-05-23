package com.dmc.mongoclient.data.repo

import com.dmc.mongoclient.data.mongo.IndexParser
import com.dmc.mongoclient.data.mongo.MongoClientHolder
import com.dmc.mongoclient.domain.model.CollectionSummary
import com.dmc.mongoclient.domain.model.CollectionType
import com.dmc.mongoclient.domain.model.DatabaseSummary
import com.dmc.mongoclient.domain.model.IndexInfo
import com.dmc.mongoclient.domain.repo.BrowseRepository
import com.mongodb.kotlin.client.coroutine.MongoClient
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.withContext
import org.bson.Document

@Singleton
class BrowseRepositoryImpl @Inject constructor(
    private val clientHolder: MongoClientHolder,
) : BrowseRepository {

    override suspend fun listDatabases(includeSystem: Boolean): List<DatabaseSummary> =
        withContext(Dispatchers.IO) {
            val client = requireClient()
            val rows = client.listDatabases().toList()
            rows.mapNotNull { doc ->
                val name = doc.getString("name") ?: return@mapNotNull null
                if (!includeSystem && name in SYSTEM_DBS) return@mapNotNull null
                DatabaseSummary(
                    name = name,
                    sizeOnDisk = doc.getLongOrNull("sizeOnDisk"),
                    empty = doc.getBoolean("empty", false),
                )
            }
        }

    override suspend fun listCollections(database: String): List<CollectionSummary> =
        withContext(Dispatchers.IO) {
            val db = requireClient().getDatabase(database)
            val infos = db.listCollections().toList()
            infos.map { doc ->
                val name = doc.getString("name") ?: "(unnamed)"
                val type = when (doc.getString("type")?.lowercase()) {
                    "collection" -> CollectionType.COLLECTION
                    "view" -> CollectionType.VIEW
                    "timeseries" -> CollectionType.TIMESERIES
                    null -> CollectionType.COLLECTION
                    else -> CollectionType.OTHER
                }
                val estimated = runCatching {
                    if (type == CollectionType.COLLECTION) {
                        db.getCollection<Document>(name).estimatedDocumentCount()
                    } else null
                }.getOrNull()
                CollectionSummary(name = name, type = type, estimatedDocumentCount = estimated)
            }.sortedBy { it.name }
        }

    override suspend fun listIndexes(database: String, collection: String): List<IndexInfo> =
        withContext(Dispatchers.IO) {
            val col = requireClient().getDatabase(database).getCollection<Document>(collection)
            col.listIndexes().toList().map(IndexParser::parse)
        }

    override suspend fun dropDatabase(database: String) = withContext(Dispatchers.IO) {
        requireClient().getDatabase(database).drop()
    }

    override suspend fun dropCollection(database: String, collection: String) =
        withContext(Dispatchers.IO) {
            requireClient().getDatabase(database).getCollection<Document>(collection).drop()
        }

    private fun requireClient(): MongoClient =
        clientHolder.activeClient() ?: error("No active MongoClient")

    private companion object {
        val SYSTEM_DBS = setOf("admin", "local", "config")
    }
}

private fun Document.getLongOrNull(key: String): Long? = when (val v = get(key)) {
    is Number -> v.toLong()
    else -> null
}
