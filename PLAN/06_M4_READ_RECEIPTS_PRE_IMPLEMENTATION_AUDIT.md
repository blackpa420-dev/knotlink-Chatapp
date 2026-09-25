# Phase 6 Milestone 4 Pre-Implementation Audit: Production Read Receipts

## 1. Current State Audit (Milestone 3 Baseline)
- **Delivered State**: Milestone 3 established `SENT → DELIVERED` via `sendDeliveryAcknowledgment()` in `BitChatRepository` and automatic triggers in `BitChatMessagingService`.
- **Missing in Milestone 4**:
  - Bulk or individual authoritative read acknowledgment (`markMessagesAsRead(chatId, messageIds)`).
  - Strict server-authoritative read state machine (`DELIVERED → READ`, blocking backward transitions like `READ → DELIVERED`).
  - UI triggers when user opens and views chat messages (`ChatDetailScreen`).
  - Offline read acknowledgment queue surviving app restart.
  - Sender UI state displaying `READ` (e.g. double checkmarks or read indicator).

## 2. Reusable Existing Architecture
- `MessageEntity` in Room (already contains `deliveryState`, `deliveredAt`, `readAt`, etc.).
- `BitChatRepository` message flow and Firestore integration patterns.
- `ChatDetailScreen` lifecycle events (`LaunchedEffect` when entering chat).

## 3. Architecture & Security Requirements
- **Server Authority**: Firestore transactions must validate that caller `uid` is a chat participant, recipient of the message (not sender), and message state allows transition to `READ`.
- **State Machine**:
  - `SENDING/SENT → DELIVERED` (Milestone 3)
  - `DELIVERED → READ` (Milestone 4)
  - Forbidden: `READ → DELIVERED`, `READ → SENT`, `SENT → READ` (must pass through `DELIVERED` or support direct `SENT → READ` if allowed by business logic, but strict `DELIVERED → READ` is safest).
- **Idempotency**: Repeated read ACKs must be harmless and idempotent.
- **Bulk Optimization**: Use batch message updates to prevent network congestion when opening a chat with multiple unread messages.
