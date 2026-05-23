package com.dmc.mongoclient.data.mongo

import com.mongodb.kotlin.client.coroutine.MongoClient
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Holds at most one active `MongoClient` for the app's lifetime. The lean-MVP
 * model is "one connection at a time" — opening a new one closes any prior
 * client. Re-entrancy is guarded by a mutex so concurrent UI actions can't
 * leave a half-closed client behind.
 *
 * `open()` runs on `Dispatchers.IO` because `MongoSettingsBuilder.build`
 * constructs a `com.mongodb.ConnectionString`, which for `mongodb+srv://` URIs
 * performs synchronous SRV+TXT DNS lookups. See project memory
 * `project-android-mongo-driver-stack`.
 */
@Singleton
class MongoClientHolder @Inject constructor(
    private val settingsBuilder: MongoSettingsBuilder,
) {
    private val lock = Mutex()
    private var current: MongoClient? = null
    private var currentConnectionId: Long? = null

    suspend fun open(connectionId: Long, uri: String): MongoClient = lock.withLock {
        current?.close()
        val client = withContext(Dispatchers.IO) {
            MongoClient.create(settingsBuilder.build(uri))
        }
        current = client
        currentConnectionId = connectionId
        client
    }

    fun activeClient(): MongoClient? = current

    fun activeConnectionId(): Long? = currentConnectionId

    suspend fun close() = lock.withLock {
        current?.close()
        current = null
        currentConnectionId = null
    }
}
