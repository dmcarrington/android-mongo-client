package com.dmc.mongoclient.data.mongo

import com.mongodb.ConnectionString
import com.mongodb.MongoClientSettings
import com.mongodb.connection.TransportSettings
import javax.inject.Inject
import javax.inject.Singleton
import javax.net.ssl.SSLContext
import org.conscrypt.Conscrypt

/**
 * Builds the `MongoClientSettings` used everywhere in the app. Centralises the
 * Phase 0 Android workarounds:
 *  - Conscrypt SSLContext (consistent TLS on Android emulators + devices)
 *  - Netty transport (sidesteps the driver's vendored TlsChannel which has a
 *    NIO buffer-state bug on Android — see project memory
 *    `project-android-mongo-driver-stack`)
 */
@Singleton
class MongoSettingsBuilder @Inject constructor() {

    fun build(uri: String): MongoClientSettings {
        val sslContext = SSLContext.getInstance("TLS", Conscrypt.newProvider())
            .apply { init(null, null, null) }
        return MongoClientSettings.builder()
            .applyConnectionString(ConnectionString(uri))
            .applyToSslSettings { it.context(sslContext) }
            .transportSettings(TransportSettings.nettyBuilder().build())
            .build()
    }
}
