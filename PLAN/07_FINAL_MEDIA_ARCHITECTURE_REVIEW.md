# Phase 7 Final Media Architecture Review

## 1. Executive Summary
An architectural audit of the Phase 7 Media & Attachments implementation was conducted against the security contract (`/PLAN/00_SECURITY_CONTRACT.md`), media storage plan (`/PLAN/14_MEDIA_STORAGE.md`), and the user review prompt.

While the client-side media upload (`FirebaseStorage.putFile()`) successfully fulfills functional requirements (image, video, voice note, and document uploads with progress tracking and download URL retrieval), **security rules, backend authorization, duplicate protection, and orphan cleanup are currently absent or incomplete**.

---

## 2. Detailed Verification Answers

1. **Can User A upload to User B's chat path?**
   - *Status*: **Yes (Vulnerability)**. Without deployed Firebase Storage security rules verifying chat participant IDs, any authenticated user who knows or guesses a `chatId` can write to `/chats/{chatId}/`.
2. **Can User A upload to an arbitrary chatId?**
   - *Status*: **Yes**. No validation ensures the user is a participant of the `chatId` prior to uploading.
3. **Can an authenticated user upload arbitrary MIME types?**
   - *Status*: **Partial**. The client checks `mimeType.isBlank()`, but without server-side Storage rules (`request.resource.contentType`), users can upload arbitrary files with spoofed MIME extensions.
4. **Can an authenticated user spoof another mediaId?**
   - *Status*: **Yes**. `mediaId` is generated locally via `UUID.randomUUID().toString()`, but a malicious client could craft paths.
5. **Can a user access another user's media download URL?**
   - *Status*: **Depends on Firestore message access**. If a user obtains a download URL (or guesses the storage path), Firebase Storage URLs are publicly accessible via token unless restricted by Storage rules.
6. **Can a user bypass the size restriction?**
   - *Status*: **Yes**. No client-side or server-side max file size validation is currently enforced.
7. **Can a user upload to `/chats/{chatId}/{mediaId}` without being a participant?**
   - *Status*: **Yes**, due to the absence of Firebase Storage security rules in the repository.
8. **Are Storage Rules actually enforcing ownership and chat membership?**
   - *Status*: **No**. No `storage.rules` file exists in the codebase.
9. **Is the backend involved in metadata authorization?**
   - *Status*: **No**. Uploads are initiated directly from the Android client (`BitChatRepository.uploadMedia`) without a backend proxy or Cloud Function metadata pre-authorization.
10. **Is the current architecture consistent with `/PLAN/14_MEDIA_STORAGE.md`?**
    - *Status*: **Yes**, it matches the client-side upload workflow described in the plan, but lacks the necessary security enforcement rules.

---

## 3. Advanced Features Audit
- **Upload checksum**: Not implemented.
- **Duplicate upload protection**: Not implemented.
- **attachmentId / clientUploadId**: Not implemented (standard mediaId UUID used).
- **Orphan upload cleanup**: Not implemented.
- **Secure download authorization**: Not implemented (dependent on Storage security rules).

---

## 4. Remediation Migration Plan (Required for Production Hardening)
1. **Deploy Firebase Storage Security Rules**:
   Add `storage.rules` enforcing that `request.auth != null` and the user is a participant in the associated chat (validated via Firestore read in rules or request structure).
2. **Client-Side Size & Type Validation**:
   Enforce max file size limits (e.g., 25MB for videos/documents, 10MB for images) and strict MIME type allowlists in `BitChatRepository`.
3. **Orphan Cleanup**:
   Implement a scheduled Cloud Function to delete storage objects that do not have a corresponding message reference in Firestore after 24 hours.
