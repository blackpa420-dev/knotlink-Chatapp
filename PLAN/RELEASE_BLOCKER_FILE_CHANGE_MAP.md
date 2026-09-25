# BitChat: Release Blocker File Change Map

## 1. Affected Files Overview

| File Path | Description of Required Changes |
| :--- | :--- |
| `app/src/main/java/com/example/navigation/BitChatNavigation.kt` | Update `BitChatRoutes` methods with `Uri.encode` and `Uri.decode` for all route parameters to prevent crash on names with spaces. |
| `app/src/main/java/com/example/data/repository/BitChatRepository.kt` | Implement `findUserByPublicIdentity` and `searchGlobalUsers` querying Firestore `users`, `usernames`, and `phone_index`. |
| `app/src/main/java/com/example/ui/viewmodel/BitChatViewModel.kt` | Remove hardcoded `123456` OTP check; implement server-side verification and fallback `BLOCKED — BULKSMSBD PRODUCTION CONFIGURATION MISSING`; connect unified identity resolver and search flows; harden `getOrCreateChatForScannedUser`. |
| `app/src/main/java/com/example/ui/screens/QrScannerScreen.kt` | Cleanly sequence bottom sheet dismissal and navigation callback when tapping "Message"; add defensive error handling. |
| `app/src/main/java/com/example/ui/screens/ScannedUserProfileSheet.kt` | Fix "Message" click handler to prevent race conditions during sheet dismissal. |
| `app/src/main/java/com/example/ui/screens/ChatsScreen.kt` | Hook `SearchOverlayScreen` into global user search, displaying registered network users alongside existing chats. |
| `PLAN/RELEASE_BLOCKER_FIX_IMPLEMENTATION_REPORT.md` | Create comprehensive post-implementation verification report. |
| `PLAN/RELEASE_BLOCKER_FIX_VERIFICATION_REPORT.md` | Create verification document detailing resolution of all 4 blockers. |
| `PLAN/FINAL_RELEASE_GATE_REPORT.md` | Update status matrix with blocker remediation results. |
| `PLAN/FINAL_PRODUCTION_STATUS.md` | Update final production status. |
| `PLAN/CHANGELOG.md` | Log release blocker fixes. |

---

## 2. Surgical Change Detail by Module

### 1. Navigation (`BitChatNavigation.kt`)
- Target: `BitChatRoutes.chatDetail`, `BitChatRoutes.videoCall`, `BitChatRoutes.audioCall`, `BitChatRoutes.groupInfo`.
- Action: Encode all parameters using `android.net.Uri.encode(param)`.
- Target: `NavHost` composable route arguments for `CHAT_DETAIL`, `GROUP_INFO`, etc.
- Action: Decode parameters using `android.net.Uri.decode(...)`.

### 2. Repository Layer (`BitChatRepository.kt`)
- Target: Identity & Discovery APIs.
- Action: Add `findUserByPublicIdentity(identifier: String): PublicUserProfile?` and `searchGlobalUsers(query: String): List<PublicUserProfile>`.
- Action: Add `verifyBackendOtp(phoneNumber: String, otpCode: String): Result<Boolean>` checking backend availability.

### 3. ViewModel Layer (`BitChatViewModel.kt`)
- Target: `verifyFirebaseOtp(onSuccess, onError)`.
- Action: Remove `if (code == "123456")`. If backend configuration is unavailable, return error string: `"BLOCKED — BULKSMSBD PRODUCTION CONFIGURATION MISSING"`.
- Target: `resolveScannedUser(rawQrPayload)`.
- Action: Call `repository.findUserByPublicIdentity` to resolve actual Firestore user.
- Target: Global Search State.
- Action: Add `searchGlobalUsers(query)` triggering repository search and updating `_globalSearchResults`.
- Target: `getOrCreateChatForScannedUser(scannedUser)`.
- Action: Properly populate `participantUids`, `avatarType`, and ensure non-null chat state.

### 4. UI Layer (`ChatsScreen.kt`, `QrScannerScreen.kt`, `ScannedUserProfileSheet.kt`)
- Target: `SearchOverlayScreen` in `ChatsScreen.kt`.
- Action: Add global user search results list and click-to-chat integration.
- Target: `ScannedUserProfileSheet.kt`.
- Action: Clean button click handling.
- Target: `QrScannerScreen.kt`.
- Action: Safe navigation trigger without crashing.
