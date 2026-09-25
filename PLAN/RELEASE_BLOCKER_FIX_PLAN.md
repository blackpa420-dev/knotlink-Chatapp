# BitChat: Release Blocker Fix Plan

## 1. Objectives & Architectural Mandates
- **Feature Freeze Compliance**: No new product features or structural redesigns.
- **Security Invariant**: Android client NEVER directly calls BulkSMSBD with client-side API secrets.
- **Fail-Safe Policy**: If server-side BulkSMSBD configuration is missing, do not bypass with mock OTPs; return explicit failure status: `BLOCKED — BULKSMSBD PRODUCTION CONFIGURATION MISSING`.
- **Unified Identity Resolution**: A single authoritative identity resolver function must power both QR scanning and global user search, querying Firestore `users`, `usernames`, and `phone_index`.
- **Zero-Crash Navigation**: All navigation routes taking string parameters (e.g. `chatName`, `chatId`, `contactId`) must be strictly URL-encoded (`Uri.encode` and `Uri.decode`).

---

## 2. Remediation Strategy by Blocker

### Fix 1: BulkSMSBD Server-Side Integration & Safe Failure Handling
1. Modify `BitChatViewModel.kt`:
   - Remove hardcoded `"123456"` OTP acceptance in `verifyFirebaseOtp`.
   - Implement real OTP verification dispatch through `BitChatRepository`.
   - When backend Cloud Function / BulkSMSBD server is unreachable or unconfigured, report `BLOCKED — BULKSMSBD PRODUCTION CONFIGURATION MISSING`.
   - Handle genuine Firebase Phone Auth credentials where available.

### Fix 2 & 3: Unified Authoritative Identity Resolver & Global Search
1. In `BitChatRepository.kt`:
   - Implement `findUserByPublicIdentity(identifier: String): PublicUserProfile?`:
     - Queries Firestore `usernames` collection by normalized username.
     - If not found, queries Firestore `users` collection by document ID (`uid`) or `publicId`.
     - If not found, checks local contacts and repository cache.
     - Strips internal secrets/tokens and returns an authoritative `PublicUserProfile`.
   - Implement `searchGlobalUsers(query: String): List<PublicUserProfile>`:
     - Normalizes query string (lowercase, trims `@`).
     - Queries Firestore `users` collection matching `username` and `displayName` prefixes.
     - Excludes blocked users and self.
2. In `BitChatViewModel.kt`:
   - Replace hardcoded `when` cases in `resolveScannedUser` with a call to `repository.findUserByPublicIdentity(publicId)`.
   - Expose `searchGlobalUsers(query: String)` with debounced `StateFlow` results.
   - Update `getOrCreateChatForScannedUser` to use the authoritative user data, ensuring correct `participantUids`, `avatarType`, and `chatId`.
3. In `ChatsScreen.kt` (`SearchOverlayScreen`):
   - Hook search input to `viewModel.searchGlobalUsers(query)`.
   - Display two clean, distinct sections: **Direct Chats & Messages** (matching existing conversations) and **Global BitChat Users** (matching registered users found on the network).
   - Tapping a discovered global user opens their conversation directly via `onSelectChat`.

### Fix 4: QR → Message Navigation & Crash Elimination
1. In `BitChatRoutes.kt` / `BitChatNavigation.kt`:
   - Wrap all route argument parameters with `android.net.Uri.encode(param)` in helper functions:
     `fun chatDetail(chatId: String, chatName: String) = "chat_detail/${android.net.Uri.encode(chatId)}/${android.net.Uri.encode(chatName)}"`
   - Decode arguments in NavHost composable destinations:
     `val chatName = android.net.Uri.decode(backStackEntry.arguments?.getString("chatName") ?: "")`
2. In `ScannedUserProfileSheet.kt`:
   - Do not dismiss the sheet immediately in `onClick`; pass the scanned user to `onMessageClick(scannedUser)` and dismiss cleanly or let the navigation transition take over.
3. In `QrScannerScreen.kt`:
   - Ensure the coroutine finishes `getOrCreateChatForScannedUser` before triggering `onChatCreated`.
   - Safely catch any navigation exceptions with defensive error handling.

---

## 3. Implementation Sequence & Order of Operations
1. Document audit and change map.
2. Update navigation routing with robust URI encoding.
3. Update `BitChatRepository` with authoritative identity lookup and global user search.
4. Update `BitChatViewModel` to connect identity resolver, search state, and OTP backend failure handling.
5. Update `ChatsScreen` (`SearchOverlayScreen`) and `QrScannerScreen` to use new unified flows.
6. Verify clean build with `compile_applet`.
7. Update test reports and changelog.
