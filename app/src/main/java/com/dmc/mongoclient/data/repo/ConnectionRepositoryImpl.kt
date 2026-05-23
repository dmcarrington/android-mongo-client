package com.dmc.mongoclient.data.repo

import com.dmc.mongoclient.data.crypto.CredentialVault
import com.dmc.mongoclient.data.crypto.EncryptedPayload
import com.dmc.mongoclient.data.db.ConnectionDao
import com.dmc.mongoclient.data.db.SavedConnectionEntity
import com.dmc.mongoclient.data.mongo.ConnectionStringParser
import com.dmc.mongoclient.data.mongo.MongoClientHolder
import com.dmc.mongoclient.data.mongo.MongoSettingsBuilder
import com.dmc.mongoclient.data.mongo.toUserMessage
import com.dmc.mongoclient.domain.model.ConnectionTestResult
import com.dmc.mongoclient.domain.model.SavedConnection
import com.dmc.mongoclient.domain.repo.ConnectionRepository
import com.mongodb.kotlin.client.coroutine.MongoClient
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.count
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

@Singleton
class ConnectionRepositoryImpl @Inject constructor(
    private val dao: ConnectionDao,
    private val vault: CredentialVault,
    private val parser: ConnectionStringParser,
    private val settingsBuilder: MongoSettingsBuilder,
    private val clientHolder: MongoClientHolder,
) : ConnectionRepository {

    override fun observeAll(): Flow<List<SavedConnection>> =
        dao.observeAll().map { rows -> rows.map(::toDomain) }

    override suspend fun getById(id: Long): SavedConnection? =
        dao.getById(id)?.let(::toDomain)

    override suspend fun save(displayName: String, uri: String): Long {
        val payload = vault.encrypt(uri)
        val entity = SavedConnectionEntity(
            displayName = displayName,
            encryptedUri = payload.ciphertext,
            iv = payload.iv,
            createdAt = System.currentTimeMillis(),
            lastUsedAt = null,
        )
        return dao.insert(entity)
    }

    override suspend fun update(id: Long, displayName: String, uri: String) {
        val existing = dao.getById(id) ?: error("Connection $id not found")
        val payload = vault.encrypt(uri)
        dao.update(
            existing.copy(
                displayName = displayName,
                encryptedUri = payload.ciphertext,
                iv = payload.iv,
            ),
        )
    }

    override suspend fun delete(id: Long) {
        val existing = dao.getById(id) ?: return
        dao.delete(existing)
    }

    override suspend fun test(uri: String): ConnectionTestResult = withContext(Dispatchers.IO) {
        val started = System.currentTimeMillis()
        var client: MongoClient? = null
        try {
            client = MongoClient.create(settingsBuilder.build(uri))
            val count = client.listDatabaseNames().count()
            ConnectionTestResult.Ok(
                databaseCount = count,
                durationMs = System.currentTimeMillis() - started,
            )
        } catch (t: Throwable) {
            ConnectionTestResult.Error(t.toUserMessage())
        } finally {
            runCatching { client?.close() }
        }
    }

    override suspend fun openActive(id: Long) {
        val entity = dao.getById(id) ?: error("Connection $id not found")
        val uri = vault.decrypt(EncryptedPayload(entity.encryptedUri, entity.iv))
        clientHolder.open(id, uri)
        dao.markUsed(id, System.currentTimeMillis())
    }

    override suspend fun closeActive() {
        clientHolder.close()
    }

    private fun toDomain(entity: SavedConnectionEntity): SavedConnection {
        val uri = vault.decrypt(EncryptedPayload(entity.encryptedUri, entity.iv))
        return SavedConnection(
            id = entity.id,
            displayName = entity.displayName,
            uri = uri,
            redactedUri = parser.redactPassword(uri),
            createdAt = entity.createdAt,
            lastUsedAt = entity.lastUsedAt,
        )
    }
}
