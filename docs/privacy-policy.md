---
title: Sextant — Privacy Policy
---

# Privacy Policy for Sextant

_Last updated: 2026-06-01_

Sextant ("the app") is a native Android client that lets you connect to and
manage MongoDB databases that you control. This policy explains what the app
does with your data.

## TL;DR

- The app does not collect, transmit, or sell any personal data to the
  developer or to any third party.
- It has no analytics, no advertising, no crash-reporting SDKs, and no
  tracking of any kind.
- The only network traffic the app generates is to the MongoDB servers
  whose connection strings you explicitly add.
- Connection strings (including any embedded password) are stored on your
  device only, encrypted at rest.

## Data the app stores on your device

The app stores the following data **locally on your device**, inside its
private app-data directory. None of it is uploaded anywhere by the app:

- **Connection profiles** — the name you give a connection and its URI.
  If the URI contains a username and password (e.g.
  `mongodb+srv://user:pass@cluster.example.net`), those credentials are
  part of the URI and are stored with it.
- **Preferences** — theme choice (System / Light / Dark) and the
  show-system-databases default.

### How connection strings are protected

Connection strings are encrypted at rest using AES/GCM. The encryption
key is generated on first use and held inside the Android Keystore under
the alias `mongo_client_master_v1`. The key itself never leaves secure
storage and cannot be exported by the app.

Uninstalling the app deletes the app-data directory and, with it, all
stored connection profiles and preferences.

## Data the app sends over the network

When you open or test a saved connection, the app contacts the MongoDB
endpoint specified by that connection's URI. Standard MongoDB driver
traffic flows between your device and that endpoint: authentication
handshakes, queries you run, documents you read or write, and so on.

The endpoint is the server **you chose** — typically MongoDB Atlas or a
server you operate. The developer of this app is not party to that
traffic, does not see it, and does not log it.

For `mongodb+srv://` URIs, the app performs DNS SRV and TXT lookups
against your device's configured DNS resolver to discover the cluster's
seed hosts. This is the standard MongoDB SRV record lookup, identical to
what the official drivers do; no DNS data is sent to the developer.

## Permissions

The app requests two Android permissions, both for the functionality
described above:

- **INTERNET** — required to open the TCP/TLS connection to the MongoDB
  endpoint you configure.
- **ACCESS_NETWORK_STATE** — used to detect when network connectivity
  drops so the app can show a "Connection lost" banner and offer to
  reconnect.

The app does not request location, contacts, camera, microphone,
storage-scoped media, or any other sensitive permission.

## Third-party services

The app uses the following open-source libraries inside the app process
(none of them transmits data to a remote service on the app's behalf):

- The official MongoDB Kotlin coroutine driver (for talking to your
  MongoDB endpoint).
- dnsjava (for SRV/TXT lookups on Android).
- Netty and Conscrypt (TLS plumbing).
- AndroidX libraries (UI, navigation, persistence, biometrics).

No advertising networks, analytics providers, or backend services
operated by the developer are integrated.

## Children's privacy

Sextant is a database-administration tool intended for adults. It is not
directed at children under 13. No data about any user, including
children, is collected by the developer.

## Your responsibilities

Sextant is a tool that lets you act on databases you control. You are
responsible for:

- The security of the credentials you enter into the connection editor.
- The actions you perform on your databases (queries, edits, deletes,
  drops).
- Compliance with any data-protection obligations that apply to data
  living in those databases.

The developer cannot recover lost data or undo destructive operations
performed through the app.

## Changes to this policy

If this policy changes materially, the updated version will be published
at the same URL and the "Last updated" date above will change. Continued
use of the app after such a change indicates acceptance of the updated
policy.

## Contact

Questions, concerns, or data-related requests:

- Email: dmcarrington@googlemail.com
- Issues: https://github.com/dmcarrington/android-mongo-client/issues
