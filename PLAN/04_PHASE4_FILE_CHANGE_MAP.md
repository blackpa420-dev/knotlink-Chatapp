# Phase 4 File Change Map

## Files to Create
- `/PLAN/04_PHASE4_PRE_IMPLEMENTATION_AUDIT.md` (Created)
- `/PLAN/04_PHASE4_IMPLEMENTATION_PLAN.md` (Created)
- `/PLAN/04_PHASE4_FILE_CHANGE_MAP.md` (Created)
- `/PLAN/04_PHASE4_IMPLEMENTATION_REPORT.md` (Pending implementation)
- `/PLAN/04_PHASE4_VERIFICATION_REPORT.md` (Pending implementation)

## Files to Modify (Authoritative Android Code)
- `app/src/main/java/com/example/data/repository/BitChatRepository.kt`: Add `initChat(targetUid)` for authoritative chat creation/retrieval.
- `app/src/main/java/com/example/ui/viewmodel/BitChatViewModel.kt`: Add `startChatWithUser()` and chat observation flows.
- `app/src/main/java/com/example/ui/screens/ScannedUserProfileSheet.kt` or profile preview screen: Wire "Start Chat" button to initialize chat and navigate to `ChatDetailScreen`.

## Files that must NOT Change
- Phase 1 authentication flow (`01XXXXXXXXX`, OTP verification, Custom Token).
- Phase 2 authoritative profile setup, username claim, and Room persistence.
- Phase 3 username search and public profile discovery.
