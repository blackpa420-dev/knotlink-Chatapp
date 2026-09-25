# Final Architecture Decisions (Binding Contract)

## 1. Selected Server-Side OTP & API Architecture
**Decision: Option A — Firebase Cloud Functions**
- **Rationale**: Firebase Cloud Functions provide a secure, server-side Node.js/TypeScript execution environment that integrates natively with Firebase Authentication, Firestore, and Admin SDKs. This eliminates the need to host, scale, and maintain a separate backend server infrastructure while securely isolating BulkSMSBD API keys and secrets entirely away from the Android APK.

## 2. Room Database Role & Data Flow
- **Authoritative Source of Truth**: Cloud Firestore / Firebase Backend.
- **Room Role**: Local cache and offline queue only. Room must never blindly overwrite newer server data.
- **Data Flows**:
  - **READ**: `Server/Firebase -> Repository -> Room -> UI`
  - **WRITE**: `UI -> Repository -> Server/Firebase -> Confirmed Result -> Room Update -> UI`
  - **OFFLINE WRITE**: `UI -> Repository -> Room (PENDING status) -> Network available -> Server -> Confirmation -> Room (SYNCED status)`

## 3. Strict Registration Rule
- OTP verification **does not** create a permanent account.
- Permanent account creation occurs **only** after:
  1. OTP verified successfully
  2. Temporary registration session token valid
  3. Profile completed (Photo, Name, Username)
  4. Username validated & available
  5. User presses "Create Account"
- **Atomic Transaction**: Account creation establishes three records simultaneously:
  - `/phone_index/{canonicalPhone}`
  - `/users/{uid}`
  - `/usernames/{username}`
- If any part of the transaction fails, the operation rolls back entirely, leaving zero orphaned or partial identity records.

## 4. Strict Login Rule
- Login flow: `Phone -> Request OTP -> Verify OTP -> phone_index lookup -> Existing UID -> Authenticate existing identity -> Restore profile & chat data -> Home`.
- If `phone_index` does not exist for the canonical phone number, the system returns `ACCOUNT_NOT_FOUND` and directs the user to registration.
- **Never silently create a new account during login.**

## 5. Permanent vs. Temporary Data
- **Temporary Data**: OTP temporary records (hashed, 3-minute TTL), temporary registration session tokens (15-minute JWT TTL).
- **Permanent Data**: User profiles (`users/{uid}`), phone mappings (`phone_index/{phone}`), username reservations (`usernames/{username}`), chats, and messages.
