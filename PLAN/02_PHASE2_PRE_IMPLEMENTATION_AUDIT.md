# Phase 2 Pre-Implementation Audit

## 1. Current Profile Implementation
- **UI**: `RegisterIdentityScreen.kt` and `ProfileSettingsScreen.kt` handle profile setup and settings.
- **State**: Managed via `BitChatViewModel` with state flows for `enteredFullName`, `enteredUsername`, `enteredAvatarPath`, etc.
- **Persistence**: Currently stored in local Room `UserIdentityEntity`. Needs integration with authoritative backend endpoints (`users/{uid}` and `usernames/{normalizedUsername}`).

## 2. Current Registration Flow
- **Phase 1 Foundation**: Phone input (`01XXXXXXXXX`), OTP request, and OTP verification established via secure Cloud Function proxy.
- **Temporary Session**: Verified OTP yields temporary registration session token.
- **Missing Link**: Transition from temporary session to profile completion, username uniqueness check, atomic account creation (`phone_index`, `users`, `usernames`), and Firebase Custom Token sign-in (`signInWithCustomToken()`).

## 3. Current Username Implementation
- Client-side checks only. Needs authoritative server-side availability check (`GET /api/v1/usernames/check`) and atomic claim (`POST /api/v1/usernames/claim` or atomic registration transaction).
- Case-insensitivity (`zabir` vs `Zabir`) and reserved list validation (`admin`, `support`, etc.) must be strictly enforced on backend/Cloud Functions.

## 4. Current Room Entities & Migration
- `UserIdentityEntity` exists in `Entities.kt`.
- Room database uses `fallbackToDestructiveMigration()`. For Phase 2, ensure entities align properly without destructive loss of chat/contact tables where possible, or document migration requirements.

## 5. Current Firestore & Storage Structure
- `phone_index/{canonicalPhone}`
- `users/{uid}`
- `usernames/{normalizedUsername}`
- Firebase Storage: `users/{uid}/profile/avatar` for profile photo uploads.

## 6. Security & Compatibility Risks
- Client-side UID generation or phone modification attempts must be blocked by Firestore Security Rules.
- Race conditions on username claims must be prevented via atomic transactions.
- Orphaned documents on registration failure must be avoided.
