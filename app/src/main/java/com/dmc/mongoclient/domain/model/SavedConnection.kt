package com.dmc.mongoclient.domain.model

/**
 * Domain view of a saved connection — decrypted URI in memory only.
 * The persistence layer (`SavedConnectionEntity`) stores it encrypted.
 */
data class SavedConnection(
    val id: Long,
    val displayName: String,
    val uri: String,
    val redactedUri: String,
    val createdAt: Long,
    val lastUsedAt: Long?,
)
