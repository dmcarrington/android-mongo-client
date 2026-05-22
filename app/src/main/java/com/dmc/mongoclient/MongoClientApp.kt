package com.dmc.mongoclient

import android.app.Application
import org.xbill.DNS.ResolverConfig
import org.xbill.DNS.config.AndroidResolverConfigProvider

class MongoClientApp : Application() {
    override fun onCreate() {
        super.onCreate()

        // Driver loggers use short names ("cluster", "connection", "tls"), not
        // "com.mongodb.*" — per-package overrides on the latter match nothing.
        // Set defaults globally and bump individual loggers when debugging.
        System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "info")
        System.setProperty("org.slf4j.simpleLogger.showThreadName", "true")
        System.setProperty("org.slf4j.simpleLogger.showShortLogName", "true")
        System.setProperty("org.slf4j.simpleLogger.showDateTime", "true")
        System.setProperty("org.slf4j.simpleLogger.dateTimeFormat", "HH:mm:ss.SSS")

        // dnsjava reads the device's resolvers via ConnectivityManager.
        // Must be initialised before any SRV/TXT lookup happens.
        AndroidResolverConfigProvider.setContext(this)
        ResolverConfig.refresh()
    }
}
