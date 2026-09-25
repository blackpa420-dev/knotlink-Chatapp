# Phase 9: Security, Privacy & Account Management — Pre-Implementation Audit

## 1. Audit Scope & Context
This pre-implementation audit analyzes the existing architecture and security posture across Android client and Firebase backend services to prepare for **Phase 9: Security, Privacy & Account Management**.

Verified Baseline:
- Phase 1: Authentication & Phone OTP Verification
- Phase 2: User Profile & Identity
- Phase 3: Global Directory & Search
- Phase 4: Deterministic 1-on-1 Chat Architecture
- Phase 5: Core Messaging Engine & Room Synchronization
- Phase 6: Delivery ACK, Read Receipts, Typing Indicators & RTDB Multi-Device Presence
- Phase 7: Media Storage, Checksums & Upload Pipeline
- Phase 8: Group Architecture, Moderation, Roles (OWNER/ADMIN/MEMBER), Permissions & Room v10

---

## 2. Security & Architecture Audit

### 2.1 Firebase Authentication & Session Management
- **Current State**: Uses custom phone OTP verification model mapping verified phone numbers to authoritative Firebase Auth UIDs.
- **Audit Findings**:
  - Device sessions are registered in `user_devices` collection upon FCM token receipt.
  - Revocation handling requires explicit server-side status (`ACTIVE`, `REVOKED`, `LOGGED_OUT`) checks so that revoked sessions immediately terminate remote and notification access.
  - Multi-device sessions must be enumerated under `/users/{uid}/devices/{deviceId}` or `/user_devices/{deviceId}` for transparent user visibility and remote termination.

### 2.2 Privacy Controls & Presence Security
- **Current State**: Presence is tracked under RTDB `status/{uid}/{deviceId}` with `online` and `lastSeen`.
- **Audit Findings**:
  - Current UI has local toggles, but privacy preferences must be persisted in user profile (`privacy_settings`) and enforced server-side / client-side queries.
  - Evaluated Privacy Policy:
    - Profile Photo Visibility: `EVERYONE`, `CONTACTS`, `NOBODY`
    - Online Status Visibility: `EVERYONE`, `CONTACTS`, `NOBODY`
    - Last Seen Visibility: `EVERYONE`, `CONTACTS`, `NOBODY`
    - Direct Message Permissions: `EVERYONE`, `CONTACTS`, `NOBODY`
    - Read Receipt Visibility: `ENABLED` (true), `DISABLED` (false)
    - Typing Indicator Privacy: `ENABLED` (true), `DISABLED` (false)
  - When Online / Last Seen privacy is set to `NOBODY` or `CONTACTS`, presence broadcasting and listeners must respect privacy flags without breaking socket heartbeat connectivity.

### 2.3 Blocking & Reporting Engine
- **Current State**: Local `_blockedContacts` in ViewModel; basic `reportMemberOrGroup` in Repository writing to `/reports`.
- **Audit Findings**:
  - Needs structured schema:
    - `/users/{uid}/blocked/{targetUid}` in Firestore and Room table/cache.
    - Blocking suppresses 1-on-1 message delivery, profile metadata resolution, and incoming push notifications from blocked users.
    - History remains intact (block is distinct from deletion).
  - Abuse Reporting:
    - Unified model supporting target types: `USER`, `MESSAGE`, `GROUP`.
    - Sanitized report fields: `reportId`, `reporterUid`, `targetType`, `targetId`, `reason`, `details`, `status` ("PENDING"), `createdAt`. Reporter identity is protected.

### 2.4 Device & Session Management
- **Current State**: Device ID generated and stored with FCM token in `user_devices/{deviceId}`.
- **Audit Findings**:
  - Need explicit device session model in Room and Firestore: `deviceId`, `platform`, `appVersion`, `lastActiveTimestamp`, `ipAddress`, `isCurrentDevice`, `isActive`.
  - Actions: "Logout This Device" (cleans local cache, unregisters FCM, deletes session) vs "Logout All Other Devices" (revokes other remote sessions in Firestore).

### 2.5 Account Deletion & Lifecycle Management
- **Current State**: App has local logout and clear data functions.
- **Audit Findings**:
  - Account deletion requires:
    - User confirmation.
    - Revoking all active device sessions and FCM tokens.
    - Setting Firestore user status to `DELETED` or `PENDING_DELETION`.
    - Anonymizing/disabling profile in public directory.
    - Complete purge of local Room database (`clearAllLocalData`).
    - Retaining group message integrity where appropriate (author displayed as "Deleted User").

### 2.6 Account Recovery Model
- **Current State**: Phone + OTP authentication.
- **Audit Findings**:
  - Verified phone number is the single authoritative recovery identity.
  - Phone -> OTP -> Authenticate existing UID -> Restore Firestore & Room state.
  - Never generates an orphaned new UID for an existing registered phone number.

### 2.7 Sensitive Logging & Local Security Audit
- **Current State**: Logging uses `Log.d` / `Log.w` with debug tags.
- **Audit Findings**:
  - Ensure zero logging of raw OTP codes, auth tokens, refresh tokens, private keys, or message plaintexts.
  - Local credentials stored in SharedPreferences / EncryptedSharedPreferences.
  - Room database schema migration from v10 to v11 to support blocked users cache and privacy settings cache non-destructively.

### 2.8 Firestore & Storage Security Rules Audit
- **Current State**: `firestore.rules` and `storage.rules` secure chats, members, pinned messages, reactions, and 25MB media files.
- **Audit Findings**:
  - Update `firestore.rules` to secure `/users/{uid}/blocked/{targetUid}`, `/users/{uid}/devices/{deviceId}`, `/user_devices/{deviceId}`, and `/reports/{reportId}`.
  - Prevent unauthorized users from reading reports, forging device records, or reading blocked lists of other users.

---

## 3. Pre-Implementation Audit Verdict
Architecture is fully verified through Phase 8. All Phase 9 security, privacy, session management, and account lifecycle components can be integrated without breaking changes.
