# BitChat Master Architecture

## Overview
BitChat is a highly secure, real-time decentralized-style Android messaging application built with Kotlin and Jetpack Compose. It strictly separates authentication verification from permanent account creation, ensuring that uncompleted onboarding processes leave no persistent user records behind.

---

## High-Level Architecture Principles

1. **Strict Separation of Concerns**:
   - **OTP Delivery & Server Logic**: Handled exclusively via **Firebase Cloud Functions** proxying requests to **BulkSMSBD**. BulkSMSBD API keys never reside in the Android APK.
   - **Authentication & Identity**: Handled by **Firebase Authentication** and **Cloud Firestore** mapped via canonical phone numbers.
   - **Realtime State (Ephemeral)**: Handled by **Firebase Realtime Database (RTDB)** for presence, typing indicators, and delivery/read state tickers.
   - **Permanent Data (Authoritative)**: Handled by **Cloud Firestore** and **Firebase Storage**.
   - **Local Cache (Offline-First)**: Handled by **Room Database** on the Android client. Room is strictly a read-through/write-through local cache and offline queue, never the ultimate source of truth.

2. **Clean State Transition Model**:
   - OTP Verification **does not** create permanent user accounts.
   - Successful OTP validation yields a temporary cryptographic verification session token.
   - Permanent user documents, phone index entries, and username claims are created **atomically only when the user submits their completed profile and username**.

3. **Canonical Phone Identity**:
   - All phone numbers are normalized to E.164 canonical format (`+880...`) before any lookup or database query.
   - A canonical phone number maps deterministically via `/phone_index/{canonicalPhone}` to one and only one permanent user identity.
