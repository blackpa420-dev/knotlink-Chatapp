# Phase 5 Implementation Plan: Core Messaging System

## 1. Objectives
- Implement production-grade core 1-to-1 messaging pipeline (send, idempotency, server timestamp ordering, pagination, offline queue).
- Support client-generated `clientMessageId` (UUID) to prevent duplicates upon retry.
- Server-authoritative `messageId` generation and backend validation of participant membership.
- Room cache synchronization and optimistic UI updates (`SENDING`, `SENT`, `FAILED`, `RETRY`).
- Maintain 100% backward compatibility with Phases 1-4.

## 2. Step-by-Step Execution Plan
1. **Repository & API Integration**: Add `sendMessage(chatId, text, clientMessageId)` and `observeMessages(chatId)` in `BitChatRepository`.
2. **ViewModel Extension**: Add message sending actions, retry logic preserving `clientMessageId`, and active conversation state in `BitChatViewModel`.
3. **UI Integration**: Connect `ChatDetailScreen` to send and observe messages reactively with proper delivery states.
4. **Offline Queue & Pagination**: Implement cursor-based pagination and offline sync queue (`syncStatus`).
5. **Testing & Verification**: Execute thorough test matrix for messaging, idempotency, retry, offline sync, and security.
