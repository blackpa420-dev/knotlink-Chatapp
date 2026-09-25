# Phase 8 Milestone 3: Group Moderation, Permissions & Final Group Hardening — Pre-Implementation Audit

## 1. Executive Summary & Objective
This audit reviews the group chat foundation implemented in Milestone 1 and advanced messaging in Milestone 2, evaluating security, authorization rules, data modeling, synchronization contracts, and UI completeness for Milestone 3 (Group Moderation, Permissions & Final Group Hardening).

---

## 2. Review of Existing Codebase & Infrastructure

### 2.1 Data Models & Room Schema
- **`ChatEntity`**:
  - Contains `id`, `name`, `lastMessage`, `timeString`, `unreadCount`, `isOnline`, `category`, `avatarType`, `lastUpdated`, `participantUids`, `chatType` (`DIRECT` vs `GROUP`), `ownerUid`, `description`, `permissions`.
  - Missing field: `isMuted` (per-user/device mute preference for group notifications).
- **`GroupMemberEntity`**:
  - Primary keys: `[chatId, uid]`.
  - Fields: `chatId`, `uid`, `role` (`OWNER`, `ADMIN`, `MEMBER`), `joinedAt`.
- **`MessageEntity`**:
  - Supports `messageType` (`TEXT`, `MEDIA`, `SYSTEM_EVENT`), `systemEventType`, `mentionedUids`, `reactionsJson`, `replyToMessageId`, `isForwarded`, `isPinned`, `isEdited`, `isDeletedForEveryone`, `isDeletedForMe`.

### 2.2 Repository & Firestore Layer
- **Group Creation**:
  - `createGroupChat()` generates `chats/{chatId}` document with `type = "GROUP"`, `ownerUid`, and populates `chats/{chatId}/members/{uid}` with initial roles (`OWNER` for creator, `MEMBER` for participants).
- **Existing Gaps in Repository for Milestone 3**:
  1. No server-authoritative role change functions (`promoteAdmin`, `demoteAdmin`).
  2. No atomic ownership transfer function (`transferOwnership`) with previous owner demotion to `ADMIN` or `MEMBER`.
  3. No member removal function with authorization check (`removeMember`).
  4. No member addition function with duplicate/participant list synchronization (`addMemberToGroup`).
  5. No group settings update function (`updateGroupSettings`) with permissions JSON schema and atomic system event logging.
  6. No mute toggle persistence for individual chats (`toggleMuteChat`).
  7. No report member abuse dispatcher (`reportMember`).
  8. Missing server-authoritative authorization checks on `sendMessage` (checking if "only admins can send messages" or "only admins can send media").

### 2.3 UI / UX State & Screens
- **`GroupInfoScreen.kt`**:
  - Currently renders group banner, member count, member list with role labels, and a simple "Exit Group" button.
  - Missing UI:
    - Role management actions (Promote to Admin, Demote to Member, Transfer Ownership, Remove from Group).
    - Add Member action sheet/dialog.
    - Group Settings & Permissions editor (Admins only send messages, Admins only edit info, Admins only pin).
    - Mute notifications switch/tile.
    - Report group/member dialog with moderation categories.
    - Visual Role Badges (`Owner` in amber/gold, `Admin` in primary blue, `Member` in neutral).
- **`ChatDetailScreen.kt`**:
  - Needs to respect group permission restrictions (e.g., disable input bar if non-admin and `onlyAdminsCanSend = true`).

---

## 3. Authoritative Source of Truth Architecture
```
        Backend / Firestore (Authoritative)
                    ↓
             BitChatRepository
                    ↓
           Room Database (Cache/Local Sync)
                    ↓
             Jetpack Compose UI
```
- The server state is unconditionally authoritative. Local Room writes serve as responsive local cache.
- Multi-device events and snapshot updates reconcile to Room without overwriting newer remote timestamps.

---

## 4. Security & Permissions Matrix

| Permission Action | OWNER | ADMIN | MEMBER | Authorization Check |
|---|:---:|:---:|:---:|---|
| **Edit Group Info** | Yes | Yes (if enabled) | No (unless enabled) | Role in `[OWNER, ADMIN]` or `allowMemberEditInfo == true` |
| **Change Group Avatar** | Yes | Yes (if enabled) | No (unless enabled) | Role in `[OWNER, ADMIN]` or `allowMemberEditInfo == true` |
| **Add Members** | Yes | Yes | Optional | Role in `[OWNER, ADMIN]` or `allowMemberAdd == true` |
| **Remove Members** | Yes | Yes (MEMBER only) | No | Owner removes anyone; Admin removes only Members; Members remove none |
| **Promote to Admin** | Yes | No | No | Owner only |
| **Demote Admin** | Yes | No | No | Owner only |
| **Transfer Ownership** | Yes | No | No | Owner only; atomic transaction changing both owner records |
| **Pin Messages** | Yes | Yes | Optional | Role in `[OWNER, ADMIN]` or `allowMemberPin == true` |
| **Delete Others' Msgs** | Yes | Yes | No | Role in `[OWNER, ADMIN]` |
| **Send Messages** | Yes | Yes | Yes (if enabled)| Checked against `onlyAdminsCanMessage` setting |
| **Send Media** | Yes | Yes | Yes (if enabled)| Checked against `onlyAdminsCanSendMedia` setting |

---

## 5. Pre-Implementation Risk Assessment & Mitigations
1. **Destructive Room Migrations**: Room database will be updated from Version 9 to Version 10 adding `isMuted` to `ChatEntity`. All existing chats and messages remain fully preserved.
2. **Atomic Ownership Transfer**: Must update both the chat document `ownerUid` and both member documents (`oldOwner -> ADMIN`, `newOwner -> OWNER`) inside a single batch/transaction to prevent orphaned or dual-owner states.
3. **Impersonation in System Events**: All group system events will be emitted through validated repository methods using the authenticated caller's identity.
