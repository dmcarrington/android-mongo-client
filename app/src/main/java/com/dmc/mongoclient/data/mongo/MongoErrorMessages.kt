package com.dmc.mongoclient.data.mongo

import com.mongodb.MongoCommandException
import com.mongodb.MongoConfigurationException
import com.mongodb.MongoSecurityException
import com.mongodb.MongoServerException
import com.mongodb.MongoSocketException
import com.mongodb.MongoTimeoutException

/** Whether the error indicates the connection to the cluster is gone or unreachable. */
fun Throwable.isConnectionLost(): Boolean = when (this) {
    is MongoSocketException, is MongoTimeoutException -> true
    else -> false
}

/** Map a Mongo / driver exception to a one-line user-facing message. */
fun Throwable.toUserMessage(): String = when (this) {
    is MongoSecurityException ->
        "Authentication failed — check the username and password."
    is MongoTimeoutException ->
        "Timed out waiting for the server. The connection may be down."
    is MongoSocketException ->
        "Lost the connection to the cluster."
    is MongoConfigurationException ->
        "Connection misconfigured: ${message ?: "see logs"}"
    is MongoCommandException -> when (errorCode) {
        11000, 11001 -> "Duplicate key — a document with that _id (or unique-indexed value) already exists."
        13 -> "Not authorised to perform that action on this resource."
        else -> errorMessage.ifBlank { "Server error: $errorCode" }
    }
    is MongoServerException ->
        message ?: "Server error from MongoDB."
    is IllegalArgumentException ->
        // JSON parse errors from JsonFormat already arrive here with friendly text.
        message ?: "Invalid input."
    else -> message ?: this::class.java.simpleName
}
