package com.dmc.mongoclient.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ConnectionDao {

    @Query("SELECT * FROM saved_connections ORDER BY COALESCE(lastUsedAt, createdAt) DESC")
    fun observeAll(): Flow<List<SavedConnectionEntity>>

    @Query("SELECT * FROM saved_connections WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): SavedConnectionEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(entity: SavedConnectionEntity): Long

    @Update
    suspend fun update(entity: SavedConnectionEntity)

    @Delete
    suspend fun delete(entity: SavedConnectionEntity)

    @Query("UPDATE saved_connections SET lastUsedAt = :timestamp WHERE id = :id")
    suspend fun markUsed(id: Long, timestamp: Long)
}
