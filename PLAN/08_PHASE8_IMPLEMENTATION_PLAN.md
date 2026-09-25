# Phase 8 Implementation Plan: Groups & Advanced Messaging

## 1. Implementation Steps

### Step 1: Data Model & Room Schema Extensions
- Extend `ChatEntity` and `MessageEntity` (or add related Room entities) to support group metadata (`type = "GROUP"`, `ownerUid`, `permissions`), replies, edited state, deleted state, pinned message references, and reactions.

### Step 2: Repository & Firestore Sync (`BitChatRepository.kt`)
- Implement group creation flows (`createGroupChat`), member management (add/remove members, promote/demote admin), leave group with ownership transfer, and advanced message operations (reactions, edits, deletions, pins, replies, forwards, mentions).

### Step 3: ViewModel & State Management (`BitChatViewModel.kt`)
- Expose group management actions, reaction handlers, message edit/delete/pin methods, and group member lists.

### Step 4: UI Enhancements (`ChatDetailScreen.kt`, new Group creation/info screens)
- Add group creation UI, group info sheet/screen, member management controls, reply preview bar, reaction picker, message action menu (edit, delete, pin, reply, forward), mention suggestions, and system event message renderers.
- Maintain the BitChat M3 visual language, typography, and dark/light styling.

### Step 5: Verification & Testing
- Verify group creation, role permissions, advanced messaging features, multi-device sync, FCM notifications, and regression of Phases 1–7.
