# Phase 9: Security, Privacy & Account Management — Implementation Plan

## 1. Objectives
Harden BitChat for production usage across 13 core security domains:
1. Granular User Privacy Controls (Profile photo, Online status, Last seen, Read receipts, Typing indicators)
2. Normalized User Blocking & Unblocking Engine (Firestore + Room Cache + Notification & Message Suppression)
3. Universal Abuse Reporting System (`USER`, `MESSAGE`, `GROUP`)
4. Multi-Device Session Management (Device registry, Session enumeration, Current device tracking)
5. Session Revocation & Remote Logout ("Logout Current Device", "Logout All Devices")
6. Secure Account Deletion Flow (Logical mark, Session purge, FCM detachment, Local data wipe)
7. Phone/OTP Account Recovery Guarantees (Authoritative UID mapping preservation)
8. Hardened Firestore Security Rules (Blocked users, Devices, Reports, User isolation)
9. Sensitive Data & Safe Logging Enforcement (Sanitized logs, Keystore/Encrypted data protection)
10. Multi-Device RTDB Presence & Typing Privacy
11. FCM Device Token Lifecycle & Cleanup
12. Account Switching Zero-Leakage Guarantee (Full database purge between auth states)
13. Room Database Schema v11 Migration (Non-destructive addition of `blocked_users` and `privacy_settings` / `user_sessions`)

---

## 2. Technical Architecture & Data Model

### 2.1 Privacy Settings Model
```kotlin
data class UserPrivacySettings(
    val profilePhotoVisibility: String = "EVERYONE", // EVERYONE, CONTACTS, NOBODY
    val onlineStatusVisibility: String = "EVERYONE", // EVERYONE, CONTACTS, NOBODY
    val lastSeenVisibility: String = "EVERYONE", // EVERYONE, CONTACTS, NOBODY
    val whoCanMessageMe: String = "EVERYONE", // EVERYONE, CONTACTS, NOBODY
    val readReceiptsEnabled: Boolean = true,
    val typingIndicatorEnabled: Boolean = true
)
```

### 2.2 Blocked User Model
- Firestore: `/users/{uid}/blocked/{targetUid}`
- Room Entity:
```kotlin
@Entity(tableName = "blocked_users", primaryKeys = ["targetUid"])
data class BlockedUserEntity(
    val targetUid: String,
    val username: String,
    val displayName: String,
    val blockedAt: Long = System.currentTimeMillis()
)
```

### 2.3 Abuse Report Model
- Firestore: `/reports/{reportId}`
```kotlin
data class AbuseReport(
    val reportId: String,
    val reporterUid: String,
    val targetType: String, // USER, MESSAGE, GROUP
    val targetId: String,
    val reason: String,
    val details: String?,
    val status: String = "PENDING",
    val createdAt: Long = System.currentTimeMillis()
)
```

### 2.4 User Device Session Model
- Firestore: `/users/{uid}/devices/{deviceId}` & `/user_devices/{deviceId}`
- Room Entity:
```kotlin
@Entity(tableName = "user_sessions", primaryKeys = ["deviceId"])
data class UserSessionEntity(
    val deviceId: String,
    val deviceName: String,
    val platform: String = "Android",
    val appVersion: String = "1.0",
    val lastActiveTimestamp: Long = System.currentTimeMillis(),
    val isCurrentDevice: Boolean = false,
    val isActive: Boolean = true
)
```

---

## 3. UI/UX Plan & Complete Feature Matrix
1. **Privacy Settings Screen / Dialog**:
   - Radio selectors for Profile Photo, Online Status, Last Seen, Direct Messaging (`Everyone`, `My Contacts`, `Nobody`).
   - Switches for Read Receipts and Typing Indicators.
2. **Blocked Users Management Screen / Dialog**:
   - List blocked contacts with instant Unblock action.
   - Block User confirmation from Chat and Profile menus.
3. **Active Devices / Sessions Management Screen / Dialog**:
   - List active devices with device icon, model name, OS, version, last active timestamp, and "Current Device" badge.
   - "Terminate Session" for individual devices + "Log Out of All Other Devices" button.
4. **Universal Abuse Report Modal**:
   - Reason dropdown / chips (Spam, Harassment, Inappropriate Content, Impersonation, Other), details field, submit with sanitized privacy protection.
5. **Account Deletion Modal**:
   - Multi-step warning, explicit "DELETE" confirmation, session purge, local wipe, navigation back to Welcome screen.

---

## 4. Verification & Testing Strategy
- Unit tests (`SecurityPrivacySessionTest.kt`) validating privacy checks, block enforcement, session revocation, and report submission.
- Verification of non-destructive Room v10 -> v11 migration.
- Regression testing across all Phase 1-8 features.
