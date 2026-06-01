# Changelog

All notable changes to Sextant are recorded here.
Format based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/);
the project follows [Semantic Versioning](https://semver.org/).

## [0.1.2] - 2026-06-01

 - Fix silent failures when attempting to drop collections and databases. 
 - Added ability to create new databases and collections.

## [0.1.1] — 2026-05-26

First release. Lean MVP of a native Android client for MongoDB, talking
directly to any deployment reachable over `mongodb://` or `mongodb+srv://`
(Atlas, self-hosted, replica sets, sharded). Side-load only for this release.

### Added — Connection management
- Save multiple named connections. URIs including embedded passwords are
  encrypted at rest with AES/GCM via Android Keystore (key alias
  `mongo_client_master_v1`, never leaves secure storage).
- Connection editor with show/hide password toggle (in-place character
  masking, cursor offsets preserved).
- **Test** button that probes the URI without saving.
- Light structural URI parser for live editor validation (no network calls
  on the UI thread).

### Added — Browsing
- Database list with size on disk and empty-state indicators.
- Toggle to show/hide `admin` / `local` / `config` system databases;
  default configurable in Settings.
- Collection list with type chips (view, time-series) and estimated
  document counts.
- Per-collection **Documents** / **Indexes** tab switcher.
- Compass-style type-to-confirm dialogs for dropping databases or
  collections.

### Added — Documents (find + CRUD)
- Paginated `find` with skip / limit (25/page default); prev/next pager.
- Filter, projection, and sort fields as raw JSON with per-field inline
  parse error reporting.
- Status header showing `Showing X–Y of N`; fast metadata count for
  unfiltered queries, exact `countDocuments(filter)` for filtered ones.
- Document detail view with formatted, syntax-highlighted JSON; selectable
  for copy-paste.
- JSON editor for insert and edit with live parse validation, **Format**
  button, and auto-generated `ObjectId` pre-filled in the insert template
  (editable).
- Single-click confirm dialog for document deletion.
- BSON ↔ JSON round-trip via MongoDB Extended JSON v2 (RELAXED mode);
  `ObjectId`, `Date`, `Long`, etc. render readably and survive edits.

### Added — Indexes
- Read-only index list per collection.
- Per-index card shows name, keys with `↑`/`↓` direction (or `2dsphere` /
  `text` / `hashed` verbatim), chips for `unique` / `sparse` / `TTL` /
  `partial`.
- Tap-to-expand reveals the raw JSON definition.

### Added — Adaptive UI
- `WindowSizeClass`-driven layouts: three-pane
  (databases · collections · content) on Expanded; stacked nav with
  back-handler on Medium and Compact.
- Hand-tuned blue-grey Material 3 ColorScheme for light and dark modes.
- Settings screen — theme picker (System / Light / Dark) and default
  show-system-databases toggle; persisted via DataStore.
- Sextant-themed adaptive launcher icon (white sextant silhouette on
  blue-grey).
- Connection-lost banner with in-place **Reconnect** so a transient drop
  doesn't force a trip back to the connection list.
- JSON syntax highlighting (keys / strings / numbers / `true`/`false`/`null`)
  in document cards, detail views, editor, and index raw views; palette
  adapts to both modes.

### Added — Reliability
- Centralised Mongo error mapping: cryptic driver exceptions render as
  "Authentication failed", "Connection lost", duplicate-key, not-authorised,
  etc.
- SLF4J `slf4j-simple` binding so connection-thread errors surface in
  logcat instead of vanishing into the NoOp logger.
- 24 unit tests across connection-string parsing, JSON formatting/parsing,
  JSON syntax tokenisation, and index-definition parsing.

### Added — Build / release infrastructure
- Signed release APK + AAB build with R8 + resource shrinking enabled.
- Tuned ProGuard rules for MongoDB driver internals, Netty, Conscrypt,
  Hilt-generated code, Room, and the in-app SASL stubs.
- `versionCode` and `versionName` sourced from `version.properties` —
  single source of truth, bumped manually per release.
- 512×512 Play Store listing icon at `play_store/icon-512.png`, generated
  from a tracked SVG.
- README with build / run instructions, credential-storage explainer,
  known limitations, and signed-release procedure.

### Internal — Android compatibility for the MongoDB driver
The official Kotlin coroutine driver doesn't ship Android-ready. Five
distinct compatibility layers are required and shipped with this release:
1. Custom `com.mongodb.spi.dns.DnsClient` implementation backed by
   `dnsjava` (Android omits the `javax.naming.*` JNDI packages the driver
   uses for SRV/TXT resolution).
2. Netty transport (`io.netty:netty-handler`) substituted for the driver's
   vendored TlsChannel, which has a NIO buffer-state bug on Android that
   stalls the TLS handshake indefinitely.
3. Standalone Conscrypt `SSLContext`, fed through Netty's `SslHandler`.
4. Local stub classes in `javax.security.sasl.*` (`SaslClient`,
   `SaslException`, `AuthenticationException`, `SaslClientFactory`,
   `Sasl`) so SCRAM authentication can link.
5. SLF4J binding (see Reliability) — failures in driver background threads
   are otherwise silent.

### What's not in 0.1.0 (deliberately deferred)
Aggregation pipeline builder, schema analysis, index create/drop,
validation rules, explain plans, server/database/collection stats,
GridFS, Atlas Search indexes, change streams, multiple concurrent
connections, document import/export, Atlas Device Sync, iOS or desktop
targets, Play Store distribution.

[Unreleased]: #
[0.1.0]: #
