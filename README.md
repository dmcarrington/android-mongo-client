# Sextant

A native Android client for MongoDB — your handheld instrument for navigating clusters. Connects directly to clusters (Atlas via
`mongodb+srv://`, self-hosted via `mongodb://`, replica sets, sharded) and
provides browsing + CRUD against documents.

This is a side-loaded personal tool. There is no Play Store release.

## Requirements

- **Android 8.0+** (API 26+) for installation
- **JDK 17** and **Android SDK Platform 36** for builds
- A **MongoDB 4.4+** server (Atlas, self-hosted) reachable from the device

## Features (lean MVP)

- Save and manage multiple connections (URI + display name). Connection URIs
  including embedded passwords are encrypted at rest using **Android Keystore
  AES/GCM**.
- Browse databases and collections, with size + estimated document counts.
- Drop databases and collections with type-to-confirm destructive UX.
- Find documents with `filter`, `projection`, `sort`, paginated (default
  25/page). Live JSON-syntax validation on each input.
- View, insert, edit, and delete individual documents through a JSON editor
  with live parse validation and Format button.
- Inspect indexes (name, keys, unique/sparse/TTL/partial chips, raw JSON).
- Adaptive layout: three-pane on tablet (expanded), stacked nav on phone.
- Theme picker (System / Light / Dark), persisted via DataStore.

## Building

### Via Android Studio
Open the project directory, let Android Studio sync, then Run on an
emulator or connected device.

### Command line
```bash
./gradlew assembleDebug
adb install app/build/outputs/apk/debug/app-debug.apk
```

You need `local.properties` pointing at your Android SDK:
```
sdk.dir=/path/to/Android/Sdk
```

## Adding a connection

1. Tap **+** on the Connections screen.
2. Give it a name and paste the URI (e.g. `mongodb+srv://user:pass@cluster.mongodb.net/`).
3. Tap **Test** to verify, then **Save**.
4. Tap the connection row's play button to connect.

The password segment is masked in the URI field by default; toggle the lock
icon to reveal.

## Credential storage

- Connection URIs (including embedded passwords) are encrypted at rest with
  **AES/GCM** using a key generated and held inside the **Android Keystore**
  (alias `mongo_client_master_v1`). The key never leaves the secure storage.
- The local SQLite (Room) database stores only ciphertext + IV.
- Decrypted URIs live in memory only when the user is interacting with a
  connection. They are never logged.
- Killing or uninstalling the app drops the in-memory copies. Uninstalling
  also destroys the Keystore key, making the stored ciphertext unrecoverable.

## Architecture

- **Kotlin + Jetpack Compose + Material 3**, single Activity, single module
- **Hilt** for DI, **Room** for connection storage, **DataStore** for settings
- **Navigation Compose** for routing
- **Coroutines + Flow** throughout the data layer; ViewModels expose
  `StateFlow<UiState>` per screen
- Direct **`mongodb-driver-kotlin-coroutine` 5.2.1**, but with several Android
  compatibility layers required to make it work — see [Known limitations]
  (#known-limitations).

## Known limitations

Explicitly out of scope for this MVP:

- Aggregation pipeline builder (write raw filters instead)
- Schema analysis / type sampling
- Index create or drop (read-only viewer)
- Validation rules editor
- Explain plans
- Server / database / collection stats dashboards
- GridFS
- Atlas Search indexes
- Change streams / live query
- More than one connection active at a time
- Document import / export
- Atlas Device Sync / Realm
- iOS or desktop targets
- Play Store / Play Console distribution, automated CI signing

## Android + MongoDB driver workarounds

The driver was not designed with Android in mind. Five separate compatibility
fixes ship in the app — losing any one breaks Atlas connections entirely:

1. **DNS SRV resolver** (`dnsjava`) registered via SPI `META-INF/services/
   com.mongodb.spi.dns.DnsClientProvider`. Android omits the
   `javax.naming.directory.*` packages that the driver's default JNDI-backed
   resolver depends on.
2. **Netty transport** (`io.netty:netty-handler`) replaces the driver's
   default async transport. The vendored `TlsChannel` has a NIO buffer-state
   bug on Android that hangs the SSL handshake indefinitely.
3. **Standalone Conscrypt** SSLContext, passed through Netty's SslHandler.
4. **`javax.security.sasl.*` stub classes** shipped in
   `app/src/main/java/javax/security/sasl/` so SCRAM authentication can load
   its `SaslClient` implementation. Android omits the whole package.
5. **SLF4J binding** so connection-thread failures aren't silently swallowed
   by the default NoOp logger.

## Building a release APK

Release builds need a keystore (one-time generation):

```bash
keytool -genkeypair -v \
  -keystore ~/keystores/mongo-client-release.jks \
  -alias mongo_client \
  -keyalg RSA -keysize 2048 -validity 10000
```

Set the following Gradle properties (in `~/.gradle/gradle.properties` —
never inside the project tree):

```properties
MONGO_CLIENT_KEYSTORE_PATH=/home/<user>/keystores/mongo-client-release.jks
MONGO_CLIENT_KEYSTORE_PASSWORD=…
MONGO_CLIENT_KEY_ALIAS=mongo_client
MONGO_CLIENT_KEY_PASSWORD=…
```

Then:

```bash
./gradlew assembleRelease
apksigner verify --verbose app/build/outputs/apk/release/app-release.apk
```

The keystore file is the long-term identity of the app — back it up safely.
There is no recovery if lost.

## Troubleshooting

**"Failed looking up TXT record for host …" the moment you paste a URI** —
the in-app parser should validate without DNS. If you see this, you've
managed to call `com.mongodb.ConnectionString(...)` from the UI thread; check
that it's only constructed inside a `withContext(Dispatchers.IO)` block.

**Connection hangs forever, then times out** — most often the cluster's IP
allowlist (Atlas: Security → Network Access). Verify the device's public IP
is in the allowlist. Compass connecting from the same network is sufficient
proof the cluster itself is reachable.

**"Authentication failed"** — the URI's username/password is wrong. Edit the
connection and try **Test** with a known-good password first.
