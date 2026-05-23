package com.dmc.mongoclient.data.crypto

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CredentialVaultTest {

    @Test
    fun encrypt_decrypt_round_trips() {
        val vault = CredentialVault()
        val plaintext = "mongodb+srv://user:p%40ssword@cluster.example.net/?retryWrites=true&w=majority"
        val payload = vault.encrypt(plaintext)
        assertNotEquals(plaintext.toByteArray().toList(), payload.ciphertext.toList())
        assertEquals(12, payload.iv.size) // GCM default IV size
        assertEquals(plaintext, vault.decrypt(payload))
    }

    @Test
    fun two_encrypts_of_same_plaintext_differ() {
        val vault = CredentialVault()
        val plaintext = "same input"
        val a = vault.encrypt(plaintext)
        val b = vault.encrypt(plaintext)
        // GCM mandates a unique IV per encryption — and ciphertext will differ
        // because the IV is mixed into the keystream.
        assertNotEquals(a.iv.toList(), b.iv.toList())
        assertNotEquals(a.ciphertext.toList(), b.ciphertext.toList())
        assertEquals(plaintext, vault.decrypt(a))
        assertEquals(plaintext, vault.decrypt(b))
    }
}
