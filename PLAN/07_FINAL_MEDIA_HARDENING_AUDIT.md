# Phase 7 Final Media Hardening Audit

## 1. Objective
Audit and establish production-grade security, authorization, integrity, duplicate prevention, orphan cleanup, and lifecycle management for Firebase Storage media attachments in BitChat.

## 2. Current State vs. Hardened Requirements

| Requirement | Current State | Hardened Specification |
| :--- | :--- | :--- |
| **Storage Security Rules** | Missing (`storage.rules` absent). Direct public access or unconstrained paths. | Deploy authoritative `storage.rules` verifying authentication, chat membership, max size, and MIME type. |
| **Ownership & Authorization** | Client directly uploads to arbitrary `/chats/{chatId}/{mediaId}` without chat participant validation. | Authoritative upload authorization flow (or rule enforcement + participant verification) ensuring User A cannot upload to User B's chat. |
| **Identifiers & Duplicate Prevention** | Uses random `mediaId` UUIDs without deduplication. | Introduce `clientUploadId` and cryptographic checksums to prevent duplicate attachment creation upon retry. |
| **Integrity & Checksum** | None. | Compute SHA-256 checksum on upload to verify file integrity and detect corruption or duplicates. |
| **Orphan Cleanup** | None. | Implement scheduled cleanup or client/server reference reconciliation for abandoned uploads. |
| **Download Authorization** | Public URLs via Firebase download tokens. | Enforce Firestore chat membership and attachment reference validation before media access. |
| **Lifecycle States** | Implicitly created upon success (`READY`). | Explicit lifecycle management: `WAITING` → `AUTHORIZED` → `UPLOADING` → `PROCESSING` → `READY` (or `FAILED`, `CANCELLED`, `DELETED`). |

## 3. Security & Architecture Plan Summary
1. **Firebase Storage Rules (`storage.rules`)**:
   - Require `request.auth != null`.
   - Validate file size (`request.resource.size < 25 * 1024 * 1024`).
   - Validate MIME types (`image/*`, `video/*`, `audio/*`, `application/pdf`, etc.).
   - Enforce path restriction and participant membership where applicable via Firestore existence checks in rules or secured backend generation.
2. **Repository & ViewModel Hardening (`BitChatRepository.kt`, `BitChatViewModel.kt`)**:
   - Generate `clientUploadId` (UUID) and compute SHA-256 checksum.
   - Enforce client-side MIME allowlist and size limits (10MB for images, 25MB for videos/documents).
   - Implement upload lifecycle states (`WAITING`, `UPLOADING`, `PROCESSING`, `READY`, `FAILED`, `CANCELLED`).
   - Idempotent attachment submission keyed by `clientUploadId` and checksum.
3. **Room & Firestore Sync**:
   - Persist attachment metadata with `attachmentId`, `clientUploadId`, checksum, lifecycle state, and progress.
   - Ensure multi-device synchronization via Firestore message documents and Room reactive flows.
