package com.dmc.mongoclient.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [SavedConnectionEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun connectionDao(): ConnectionDao
}
