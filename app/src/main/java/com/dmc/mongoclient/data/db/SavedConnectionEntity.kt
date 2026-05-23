package com.dmc.mongoclient.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "saved_connections")
data class SavedConnectionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val displayName: String,
    val encryptedUri: ByteArray,
    val iv: ByteArray,
    val createdAt: Long,
    val lastUsedAt: Long?,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SavedConnectionEntity) return false
        return id == other.id &&
            displayName == other.displayName &&
            encryptedUri.contentEquals(other.encryptedUri) &&
            iv.contentEquals(other.iv) &&
            createdAt == other.createdAt &&
            lastUsedAt == other.lastUsedAt
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + displayName.hashCode()
        result = 31 * result + encryptedUri.contentHashCode()
        result = 31 * result + iv.contentHashCode()
        result = 31 * result + createdAt.hashCode()
        result = 31 * result + (lastUsedAt?.hashCode() ?: 0)
        return result
    }
}
