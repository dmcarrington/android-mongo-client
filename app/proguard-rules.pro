# R8 / ProGuard rules. Only the release build runs these — debug skips R8.
# Widen `-keep` reactively if a release-mode crash surfaces a stripped class.
# See project memory `project-android-mongo-driver-stack` for context.

# ----- MongoDB driver + BSON -----
# BSON codecs are looked up reflectively at runtime.
-keep class org.bson.** { *; }
-keep interface org.bson.** { *; }
-keepclassmembers class * implements org.bson.codecs.Codec {
    public <init>(...);
}

# Driver internals use reflection on configuration classes and connection
# implementations. Don't strip these — and don't warn about platform stuff
# that's expected to be missing on Android.
-keep class com.mongodb.** { *; }
-keep interface com.mongodb.** { *; }
-dontwarn com.mongodb.internal.**
-dontwarn com.mongodb.kerberos.**

# ----- SPI: our custom DnsClient must be discoverable by ServiceLoader -----
-keep class com.dmc.mongoclient.data.mongo.AndroidDnsClient { *; }
-keep class com.dmc.mongoclient.data.mongo.AndroidDnsClientProvider { *; }

# ----- javax.security.sasl stubs (Android omits the package) -----
# The driver's SaslAuthenticator$SaslClientImpl implements these interfaces;
# R8 must not strip them or the auth class fails to link.
-keep class javax.security.sasl.** { *; }
-keep interface javax.security.sasl.** { *; }

# ----- Netty -----
-keep class io.netty.** { *; }
-keep interface io.netty.** { *; }
-dontwarn io.netty.**
# Netty looks up some option classes by name.
-keepclassmembers class io.netty.channel.ChannelOption {
    public static ** *;
}

# ----- Conscrypt -----
-keep class org.conscrypt.** { *; }
-dontwarn org.conscrypt.**

# ----- dnsjava -----
-keep class org.xbill.DNS.** { *; }
-dontwarn org.xbill.DNS.**

# ----- SLF4J -----
-dontwarn org.slf4j.**

# ----- Hilt / Dagger -----
# Hilt ships its own consumer-proguard rules but be explicit about generated
# components and our entry points to keep upgrades robust.
-keep class dagger.hilt.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager { *; }
-keep @dagger.hilt.android.AndroidEntryPoint class *
-keep @dagger.hilt.android.HiltAndroidApp class *
-keep @dagger.hilt.android.lifecycle.HiltViewModel class * { <init>(...); }

# ----- Room -----
# Room ships consumer rules but generated DAO impls are referenced reflectively
# via the Application database accessor — keep them.
-keep class * extends androidx.room.RoomDatabase {
    <init>();
}
-keep class * extends androidx.room.RoomDatabase$Builder { *; }

# ----- AndroidX DataStore -----
-keep class androidx.datastore.preferences.protobuf.** { *; }
-dontwarn androidx.datastore.preferences.protobuf.**

# ----- JDK classes the driver / Netty reach for but Android may not ship -----
-dontwarn java.lang.management.**
-dontwarn javax.naming.**
-dontwarn javax.management.**
-dontwarn javax.security.auth.kerberos.**
-dontwarn javax.security.auth.login.**
-dontwarn sun.misc.**
-dontwarn sun.nio.ch.**
-dontwarn org.jctools.**
-dontwarn reactor.blockhound.**

# ----- Optional dependencies the driver / reactor reference but we don't ship -----
# Micrometer metrics (would only matter if a MeterRegistry was configured).
-dontwarn io.micrometer.**
# kotlinx-serialization codec provider (driver loads via reflection; we use raw Documents).
-dontwarn org.bson.codecs.kotlinx.**
# Client-side field-level encryption (mongo-crypt) — not used.
-dontwarn com.mongodb.crypt.capi.**

# ----- Kotlin metadata required for reflection-based libraries -----
-keepattributes Signature, InnerClasses, EnclosingMethod, *Annotation*
-keepattributes SourceFile, LineNumberTable
