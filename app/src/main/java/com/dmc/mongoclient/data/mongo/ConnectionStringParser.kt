package com.dmc.mongoclient.data.mongo

import javax.inject.Inject
import javax.inject.Singleton

sealed interface ParseResult {
    data class Ok(
        val redactedForDisplay: String,
        val host: String,
        val isSrv: Boolean,
    ) : ParseResult

    data class Error(val message: String) : ParseResult
}

/**
 * Light structural parser for MongoDB connection URIs. Deliberately avoids
 * constructing `com.mongodb.ConnectionString` because that triggers
 * synchronous SRV+TXT DNS lookups, which we cannot do on the main thread
 * (Compose's onValueChange runs there). Full driver-level validation happens
 * later in `repo.test()` / `repo.openActive()` on Dispatchers.IO.
 */
@Singleton
class ConnectionStringParser @Inject constructor() {

    fun parse(uri: String): ParseResult {
        val trimmed = uri.trim()
        val isSrv = trimmed.startsWith("mongodb+srv://", ignoreCase = true)
        val isStd = trimmed.startsWith("mongodb://", ignoreCase = true)
        if (!isSrv && !isStd) {
            return ParseResult.Error("URI must start with mongodb:// or mongodb+srv://")
        }

        val match = URI_SHAPE.find(trimmed)
            ?: return ParseResult.Error("Invalid connection string")
        val host = match.groupValues[2].takeIf { it.isNotBlank() }
            ?: return ParseResult.Error("No host in connection string")

        return ParseResult.Ok(
            redactedForDisplay = redactPassword(trimmed),
            host = host,
            isSrv = isSrv,
        )
    }

    /**
     * Replace the password segment of a `mongodb[+srv]://user:password@host/...`
     * URI with `****`. The match is intentionally narrow — only inside the
     * authority's userinfo, which is the only spot a credential lives in a
     * MongoDB URI.
     */
    fun redactPassword(uri: String): String =
        PASSWORD_REGEX.replace(uri) { m -> "${m.groupValues[1]}${m.groupValues[2]}:****@" }

    private companion object {
        // Captures: (1) optional userinfo, (2) first host[:port]
        val URI_SHAPE = Regex(
            """^mongodb(?:\+srv)?://((?:[^@/?#\s]+)@)?([^/?#,\s]+)""",
            RegexOption.IGNORE_CASE,
        )
        val PASSWORD_REGEX = Regex(
            """^(mongodb(?:\+srv)?://)([^:/?#@\s]+):([^@\s]+)@""",
            RegexOption.IGNORE_CASE,
        )
    }
}
