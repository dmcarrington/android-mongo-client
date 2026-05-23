package com.dmc.mongoclient.data.mongo

import com.dmc.mongoclient.domain.model.IndexInfo
import com.dmc.mongoclient.domain.model.IndexKey
import org.bson.Document

/**
 * Maps a single document from `collection.listIndexes()` into our domain
 * shape. The driver returns plain `Document` rows — we extract the bits the
 * UI cares about and keep the full RELAXED JSON for the "show raw" toggle.
 */
object IndexParser {

    fun parse(raw: Document): IndexInfo {
        val name = raw.getString("name") ?: "(unnamed)"
        val keysDoc = raw.get("key", Document::class.java) ?: Document()
        val keys = keysDoc.entries.map { (field, value) -> IndexKey(field, value ?: 1) }
        val ttl = (raw["expireAfterSeconds"] as? Number)?.toLong()
        val partial = raw.get("partialFilterExpression", Document::class.java)?.toJson(JsonFormat.pretty)
        return IndexInfo(
            name = name,
            keys = keys,
            unique = raw.getBoolean("unique", false),
            sparse = raw.getBoolean("sparse", false),
            ttlSeconds = ttl,
            partialFilterExpression = partial,
            raw = raw.toJson(JsonFormat.pretty),
        )
    }
}
