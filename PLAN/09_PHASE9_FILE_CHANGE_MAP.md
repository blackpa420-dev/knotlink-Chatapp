# Phase 9: Security, Privacy & Account Management — File Change Map

## 1. Local Database & Schema Files
- `/app/src/main/java/com/example/data/local/Entities.kt`
  - Add `BlockedUserEntity` (`targetUid`, `username`, `displayName`, `blockedAt`)
  - Add `UserSessionEntity` (`deviceId`, `deviceName`, `platform`, `appVersion`, `lastActiveTimestamp`, `isCurrentDevice`, `isActive`)
  - Add `privacySettingsJson` to `UserIdentityEntity` or default fields
- `/app/src/main/java/com/example/data/local/BitChatDao.kt`
  - Add queries for `blocked_users` (`getAllBlockedUsers`, `insertBlockedUser`, `deleteBlockedUser`, `clearBlockedUsers`)
  - Add queries for `user_sessions` (`getAllUserSessions`, `insertUserSessions`, `deleteUserSession`, `clearUserSessions`)
- `/app/src/main/java/com/example/data/local/BitChatDatabase.kt`
  - Increment Room database version from `10` to `11`
  - Add `BlockedUserEntity::class` and `UserSessionEntity::class` to `@Database` entities

---

## 2. Repository & Backend Integration Files
- `/app/src/main/java/com/example/data/repository/BitChatRepository.kt`
  - Add `UserPrivacySettings` data class and methods (`updatePrivacySettings`, `getPrivacySettings`)
  - Add `blockUser(targetUid, username, displayName)` and `unblockUser(targetUid)` syncing with Firestore `/users/{uid}/blocked/{targetUid}` and Room
  - Add `observeBlockedUsers()` Flow
  - Add `submitAbuseReport(targetType, targetId, reason, details)` syncing with Firestore `/reports/{reportId}`
  - Add `registerDeviceSession(deviceName, appVersion)` and `observeUserSessions()` Flow
  - Add `revokeDeviceSession(deviceId)` and `revokeAllOtherSessions()`
  - Add `deleteAccount(reason)` performing full logical deactivation, session revocation, and `clearAllLocalData()`
  - Add block checks before sending direct messages or acknowledging receipts
  - Add sanitized logging to prevent accidental leaks of sensitive tokens or credentials
- `/firestore.rules`
  - Add rules for `/users/{uid}/blocked/{targetUid}` (user-isolated read/write)
  - Add rules for `/users/{uid}/devices/{deviceId}` and `/user_devices/{deviceId}`
  - Add rules for `/reports/{reportId}` (create allowed for authenticated users, read restricted to moderation/backend)

---

## 3. Presentation & UI Layer Files
- `/app/src/main/java/com/example/ui/viewmodel/BitChatViewModel.kt`
  - Add StateFlows and methods for `userPrivacySettings`, `blockedUsersList`, `userSessionsList`
  - Add ViewModel functions: `updatePrivacySettings`, `blockUser`, `unblockUser`, `reportContent`, `revokeDeviceSession`, `revokeAllOtherSessions`, `deleteAccount`
- `/app/src/main/java/com/example/ui/screens/SettingsScreen.kt`
  - Enhance `AdvanceSettingsPage` and `BasicSettingsPage` with interactive sheets/modals:
    - **Privacy Settings Sheet**: Profile photo, Online status, Last seen, Read receipts, Typing indicators
    - **Blocked Users Sheet**: Live Room-synced blocked contacts list with instant unblock and search
    - **Active Devices / Sessions Sheet**: Live device sessions, model names, app version, last active time, terminate session and logout all
    - **Account Deletion Dialog**: Multi-step confirmation, warning modal, and delete execution
    - **Abuse Report Dialog**: Reusable report submission modal for users, messages, and groups
- `/app/src/main/java/com/example/ui/screens/ChatDetailScreen.kt` & `/app/src/main/java/com/example/ui/screens/GroupInfoScreen.kt`
  - Connect "Block Contact" and "Report" actions to the unified repository/viewmodel handlers

---

## 4. Test Suite Files
- `/app/src/test/java/com/example/SecurityPrivacySessionTest.kt`
  - Unit tests for privacy settings serialization/updates
  - Unit tests for block/unblock enforcement and message prevention
  - Unit tests for abuse report validation and submission
  - Unit tests for multi-device session management and remote revocation
  - Unit tests for account deletion and data wipe lifecycle
