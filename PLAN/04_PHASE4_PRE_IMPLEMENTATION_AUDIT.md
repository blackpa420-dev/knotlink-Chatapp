# Phase 4 Pre-Implementation Audit: Chat & Conversation Foundation

## 1. Current Chat & Conversation Implementation
- **Room Entities**: `ChatEntity` already exists in `Entities.kt` with `id` (`chatId`), `name`, `lastMessage`, `participantUids`, etc.
- **Repository**: `BitChatRepository` has methods for local contacts, messages, and repository state, but needs authoritative `initChat(targetUid)` supporting server-side deterministic `chatId` generation and duplicate prevention.
- **ViewModel**: `BitChatViewModel` handles general chat state, search, and navigation. Needs integration for `startChat(targetUid)` and observing authoritative chat sessions.

## 2. Security & Architecture Requirements
- Chat IDs must be generated server-authoritatively or via deterministic hashing of sorted participant UIDs (`SHA-256(sort([uidA, uidB]))`).
- Android client must never decide or generate authoritative chat IDs.
- Firestore Security Rules must enforce that only authenticated participants (`request.auth.uid in participantUids`) can read/write chat documents.
- Race condition mitigation: Simultaneous initialization from both participants must resolve to a single authoritative chat document.

## 3. Scope Boundaries
- **In Scope**: Server-authoritative chat initialization (`POST /api/v1/chats/init`), deterministic ID generation, participant validation, Chat Entity/Room cache synchronization, Chat List observation, and Profile -> Chat navigation handoff.
- **Out of Scope**: Complete messaging pipeline (message sending, delivery/read receipts, FCM sync, attachments, E2EE/Double Ratchet).
