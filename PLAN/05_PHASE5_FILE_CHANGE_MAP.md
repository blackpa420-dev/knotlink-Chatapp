# Phase 5 File Change Map

## Files to Create
- `/PLAN/05_PHASE5_PRE_IMPLEMENTATION_AUDIT.md` (Created)
- `/PLAN/05_PHASE5_IMPLEMENTATION_PLAN.md` (Created)
- `/PLAN/05_PHASE5_FILE_CHANGE_MAP.md` (Created)
- `/PLAN/05_PHASE5_IMPLEMENTATION_REPORT.md` (Pending implementation)
- `/PLAN/05_PHASE5_VERIFICATION_REPORT.md` (Pending implementation)

## Files to Modify (Authoritative Android Code)
- `app/src/main/java/com/example/data/repository/BitChatRepository.kt`: Add `sendMessage()` and message observation/pagination logic.
- `app/src/main/java/com/example/ui/viewmodel/BitChatViewModel.kt`: Add message sending state handlers, retry actions, and active chat message flows.
- `app/src/main/java/com/example/ui/screens/ChatDetailScreen.kt`: Connect message composer input, send action, optimistic UI states, and message list observation.

## Files that must NOT Change
- Phase 1 authentication flow.
- Phase 2 authoritative profile setup and restoration.
- Phase 3 username search.
- Phase 4 chat initialization (`initChat`).
