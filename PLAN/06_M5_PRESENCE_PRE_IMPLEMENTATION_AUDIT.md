# Phase 6 Milestone 5 Pre-Implementation Audit: Presence, Typing Indicators & Multi-Device Realtime Consistency

## 1. Current State Audit
- **Firestore / Messaging / Read Receipts**: Milestone 1–4 are fully operational.
- **Realtime Database**: Firebase Realtime Database SDK dependency is present (`libs.firebase.database`), but presence and typing nodes (`status/{uid}`, `typing/{chatId}/{uid}`) are not yet implemented in production source code.
- **Multi-Device Requirements**: Must support multiple active device sessions per user (`presence/{uid}/{deviceId}` or session-aware connection monitoring via `.info/connected` and `onDisconnect`).
- **Typing Indicators**: Ephemeral typing state per chat (`typing/{chatId}/{uid}`) with debounce, auto-expire, and cleanup on send/leave/kill.

## 2. Reusable Existing Architecture
- `FirebaseAuth.getInstance()` for current authenticated user UID.
- Kotlin Coroutines & Flows for reactive state management in ViewModel and UI.
- `ChatDetailScreen` for chat header presence and typing display.

## 3. Architecture & Security Requirements
- **RTDB vs Firestore**: Ephemeral presence and typing state use RTDB to prevent Firestore write rate limits.
- **Security Rules**: Users may only write their own status (`status/{uid}`) and their own typing node (`typing/{chatId}/{uid}`).
- **Privacy**: Support toggling online status / last seen preferences.
- **Lifecycle**: Clean disconnect handlers, no global status/typing listeners across the entire app population.
