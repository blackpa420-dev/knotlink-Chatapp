# Phase 00 Finalization & Technical Contract Summary

## 1. Final Architecture Decisions
- **Server Architecture**: Option A (Firebase Cloud Functions) selected as the secure server-side OTP and API proxy layer. BulkSMSBD credentials never exist in the Android APK.
- **Room Role**: Local cache + offline queue only. Authoritative Source of Truth is Cloud Firestore. Room never blindly overwrites newer server data.
- **Registration Rule**: OTP verification **does not** create a permanent account. Account creation happens atomically only upon profile completion, username validation, and tapping "Create Account" (`phone_index`, `users`, `usernames`). Failure results in a complete rollback with zero orphaned records.
- **Login Rule**: OTP verification -> `phone_index` lookup -> existing UID -> authenticate existing identity. If not found, returns `ACCOUNT_NOT_FOUND` (never silently creates a duplicate account).

## 2. Audit Result
- Inspected existing codebase and identified legacy auth patterns mixing OTP with permanent account creation.
- Established strict architectural separation between BulkSMSBD OTP verification and permanent Firestore account creation.

## 3. Registration Flow
Phone -> Request OTP via Cloud Functions/BulkSMSBD -> Verify OTP -> **Temporary Registration Session (Zero permanent user records created)** -> Profile Setup -> Username Validation -> Atomic Account Creation (`phone_index`, `users`, `usernames`) -> Home.

## 4. Login Flow
Phone -> Request OTP -> Verify OTP -> Canonical Phone Lookup in Firestore (`phone_index`) -> Existing Account Restored. If not found -> `ACCOUNT_NOT_FOUND` -> Redirect to Registration.

## 5. Firebase Responsibilities
- Auth: Sessions
- Firestore: Authoritative persistent records
- RTDB: Presence & Typing
- Storage: Media
- Cloud Functions: Secure BulkSMSBD proxy, OTP verification, and atomic registration

## 6. Room Responsibilities
- Local cache and offline queue. Follows strict READ, WRITE, and OFFLINE WRITE flows.

## 7. Permanent vs. Temporary Data
- **Temporary**: OTP hashes (3-min TTL), temporary registration session tokens (15-min TTL).
- **Permanent**: User profiles, phone index, username claims, chats, and messages.

## 8. Ten-Phase Roadmap
1. Auth + OTP
2. Registration + Profile + Username
3. Firestore + Room Cache
4. Search & Chat Creation
5. Messaging & Inbox
6. Realtime Presence & Typing
7. FCM Notifications
8. Media & Storage
9. Security & Offline Robustness
10. Testing & Hardening

## 9. Prerequisites for Phase 1
- Phase 00 planning and finalization documents complete in `/PLAN`.
- Explicit approval from Lead Architect/User.
