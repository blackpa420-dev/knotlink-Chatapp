# BitChat: Release Blocker Audit

## 1. Executive Summary & Incident Scope

During real device testing of the BitChat Release Candidate, four critical production blockers were identified:
1. **BulkSMSBD OTP is NOT being used**: The app hardcodes a client-side OTP bypass (`123456`) instead of routing OTP authentication through a secure server-side BulkSMSBD integration.
2. **QR scan resolves to the WRONG user identity**: QR code resolution uses client-side hardcoded user mapping dictionaries and falls back to synthesized mock identities instead of resolving the authoritative user identity from Firestore/Backend.
3. **User search returns no results**: The search interface only filters in-memory local chats with a faulty suffix matching filter; it never queries the Firestore user directory (`users`, `usernames`) for registered BitChat users.
4. **Tapping "Message" after a QR-scanned user causes the app to close/crash**: Navigation to `chatDetail` fails and crashes the application because route arguments (e.g., chat names with spaces) are not URL-encoded, and `onDismiss()` concurrently tears down the bottom sheet while launching navigation.

---

## 2. Deep Root-Cause Analysis

### Blocker 1: BulkSMSBD OTP Bypass
- **Location**: `app/src/main/java/com/example/ui/viewmodel/BitChatViewModel.kt` (lines 626–648).
- **Defect**:
  ```kotlin
  // Accept 123456 as the fixed correct OTP
  if (code == "123456") {
      _isVerifyingOtp.value = false
      showToast("OTP Verification Successful!")
      handleOtpVerificationSuccess(onSuccess)
      return
  }
  ```
- **Root Cause**: A temporary test bypass was left in production code. The Android app must NEVER call the BulkSMSBD API directly (which would expose API credentials in client binaries). Instead, OTP requests and verifications must go through backend Cloud Functions or secure server-side endpoints. If the production BulkSMSBD server configuration/endpoint is missing or unprovisioned, the app MUST NOT silently accept `123456` or fake OTPs; it must explicitly fail with `BLOCKED — BULKSMSBD PRODUCTION CONFIGURATION MISSING`.

---

### Blocker 2: QR Scan Resolves to Wrong User Identity
- **Location**: `app/src/main/java/com/example/ui/viewmodel/BitChatViewModel.kt` (lines 1154–1220).
- **Defect**:
  - `resolveScannedUser` uses a hardcoded `when` statement that maps scanned `publicId` values to static demo objects (`"BC-7F2K9M"` -> Aria Sterling, `"EVELYN"` -> Evelyn Vance, etc.).
  - The fallback `else` branch synthesizes a fake user profile with a generated title (`"BitChat Member ($publicId)"`) rather than querying the remote database.
  - When Device A scans Device B's QR code, Device A receives a hardcoded or synthesized profile instead of Device B's actual Firestore profile.
- **Root Cause**: Absence of a unified, authoritative user identity resolver that queries Firestore (`users`, `usernames`, and `phone_index`) to fetch the true user document.

---

### Blocker 3: User Search Returns No Results
- **Location**: `app/src/main/java/com/example/ui/screens/ChatsScreen.kt` (lines 1882–1898) & `BitChatViewModel.kt`.
- **Defect**:
  - `SearchOverlayScreen` computes `filteredResults` solely over `chats` (the user's existing local chat list).
  - It does not trigger remote user queries against registered BitChat users.
  - The filtering logic contains an invalid expression `matchesText || chatName.endsWith(".chat")`, which causes incorrect results when filtering by domain.
- **Root Cause**: The search architecture lacks a global user directory search flow that queries Firestore `users` and `usernames` by normalized query string (case-insensitive username and display name prefixes) and returns public user profiles that users can interact with and message.

---

### Blocker 4: QR → "Message" App Crash
- **Location**:
  - `app/src/main/java/com/example/navigation/BitChatNavigation.kt` (`BitChatRoutes.chatDetail`)
  - `app/src/main/java/com/example/ui/screens/ScannedUserProfileSheet.kt` (lines 250–255)
  - `app/src/main/java/com/example/ui/screens/QrScannerScreen.kt` (lines 327–333)
  - `app/src/main/java/com/example/ui/viewmodel/BitChatViewModel.kt` (`getOrCreateChatForScannedUser`)
- **Defect**:
  1. `BitChatRoutes.chatDetail(chatId, chatName)` creates a navigation route: `"chat_detail/$chatId/$chatName"`. When `chatName` contains spaces or special characters (e.g. `"Aria Sterling"`, `"BitChat Member (BC-001)"`), Navigation Compose fails route pattern matching with an unhandled URI/navigation exception, crashing the entire Android process.
  2. `ScannedUserProfileSheet.kt` triggers `onDismiss()` synchronously inside the button `onClick` handler at the same instant `onMessageClick()` is invoked, causing race conditions in sheet teardown and navigation dispatch.
  3. `getOrCreateChatForScannedUser` generated chat entities without ensuring valid, non-empty participant UIDs and without handling URL encoding when routing.
- **Root Cause**: Unencoded URL route parameters in Jetpack Compose Navigation, concurrent bottom sheet dismissal during navigation, and missing defensive data handling.

---

## 3. Blocker Matrix

| Blocker # | Issue Description | Severity | Impact | Required Remediation |
| :--- | :--- | :--- | :--- | :--- |
| **1** | Hardcoded OTP "123456" in `verifyFirebaseOtp` | Critical | Security / Compliance | Route OTP verification to backend; remove `123456` bypass; return clear error if server config missing. |
| **2** | QR scan resolves to wrong/hardcoded user identity | Critical | Functional / Data Integrity | Unify identity resolver in repository to query Firestore `users`/`usernames`/`phone_index` authoritatively. |
| **3** | User search returns empty for registered users | Critical | Functional / User Discovery | Implement global Firestore user search in `BitChatRepository`/`BitChatViewModel` and hook into `SearchOverlayScreen`. |
| **4** | Tapping "Message" after QR scan crashes the app | Critical | Stability / Fatal Crash | URL-encode all navigation route parameters, serialize sheet dismiss and navigation, ensure complete `participantUids`. |
