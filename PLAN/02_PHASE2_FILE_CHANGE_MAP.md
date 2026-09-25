# Phase 2 File Change Map

## Files to Create
- `/PLAN/02_PHASE2_PRE_IMPLEMENTATION_AUDIT.md` (Created)
- `/PLAN/02_PHASE2_IMPLEMENTATION_PLAN.md` (Created)
- `/PLAN/02_PHASE2_FILE_CHANGE_MAP.md` (Created)
- `/PLAN/02_PHASE2_VERIFICATION.md` (Pending implementation)
- `/PLAN/02_PHASE2_COMPLETE.md` (Pending implementation)

## Files to Modify (Authoritative Android Code)
- `app/src/main/java/com/example/data/repository/BitChatRepository.kt`: Add profile sync, username check, and registration completion calls.
- `app/src/main/java/com/example/ui/viewmodel/BitChatViewModel.kt`: Connect UI registration and profile state to backend/repository functions.
- `app/src/main/java/com/example/ui/screens/RegisterIdentityScreen.kt`: Wire username check, avatar upload, and complete registration actions.
- `app/src/main/java/com/example/ui/screens/ProfileSettingsScreen.kt`: Wire profile updates and avatar synchronization.

## Files that must NOT Change
- Phase 1 authentication flow (Phone input format `01XXXXXXXXX`, BulkSMSBD proxy, OTP verification logic).
- Existing navigation routes and non-auth UI elements.
- Room database core setup (preserving existing tables and migration safety).
