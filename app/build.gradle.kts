plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.dmc.mongoclient"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.dmc.mongoclient"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0-spike"
    }

    buildTypes {
        getByName("debug") {
            isMinifyEnabled = false
        }
        getByName("release") {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
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
    implementation(libs.androidx.compose.ui.tooling.preview)
    debugImplementation(libs.androidx.compose.ui.tooling)

    implementation(libs.kotlinx.coroutines.android)

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
