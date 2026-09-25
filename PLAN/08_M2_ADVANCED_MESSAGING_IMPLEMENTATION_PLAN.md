# Phase 8 Milestone 2: Advanced Messaging Features - Implementation Plan

## 1. Architectural Strategy & Modules

### Step 1: Data Model & Room Schema Extensions
- Extend `MessageEntity` in `Entities.kt`:
  - `replyToMessageId: String?`
  - `isEdited: Boolean`
  - `editedAt: Long?`
  - `isDeletedForEveryone: Boolean`
  - `deletedAt: Long?`
  - `isDeletedForMe: Boolean`
  - `isPinned: Boolean`
  - `pinnedAt: Long?`
  - `messageType: String` ("TEXT", "MEDIA", "SYSTEM_EVENT")
  - `systemEventType: String?`
  - `forwardedFromMessageId: String?`
  - `reactionsSummary: String?` (JSON/serialized summary for quick local caching)
  - `mentionedUids: String?`
- Create `ReactionEntity` in Room for caching reaction list per message (`messageId`, `chatId`, `uid`, `emoji`, `createdAt`).
- Create `PinnedMessageEntity` in Room (`chatId`, `messageId`, `pinnedByUid`, `pinnedAt`).
- Increment Room Database version cleanly.

### Step 2: Firestore & Repository Operations (`BitChatRepository.kt`)
- **Replies**: Reference `replyToMessageId` in message document without duplicating entire original text.
- **Forwards**: Create authentic new message in target chat(s) with sender = current user.
- **Reactions**: Read/write `/chats/{chatId}/messages/{messageId}/reactions/{uid}`.
- **Edits**: Authoritative check that `request.auth.uid == resource.data.senderUid`. Update `text`, `isEdited = true`, `editedAt = serverTimestamp()`.
- **Deletions**:
  - `DELETE_FOR_ME`: Mark locally in Room as `isDeletedForMe = true`.
  - `DELETE_FOR_EVERYONE`: Authoritative check (sender or owner/admin). Set `isDeletedForEveryone = true`, `deletedAt = serverTimestamp()`.
- **Pins**: Add/remove `/chats/{chatId}/pinned/{messageId}` and update message `isPinned` state.
- **Mentions**: Resolve `@username` to member UIDs in group chat.
- **System Events**: Insert dedicated message type `"SYSTEM_EVENT"` with `systemEventType`.

### Step 3: ViewModel Layer (`BitChatViewModel.kt`)
- Expose state and coroutines for:
  - Replying to a message (sets active reply draft in state).
  - Forwarding messages to selected chats.
  - Adding/removing reactions on a message.
  - Editing message text.
  - Deleting message (for me vs for everyone).
  - Pinning/unpinning message & observing pinned messages list.
  - Mention member query suggestions based on `@` input in group.
  - Dispatching system events.

### Step 4: UI & M3 Interaction Enhancements (`ChatDetailScreen.kt`)
- Message long-press contextual sheet / popup (Reply, Forward, React bar with 6 quick emojis + picker, Edit [if own], Delete, Pin/Unpin).
- Reply preview banner docked above message composer with close/cancel button.
- Reply quote bubble inside message item (tap scrolls to referenced original message).
- Reaction chips row displayed under message bubble (with count and active user reaction highlight).
- Forward target selection modal sheet with chat list and multi-select.
- In-composer Edit mode banner showing "Editing message" with cancel button.
- Deleted message placeholder banner: "🚫 This message was deleted".
- Pinned message sticky bar below top app bar with click to jump, plus full Pinned Messages sheet.
- Group mention autocomplete dropdown above composer.
- System event pill rendering (centered, translucent neutral styling).

### Step 5: Verification & Safety
- Full build and compilation check (`compile_applet`).
- Verification matrix for all 9 messaging operations.
- Security verification and regression test against Phases 1–7.
