package com.dmc.mongoclient.data.crypto

data class EncryptedPayload(
    val ciphertext: ByteArray,
    val iv: ByteArray,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is EncryptedPayload) return false
        return ciphertext.contentEquals(other.ciphertext) && iv.contentEquals(other.iv)
    }

    override fun hashCode(): Int = 31 * ciphertext.contentHashCode() + iv.contentHashCode()
}
