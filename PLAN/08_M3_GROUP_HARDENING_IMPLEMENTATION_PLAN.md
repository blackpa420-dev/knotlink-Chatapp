# Phase 8 Milestone 3: Group Moderation, Permissions & Final Group Hardening — Implementation Plan

## 1. Plan Overview & Objectives
Phase 8 Milestone 3 delivers a complete group moderation, authorization, role hierarchy, permission controls, and security hardening layer for BitChat.

---

## 2. Implementation Steps

### Step 1: Data Model & Schema Enhancement
1. Update `ChatEntity` in `Entities.kt` to include:
   - `isMuted: Boolean = false`
2. Update `BitChatDao.kt` with queries:
   - `updateChatMuteStatus(chatId: String, isMuted: Boolean)`
   - `updateGroupRole(chatId: String, uid: String, newRole: String)`
   - `removeMemberFromChat(chatId: String, uid: String)`
   - `updateGroupInfo(chatId: String, name: String, description: String?, avatarUrl: String?, permissions: String?)`
   - `updateChatOwner(chatId: String, newOwnerUid: String)`
3. Increment `BitChatDatabase` schema version to 10.

### Step 2: Repository Authorization & Moderation Engine (`BitChatRepository.kt`)
Implement the authoritative methods:
1. `addMemberToGroup(chatId: String, newUid: String, callerUid: String)`:
   - Verifies caller permission (Owner/Admin or policy).
   - Verifies target UID isn't already a member.
   - Sets member doc in Firestore, updates `participantUids`, inserts to Room, emits `MEMBER_ADDED` system event.
2. `removeMemberFromGroup(chatId: String, targetUid: String, callerUid: String)`:
   - Verifies caller permission: Owner can remove Admin/Member; Admin can remove Member; Member cannot remove.
   - Deletes member doc, updates `participantUids`, cleans Room member row, emits `MEMBER_REMOVED` system event.
3. `promoteAdmin(chatId: String, targetUid: String, callerUid: String)`:
   - Verifies caller is OWNER.
   - Updates target member role to `ADMIN` in Firestore & Room, emits `ADMIN_PROMOTED` system event.
4. `demoteAdmin(chatId: String, targetUid: String, callerUid: String)`:
   - Verifies caller is OWNER.
   - Updates target member role to `MEMBER` in Firestore & Room, emits `ADMIN_DEMOTED` system event.
5. `transferOwnership(chatId: String, newOwnerUid: String, callerUid: String)`:
   - Verifies caller is OWNER.
   - Performs atomic write: sets `ownerUid = newOwnerUid` on chat doc, updates old owner role to `ADMIN`, updates new owner role to `OWNER`.
   - Emits `OWNER_TRANSFERRED` system event.
6. `updateGroupSettings(chatId: String, title: String, description: String?, avatarUrl: String?, permissionsJson: String?, callerUid: String)`:
   - Verifies caller is OWNER or ADMIN.
   - Updates Firestore chat document & Room `ChatEntity`.
   - Emits `GROUP_INFO_UPDATED` or `PERMISSION_UPDATED` system event.
7. `toggleMuteGroup(chatId: String, isMuted: Boolean)`:
   - Updates local Room `ChatEntity.isMuted` and user preferences without polluting shared group state.
8. `reportMemberOrGroup(chatId: String, targetUid: String?, reason: String, details: String?)`:
   - Writes record to Firestore `reports` collection with timestamp, reporterUid, target, reason, and status `PENDING`.

### Step 3: ViewModel Layer (`BitChatViewModel.kt`)
Expose all moderation, settings, role updates, and mute controls with coroutines and state flows.

### Step 4: UI / UX Hardening in `GroupInfoScreen.kt` & `ChatDetailScreen.kt`
1. In `GroupInfoScreen.kt`:
   - **Role Badges**: Display distinct colored chips (`👑 Owner` gold/amber, `⭐ Admin` blue, `Member` neutral).
   - **Member Context Actions**: Clicking on a member card displays authorized actions based on current user role (Promote to Admin, Demote to Member, Transfer Ownership, Remove Member, Report Member).
   - **Add Member Dialog / Selector**: Allows searching/selecting from contacts or user search to add new members.
   - **Group Settings / Permissions Editor**: Owner/Admin sheet to toggle `onlyAdminsCanMessage`, `onlyAdminsCanEditInfo`, `onlyAdminsCanPin`.
   - **Mute Group Switch**: Dedicated settings row with instant toggle.
   - **Report Group / Member Dialog**: Modal dialog with structured reason picker (Spam, Harassment, Inappropriate, Other).
2. In `ChatDetailScreen.kt`:
   - Check if current user is restricted by `onlyAdminsCanMessage`. If restricted, show disabled placeholder: "Only admins can send messages in this group".

### Step 5: Unit & Robolectric Tests
Create comprehensive tests in `GroupModerationTest.kt` validating:
- Role checks and permission hierarchies.
- Atomic owner transfer logic.
- Admin creation and demotion constraints.
- Member removal boundaries.
- Group mute toggle state.
- System event generation contracts.
