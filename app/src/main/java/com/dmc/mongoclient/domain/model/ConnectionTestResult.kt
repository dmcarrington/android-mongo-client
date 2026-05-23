package com.dmc.mongoclient.domain.model

sealed interface ConnectionTestResult {
    data class Ok(val databaseCount: Int, val durationMs: Long) : ConnectionTestResult
    data class Error(val message: String) : ConnectionTestResult
}
