# Phase 6 Milestone 4 File Change Map

## Files to Create
- `/PLAN/06_M4_READ_RECEIPTS_PRE_IMPLEMENTATION_AUDIT.md` (Created)
- `/PLAN/06_M4_READ_RECEIPTS_IMPLEMENTATION_PLAN.md` (Created)
- `/PLAN/06_M4_READ_RECEIPTS_FILE_CHANGE_MAP.md` (Created)
- `/PLAN/06_M4_READ_RECEIPTS_IMPLEMENTATION_REPORT.md` (Pending implementation)
- `/PLAN/06_M4_READ_RECEIPTS_VERIFICATION_REPORT.md` (Pending implementation)

## Files to Modify (Authoritative Android Code - Pending Implementation)
- `app/src/main/java/com/example/data/local/BitChatDao.kt`: Add batch update for read messages.
- `app/src/main/java/com/example/data/repository/BitChatRepository.kt`: Add `markMessagesAsRead()` with Firestore transaction and validation.
- `app/src/main/java/com/example/ui/viewmodel/BitChatViewModel.kt`: Add method to trigger read acknowledgements upon opening chat.
- `app/src/main/java/com/example/ui/screens/ChatDetailScreen.kt`: Trigger read acknowledgment on initial display/resume.

## Files that must NOT Change
- Authentication flow.
- Profile setup.
- Username search.
- Chat initialization (`initChat`).
- E2EE or media attachments (prohibited in Phase 6).
