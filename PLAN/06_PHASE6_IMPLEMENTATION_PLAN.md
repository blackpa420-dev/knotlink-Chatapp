# Phase 6 Implementation Plan: Real-Time Messaging, FCM Notifications & Presence

## 1. Objectives
- Implement Firebase Cloud Messaging (FCM) background/foreground message delivery and push notifications.
- Manage multi-device FCM tokens (`user_devices/{deviceId}`).
- Integrate Firebase Realtime Database for ephemeral presence (`status/{uid}`) and typing indicators (`typing/{chatId}/{uid}`).
- Enforce strict server-authoritative delivery state transitions (`SENT → DELIVERED → READ`) with authenticated acknowledgement APIs.
- Implement robust Room deduplication using `serverMessageId` to handle concurrent FCM and Firestore updates.

## 2. Step-by-Step Execution Plan
1. **FCM Token Management**: Implement token registration, refresh, and logout cleanup in repository and backend sync.
2. **FCM Background Service**: Create `BitChatMessagingService` to receive FCM data payloads, update Room, and display intelligent notifications.
3. **Presence & Typing**: Implement RTDB connection monitoring (`.info/connected`, `onDisconnect`) and debounced typing indicator listeners.
4. **Delivery & Read Receipts**: Add authenticated API endpoints/repository calls for delivery and read acknowledgements with valid state transitions.
5. **Room Reconciliation**: Ensure deduplication via `serverMessageId` across FCM pushes, Firestore listeners, and pagination.
6. **Testing & Verification**: Verify multi-device sync, offline queue, retry, read receipts, and regression of Phases 1-5.
