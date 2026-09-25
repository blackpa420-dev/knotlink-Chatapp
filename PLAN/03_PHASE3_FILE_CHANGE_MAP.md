# Phase 3 File Change Map

## Files to Create
- `/PLAN/03_PHASE3_PRE_IMPLEMENTATION_AUDIT.md` (Created)
- `/PLAN/03_PHASE3_IMPLEMENTATION_PLAN.md` (Created)
- `/PLAN/03_PHASE3_FILE_CHANGE_MAP.md` (Created)
- `/PLAN/03_PHASE3_IMPLEMENTATION_REPORT.md` (Pending implementation)
- `/PLAN/03_PHASE3_VERIFICATION_REPORT.md` (Pending implementation)

## Files to Modify (Authoritative Android Code)
- `app/src/main/java/com/example/data/repository/BitChatRepository.kt`: Add backend search query method.
- `app/src/main/java/com/example/ui/viewmodel/BitChatViewModel.kt`: Add debounced search state flows and public profile selection logic.
- `app/src/main/java/com/example/ui/screens/ContactsScreen.kt` or Search screen component: Connect global username search and public profile preview sheet.

## Files that must NOT Change
- Phase 1 authentication flow (Phone input format `01XXXXXXXXX`, BulkSMSBD proxy, OTP verification logic).
- Phase 2 authoritative profile setup, username claim, and custom token sign-in.
- Room database core setup (preserving existing tables and migration safety).
