# Phase 6 Milestone 4 Implementation Plan: Production Read Receipts

## 1. Objectives
- Implement authoritative batch read acknowledgment (`markMessagesAsRead`) in `BitChatRepository` via Firestore transactions.
- Enforce strict state machine (`DELIVERED → READ`) with server timestamp (`readAt`).
- Trigger read receipts automatically when the recipient opens and views `ChatDetailScreen` for unread messages authored by the other participant.
- Ensure offline read ACKs survive app restart and sync upon reconnection.
- Update sender UI to reflect `READ` state.

## 2. Step-by-Step Execution Plan
1. **Repository & Backend ACK**:
   - Add `markMessagesAsRead(chatId: String, messageIds: List<String>)` in `BitChatRepository`.
   - Implement Firestore transaction validating chat membership, recipient status (`senderUid != currentUid`), and state transition (`DELIVERED → READ`).
2. **UI Integration**:
   - In `ChatDetailScreen` or `BitChatViewModel`, trigger `markMessagesAsRead` when entering the chat and observing unread messages from the other user.
3. **Room & State Reconciliation**:
   - Update Room DAO to batch update `deliveryState = 'READ'` and `readAt = serverTimestamp`.
   - Ensure UI observes authoritative state changes via Flow.
4. **Offline Persistence & Retry**:
   - Implement local pending read queue if offline, ensuring idempotency and survival across app restarts.
5. **Testing & Verification**:
   - Verify single/bulk read, idempotency, unauthorized read rejection, offline queue retry, and regression of Phases 1–5 & Milestones 1–3.
