# Phase 7 Final Media Hardening File Map

## Files to Create
1. `/storage.rules`: Firebase Storage security rules for authoritative path access, size limits, and MIME type validation.

## Files to Modify
1. `app/src/main/java/com/example/data/local/Entities.kt`: Add attachment metadata fields (`clientUploadId`, `checksum`, `lifecycleState`, `progress`).
2. `app/src/main/java/com/example/data/repository/BitChatRepository.kt`: Add SHA-256 checksum calculation, `clientUploadId`, lifecycle states, size/MIME validation, and idempotent upload handling.
3. `app/src/main/java/com/example/ui/viewmodel/BitChatViewModel.kt`: Expose hardening states, retry/cancel handling, and progress flows.
4. `app/src/main/java/com/example/ui/screens/ChatDetailScreen.kt`: Display upload progress indicators, retry/cancel buttons, and failed upload states using existing BitChat styling.
5. `/PLAN/CHANGELOG.md`: Record hardening completion.
