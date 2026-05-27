import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

// Single source of truth for versionCode/versionName lives in
// `version.properties` at the repo root. Bump it before each Play upload.
val versionProps = Properties().apply {
    rootProject.file("version.properties").inputStream().use { load(it) }
}

android {
    namespace = "com.dmc.mongoclient"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.dmc.mongoclient"
        minSdk = 26
        targetSdk = 36
        versionCode = versionProps.getProperty("versionCode").toInt()
        versionName = versionProps.getProperty("versionName")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            val keystorePath = signingProperty("MONGO_CLIENT_KEYSTORE_PATH")
            if (!keystorePath.isNullOrBlank()) {
                storeFile = file(keystorePath)
                storePassword = signingProperty("MONGO_CLIENT_KEYSTORE_PASSWORD")
                keyAlias = signingProperty("MONGO_CLIENT_KEY_ALIAS")
                keyPassword = signingProperty("MONGO_CLIENT_KEY_PASSWORD")
                enableV1Signing = true
                enableV2Signing = true
                enableV3Signing = true
            }
        }
    }

    buildTypes {
        getByName("debug") {
            isMinifyEnabled = false
        }
        getByName("release") {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            // Falls back to unsigned (which fails install) rather than silently
            // debug-signing — surfaces missing-keystore mistakes loudly.
            signingConfig = signingConfigs.getByName("release")
                .takeIf { it.storeFile != null }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }

    packaging {
        resources {
            excludes += setOf(
                "META-INF/native-image/**",
                "META-INF/proguard/**",
                "META-INF/INDEX.LIST",
                "META-INF/io.netty.versions.properties",
                "META-INF/LICENSE.md",
                "META-INF/LICENSE-notice.md",
                "META-INF/DEPENDENCIES",
                "META-INF/NOTICE",
                "META-INF/NOTICE.txt",
                "META-INF/LICENSE",
                "META-INF/LICENSE.txt",
            )
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material3.adaptive)
    implementation(libs.androidx.compose.ui.tooling.preview)
    debugImplementation(libs.androidx.compose.ui.tooling)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.navigation.compose)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.biometric)

    implementation(libs.kotlinx.coroutines.android)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)

    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.runner)

    implementation(libs.mongodb.driver.kotlin.coroutine) {
        // JVM-only native libs cause R8/packaging friction on Android.
        exclude(group = "com.github.luben", module = "zstd-jni")
        exclude(group = "org.xerial.snappy", module = "snappy-java")
    }

    // Android-friendly DNS lookups (SRV/TXT) for mongodb+srv:// — replaces the
    // driver's default JndiDnsClient, which depends on javax.naming.* that
    // Android doesn't ship.
    implementation(libs.dnsjava)

    // Driver diagnostics. Without an SLF4J binding the driver routes to NoOp
    // and connection-thread failures vanish silently. Routes to System.err
    // which Android forwards to logcat under tag "System.err".
    implementation(libs.slf4j.simple)

    // Standalone Conscrypt for SSLEngine. The bug turned out to be in the
    // driver's vendored TlsChannel (NIO buffer state), not the SSLEngine —
    // keeping Conscrypt as belt-and-suspenders for the Netty path.
    implementation(libs.conscrypt.android)

    // Netty transport for the MongoDB driver. Replaces the driver's internal
    // async transport which uses TlsChannel — that wrapper has a buffer-flip
    // issue on Android that left every SSLEngine stuck in BUFFER_UNDERFLOW.
    // Netty's own NIO+SSL plumbing avoids the broken path entirely.
    implementation(libs.netty.handler)
}

/** Reads a signing credential from -P / gradle.properties first, env var as fallback. */
fun signingProperty(name: String): String? =
    (project.findProperty(name) as? String)?.takeIf { it.isNotBlank() }
        ?: System.getenv(name)?.takeIf { it.isNotBlank() }
