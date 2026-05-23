package com.dmc.mongoclient.domain.repo

import com.dmc.mongoclient.domain.model.ConnectionTestResult
import com.dmc.mongoclient.domain.model.SavedConnection
import kotlinx.coroutines.flow.Flow

interface ConnectionRepository {

    fun observeAll(): Flow<List<SavedConnection>>

    suspend fun getById(id: Long): SavedConnection?

    /** Returns the new row's id. */
    suspend fun save(displayName: String, uri: String): Long

    suspend fun update(id: Long, displayName: String, uri: String)

    suspend fun delete(id: Long)

    /** Probes a URI without persisting anything. */
    suspend fun test(uri: String): ConnectionTestResult

    /** Opens the saved connection as the app's active client. */
    suspend fun openActive(id: Long)

    suspend fun closeActive()
}
