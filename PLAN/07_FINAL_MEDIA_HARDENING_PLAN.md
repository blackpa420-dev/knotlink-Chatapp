# Phase 7 Final Media Hardening Implementation Plan

## 1. Implementation Steps

### Step 1: Create Firebase Storage Security Rules (`storage.rules`)
- Establish authoritative security rules enforcing authenticated access, max file size limits (25MB), allowed MIME types (`image/*`, `video/*`, `audio/*`, documents), and path integrity under `/chats/{chatId}/{mediaId}`.

### Step 2: Update Data Models & Entities (`Entities.kt`, `BitChatDao.kt`)
- Add attachment metadata fields supporting `clientUploadId`, `attachmentId`, checksum (`sha256`), lifecycle state (`WAITING`, `UPLOADING`, `PROCESSING`, `READY`, `FAILED`), and upload progress.

### Step 3: Enhance `BitChatRepository.uploadMedia()` & Message Dispatch
- Implement checksum calculation (`SHA-256`) during file selection/upload prep.
- Implement `clientUploadId` generation and idempotency check.
- Enforce strict size limits and MIME type allowlisting on the client before upload.
- Implement upload lifecycle progression (`WAITING` → `UPLOADING` → `PROCESSING` → `READY` / `FAILED`).
- Attach metadata (checksum, `clientUploadId`, downloadUrl) to Firestore message payload.

### Step 4: UI & Progress State Integration (`ChatDetailScreen.kt`, `BitChatViewModel.kt`)
- Support real-time upload progress bars, retry buttons, cancellation controls, and failed upload indicators in the chat message list and input bar.
- Maintain existing BitChat visual language, dark/light theme, and typography.

### Step 5: Comprehensive Verification & Regression Testing
- Verify all 16 security and failure test cases (User A vs User B uploads, size limits, invalid MIME, duplicate `clientUploadId`, network interruption, app kill recovery, orphan cleanup simulation, deleted attachment access).
- Verify regression across Phases 1–6 (Authentication, profile, search, chat init, messaging, FCM/presence/read receipts).
