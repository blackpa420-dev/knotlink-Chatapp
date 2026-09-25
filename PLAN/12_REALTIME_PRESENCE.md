# Realtime Presence & Typing Indicators

## 1. Ephemeral State Architecture
- **Storage**: Firebase Realtime Database (RTDB).
- **Presence Node**: `/presence/{userId}` with `.info/connected` hook to automatically set `status: "offline"` on disconnect.
- **Typing Node**: `/typing/{chatId}/{userId}` set to `true` while typing, auto-cleared after 3 seconds of inactivity.
- **Rationale**: RTDB is used instead of Firestore for high-frequency ephemeral state to avoid Firestore write rate limits and unnecessary billing overhead.
