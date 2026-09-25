# Phase 8 Milestone 1: Group Chat Foundation - Implementation Plan

## 1. Implementation Steps

### Step 1: Room & Entity Schema Updates
- Extend `ChatEntity` and `MessageEntity` in `Entities.kt` to support `chatType` (`DIRECT` vs `GROUP`), `ownerUid`, and group permission flags. Add `GroupMemberEntity` if needed for local caching of group membership and roles.

### Step 2: Repository & Firestore Sync (`BitChatRepository.kt`)
- Implement `createGroupChat(title, description, avatarUrl, memberUids)` creating server-authoritative group documents and member subcollections.
- Implement group loading, membership validation, and owner transfer flows.

### Step 3: ViewModel & State Management (`BitChatViewModel.kt`)
- Expose group creation state, member selector state, group settings, and group chat flows.

### Step 4: UI Enhancements (`ChatsScreen.kt`, `ChatDetailScreen.kt`, new Group Creation & Info screens)
- Add "New Group" action in chat list / action menu.
- Build Group Creation screen: group name input, avatar picker (using Phase 7 media upload), member search and selection (using Phase 3 discovery), and create button.
- Build Group Info screen: view group members, roles, and manage group settings.

### Step 5: Verification & Testing
- Verify group creation, member validation, owner transfer, secure avatar storage, Room persistence, multi-device sync, and regression across Phases 1–7.
