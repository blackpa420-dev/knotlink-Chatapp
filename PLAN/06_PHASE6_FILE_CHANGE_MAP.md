# Phase 6 File Change Map

## Files to Create
- `/PLAN/06_PHASE6_PRE_IMPLEMENTATION_AUDIT.md` (Created)
- `/PLAN/06_PHASE6_IMPLEMENTATION_PLAN.md` (Created)
- `/PLAN/06_PHASE6_FILE_CHANGE_MAP.md` (Created)
- `/PLAN/06_PHASE6_IMPLEMENTATION_REPORT.md` (Pending implementation)
- `/PLAN/06_PHASE6_VERIFICATION_REPORT.md` (Pending implementation)
- `app/src/main/java/com/example/service/BitChatMessagingService.kt` (Pending implementation)
- `app/src/main/java/com/example/utils/PresenceManager.kt` (Pending implementation)

## Files to Modify (Authoritative Android Code)
- `app/src/main/AndroidManifest.xml`: Register `BitChatMessagingService`.
- `app/src/main/java/com/example/data/repository/BitChatRepository.kt`: Add token registration, delivery/read receipt APIs, and FCM deduplication.
- `app/src/main/java/com/example/ui/viewmodel/BitChatViewModel.kt`: Add presence, typing, and read receipt handlers.

## Files that must NOT Change
- Phase 1 authentication flow.
- Phase 2 authoritative profile setup.
- Phase 3 username search.
- Phase 4 chat initialization (`initChat`).
- Phase 5 core messaging architecture.
