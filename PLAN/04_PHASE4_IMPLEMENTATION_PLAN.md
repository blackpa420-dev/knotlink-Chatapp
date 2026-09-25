# Phase 4 Implementation Plan: Chat & Conversation Foundation

## 1. Objectives
- Establish server-authoritative 1:1 chat initialization (`POST /api/v1/chats/init` or repository equivalent).
- Enforce deterministic canonical chatId generation from sorted participant Uids.
- Prevent duplicate chat creation (race conditions handled via transactions/idempotency).
- Integrate profile-to-chat handoff: Search -> Public Profile -> Start Chat -> Chat Screen.
- Connect chat list observation and local Room cache synchronization.

## 2. Step-by-Step Execution Plan
1. **Repository & API Integration**: Add `initChat(targetUid: String): ChatEntity` in `BitChatRepository` to initialize or retrieve direct chats securely.
2. **ViewModel Extension**: Add `startChatWithUser(targetUid: String, onSuccess: (String) -> Unit)` in `BitChatViewModel` handling loading state and navigation handoff.
3. **UI Integration**: Connect "Start Chat" button on public profile / scanned user sheet to trigger chat initialization and navigate to `ChatDetailScreen` using the authoritative `chatId`.
4. **Room & State Synchronization**: Observe chats via Room database cache while keeping server/backend as the authoritative source.
5. **Testing & Verification**: Verify single chat document creation for A->B, B->A, simultaneous initialization, self-chat rejection, and unauthorized access blocking.
