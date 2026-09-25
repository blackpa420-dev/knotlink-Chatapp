# Phase 6 Pre-Implementation Audit: Real-Time Messaging, FCM Notifications & Presence

## 1. Current Implementation Audit
- **Phases 1-5 Complete**: Authentication (Firebase Auth + Custom Token), Profile setup and persistence, Username search and discovery, Chat initialization (`initChat` with deterministic canonical `chatId`), and Core 1:1 Messaging (client `clientMessageId` idempotency, server `serverMessageId`, Room cache with `syncStatus` and `deliveryState`).
- **Missing in Phase 6**: 
  - Firebase Cloud Messaging (FCM) service for background message delivery and notifications.
  - Multi-device FCM token management (`user_devices/{deviceId}`).
  - Realtime Database (RTDB) ephemeral presence (`status/{uid}`) and typing indicators (`typing/{chatId}/{uid}`).
  - Server-authoritative delivery state progression (`SENT → DELIVERED → READ`) with authenticated acknowledgement APIs.
  - Room deduplication and reconciliation between Firestore realtime events and FCM pushes.

## 2. Existing Code Reusable
- `MessageEntity` in Room (already contains `clientMessageId`, `serverMessageId`, `deliveryState`, `syncStatus`, etc.).
- `BitChatRepository` and `BitChatViewModel` message flow foundations.

## 3. Code that Must Be Created / Modified
- **Create**:
  - `BitChatMessagingService.kt`: Extends `FirebaseMessagingService` to handle incoming FCM data payloads, validate UIDs, insert into Room, and display grouped notifications.
  - `PresenceManager.kt`: Manages RTDB `.info/connected` and `onDisconnect()` presence states.
  - `TypingManager.kt`: Manages ephemeral typing state in RTDB.
  - Token registration repository methods (`POST /api/v1/devices/token`).
  - Delivery and Read receipt repository methods (`POST /api/v1/messages/deliver`, `POST /api/v1/messages/read`).
- **Modify**:
  - `BitChatRepository.kt`: Add token sync, delivery/read acknowledgment calls, and FCM/Firestore deduplication logic.
  - `AndroidManifest.xml`: Register `BitChatMessagingService`.

## 4. Firestore & RTDB Schema
- **Firestore**:
  - `/chats/{chatId}`
  - `/chats/{chatId}/messages/{messageId}`
  - `/user_devices/{deviceId}` (uid, deviceId, installationId, fcmToken, platform, appVersion, lastSeenAt, isActive)
- **RTDB**:
  - `/status/{uid}` (state: online/offline, lastSeen, connectionState)
  - `/typing/{chatId}/{uid}` (isTyping: boolean, updatedAt: timestamp)

## 5. FCM Payload Contract
```json
{
  "type": "CHAT_MESSAGE",
  "chatId": "chat_xxx",
  "messageId": "msg_xxx",
  "clientMessageId": "uuid",
  "senderUid": "uidA",
  "text": "Hello",
  "serverTimestamp": "1723456789000"
}
```

## 6. Security Rules & Authorizations
- Firestore & RTDB rules must enforce `request.auth.uid == uid` for devices/presence/typing and `request.auth.uid in participantUids` for chat messages.
