# Phase 5 Pre-Implementation Audit: Core Messaging System

## 1. Current Messaging Implementation
- **Room Entities**: `MessageEntity` and `ChatEntity` exist in Room (`Entities.kt`).
- **Repository**: `BitChatRepository` has local message insertion and pre-population, but needs production-grade remote message sending (`POST /api/v1/messages/send`), idempotency with `clientMessageId`, pagination (`GET /api/v1/messages`), and offline queue synchronization (`syncStatus`).
- **ViewModel**: `BitChatViewModel` handles general state, but needs reactive message observation and send/retry handling for `ChatDetailScreen`.

## 2. Security & Architecture Requirements
- Server-authoritative message generation (`messageId`).
- Client-generated idempotency key (`clientMessageId` via UUID).
- Backend must validate chat participation (`request.auth.uid in participantUids`).
- Room remains offline cache/queue, never authoritative source of truth.
- Future-proof schema supporting E2EE ciphertext fields without immediate cryptographic implementation.
