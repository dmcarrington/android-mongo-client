package com.dmc.mongoclient.data.repo

import com.dmc.mongoclient.data.mongo.JsonFormat
import com.dmc.mongoclient.data.mongo.MongoClientHolder
import com.dmc.mongoclient.domain.model.DocumentRef
import com.dmc.mongoclient.domain.model.FindPage
import com.dmc.mongoclient.domain.model.FindRequest
import com.dmc.mongoclient.domain.repo.DocumentRepository
import com.mongodb.client.model.Filters
import com.mongodb.kotlin.client.coroutine.MongoClient
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.withContext
import org.bson.BsonDocument
import org.bson.BsonValue
import org.bson.Document

@Singleton
class DocumentRepositoryImpl @Inject constructor(
    private val clientHolder: MongoClientHolder,
) : DocumentRepository {

    override suspend fun find(
        database: String,
        collection: String,
        request: FindRequest,
    ): FindPage = withContext(Dispatchers.IO) {
        val col = requireClient().getDatabase(database).getCollection<Document>(collection)
        val filterDoc = parseOrEmpty(request.filter)
        var publisher = col.find(filterDoc).skip(request.skip).limit(request.limit)
        request.projection?.takeIf { it.isNotBlank() }
            ?.let { publisher = publisher.projection(parseOrEmpty(it)) }
        request.sort?.takeIf { it.isNotBlank() }
            ?.let { publisher = publisher.sort(parseOrEmpty(it)) }
        val docs = publisher.toList()
        FindPage(
            documents = docs.map { doc ->
                val id = doc.toBsonDocument()["_id"]
                    ?: throw IllegalStateException("Document is missing _id")
                DocumentRef(id = id, prettyJson = JsonFormat.render(doc))
            },
            skip = request.skip,
            limit = request.limit,
            mightHaveMore = docs.size == request.limit,
        )
    }

    override suspend fun insertOne(
        database: String,
        collection: String,
        json: String,
    ): String = withContext(Dispatchers.IO) {
        val doc = JsonFormat.parseObject(json)
        requireClient().getDatabase(database).getCollection<Document>(collection).insertOne(doc)
        val inserted = doc.toBsonDocument()["_id"]
            ?: throw IllegalStateException("Insert succeeded but no _id was assigned")
        inserted.toExtendedJsonValue()
    }

    override suspend fun replaceOne(
        database: String,
        collection: String,
        ref: DocumentRef,
        newJson: String,
    ) = withContext(Dispatchers.IO) {
        // We don't force `_id` back onto the replacement; the driver/server
        // uses the filter's _id when the replacement omits it, and raises a
        // WriteError if the replacement has a *different* _id (which is the
        // right behaviour — we'd rather surface the conflict than silently
        // overwrite).
        val parsed = JsonFormat.parseObject(newJson)
        requireClient().getDatabase(database).getCollection<Document>(collection)
            .replaceOne(Filters.eq("_id", ref.id), parsed)
        Unit
    }

    override suspend fun deleteOne(
        database: String,
        collection: String,
        ref: DocumentRef,
    ) = withContext(Dispatchers.IO) {
        requireClient().getDatabase(database).getCollection<Document>(collection)
            .deleteOne(Filters.eq("_id", ref.id))
        Unit
    }

    override suspend fun estimatedCount(database: String, collection: String): Long =
        withContext(Dispatchers.IO) {
            requireClient().getDatabase(database).getCollection<Document>(collection)
                .estimatedDocumentCount()
        }

    override suspend fun countDocuments(
        database: String,
        collection: String,
        filter: String,
    ): Long = withContext(Dispatchers.IO) {
        val col = requireClient().getDatabase(database).getCollection<Document>(collection)
        col.countDocuments(parseOrEmpty(filter))
    }

    private fun requireClient(): MongoClient =
        clientHolder.activeClient() ?: error("No active MongoClient")

    private fun parseOrEmpty(text: String): Document =
        if (text.isBlank()) Document() else JsonFormat.parseObject(text)
}

/**
 * Serialise a single BsonValue as Extended-JSON. The BSON library only exposes
 * `toJson()` on `BsonDocument`, so we wrap, render, and strip the wrapper.
 */
private fun BsonValue.toExtendedJsonValue(): String {
    val wrapper = BsonDocument("v", this)
    val json = wrapper.toJson()
    val colon = json.indexOf(':')
    return json.substring(colon + 1, json.length - 1).trim()
}
