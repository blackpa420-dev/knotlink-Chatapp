# Phase 1 Implementation: Authentication + OTP + Identity Foundation

## 1. Overview
Phase 1 implements the core authentication and identity foundation for BitChat:
- **Phone Input Validation**: Local Bangladesh format (`01XXXXXXXXX`, exactly 11 digits, prefixes `013` through `019`).
- **Canonicalization**: Internal backend canonical representation (`+8801712345678`) derived from user input.
- **OTP Verification & Delivery**: Managed securely via backend proxy / Cloud Functions integration with BulkSMSBD.
- **Temporary Registration Session**: OTP verification for new users establishes a 15-minute temporary session without creating permanent Firestore records.
- **Atomic Registration**: Final account creation atomically writes `phone_index/{canonicalPhone}`, `users/{uid}`, and `usernames/{username}`.
- **Existing User Login**: Resolves canonical phone to existing UID, mints Firebase Custom Auth token, signs in via `FirebaseAuth.signInWithCustomToken()`, and restores profile and chat data.
- **Room Cache**: Acts strictly as local offline cache and read-through/write-through mirror.

## 2. Technical Components
- **Phone Normalization Helper**: `PhoneUtils.canonicalizePhone(localPhone)`
- **Repository Layer**: `BitChatRepository` & auth data source.
- **ViewModel State**: Manages auth states, OTP requests, and verification tokens.
- **Firestore Schema**: `phone_index`, `users`, `usernames`.
