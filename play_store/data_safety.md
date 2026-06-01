# Play Console — Data Safety form answers

Reference answers for Sextant's submission to Google Play's Data Safety
section. Each answer includes the rationale you can paste into a review
response if the reviewer challenges the declaration.

> **Definitions that matter for these answers**
>
> Play's definition of "collect": _transmitting data off a user's device._
> Storage on-device is **not** collection. Data transferred to a third
> party **at the user's direct request** (Play's stated examples are
> sending an email or making a phone call) is **not** collection either —
> a MongoDB query addressed to a server the user themself entered is the
> same shape.

---

## Section 1 — Data collection and security

### Does your app collect or share any of the required user data types?

**Answer: No.**

**Rationale:**
- Sextant transmits no data to the developer or to any third party of the
  developer's choosing. There are no analytics, crash-reporting, ads, or
  telemetry SDKs in the app.
- The only outbound network traffic the app generates is to MongoDB
  endpoints that the user themself enters in the connection editor. That
  is a user-initiated transfer to a destination of the user's choice,
  which Play exempts from declaration.
- All data the user enters (connection name, URI including any embedded
  credentials, preferences) is stored locally inside the app's private
  data directory, encrypted at rest with AES/GCM under a key held in the
  Android Keystore (alias `mongo_client_master_v1`). The key never leaves
  secure storage.

### Is all of the user data collected by your app encrypted in transit?

**Answer: Yes.**

**Rationale:**
- `mongodb+srv://` URIs enforce TLS by default. `mongodb://` URIs support
  `tls=true` and the app does not strip or downgrade the transport.
- The app does not establish any other network connections.
- On-device, connection URIs are also encrypted at rest (AES/GCM via
  Android Keystore) — relevant if Play interprets this question broadly.

### Do you provide a way for users to request that their data be deleted?

**Answer: Yes.**

**Rationale:**
- All stored data is local to the device. Three deletion paths:
  1. Delete a connection in-app from the connection list.
  2. Use Android Settings → Apps → Sextant → Storage → **Clear storage**
     to wipe all profiles and preferences.
  3. Uninstall the app — the private app-data directory is removed by
     Android.
- There is no server-side data, so no developer-side deletion request
  process is needed.

### Has your app been independently validated against a global security standard?

**Answer: No** (leave unchecked).

This is the optional MASA (Mobile Application Security Assessment) badge.
Not applicable for an indie release.

---

## Section 2 — Data types

**Skip.** No data is declared as collected or shared in Section 1, so the
data-type breakdown does not need to be filled in.

If — and only if — a Play reviewer rejects the "No" answer and asks for a
declaration, the closest accurate fit would be:

| Data type | Collected/Shared | Purpose | Required/Optional | Processed ephemerally |
| --- | --- | --- | --- | --- |
| _(none currently applicable)_ | | | | |

Do not pre-emptively declare data types you don't actually collect — over-
declaring is itself a Play policy issue ("misleading data declarations").

---

## Adjacent questions on the same App content page

These aren't strictly Data Safety but they live in the same section of
Play Console and reviewers expect them filled in before the first submission.

| Question | Answer |
| --- | --- |
| Government app | No |
| News app | No |
| COVID-19 contact tracing or status | No |
| Contains ads | No |
| In-app purchases | No |
| Target audience age groups | **18+ only** (database admin tool; nothing in-app is appropriate for or aimed at minors) |
| Appeal to children | No |
| Privacy policy URL | The hosted URL of `docs/privacy-policy.md` once GitHub Pages is enabled (something like `https://dmcarrington.github.io/android-mongo-client/privacy-policy`) |
| Content rating | Complete the IARC questionnaire — selecting "No" for every sensitive-content question lands at **Everyone** / **PEGI 3** for a Tools-category app like this |
| App category | Tools |
| Data safety section policy commitment | Yes (you commit that your declarations are accurate) |

---

## If the reviewer challenges the "No collection" declaration

Paste this into the review response form:

> Sextant is a database administration client. All user-entered data —
> including connection strings that may contain embedded credentials — is
> stored locally on the device, encrypted at rest using AES/GCM with a
> key held in the Android Keystore. The app does not transmit any user
> data to the developer or to any third party of the developer's
> choosing, and it integrates no analytics, advertising, or telemetry
> SDKs.
>
> The only outbound network traffic the app generates is to the MongoDB
> endpoints that the user themself configures in the connection editor.
> Per Play's Data Safety guidance, transfers initiated by the user to a
> third party of their own choosing (analogous to sending an email or
> placing a phone call) are exempt from declaration as data collection.
>
> Source code is public at https://github.com/dmcarrington/android-mongo-client
> and confirms the absence of any developer-controlled data transmission.

---

## Things to avoid

- **Do not** declare "Personal info" because the URI contains a username
  and password — that field describes data sent **to the developer**, not
  credentials the user holds for their own server.
- **Do not** declare "App activity" with "Account management" as the
  purpose. The user has no account with the developer; the MongoDB
  credentials are for the user's own service.
- **Do not** tick "Data is encrypted in transit" while also declaring
  collection types you don't actually collect — over-declaring triggers
  the same enforcement path as under-declaring.
