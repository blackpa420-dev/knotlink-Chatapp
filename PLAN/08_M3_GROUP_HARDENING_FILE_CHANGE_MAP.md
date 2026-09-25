# Phase 8 Milestone 3: Group Moderation, Permissions & Final Group Hardening — File Change Map

## File Change Matrix

| File Path | Nature of Change | Description |
|---|---|---|
| `/app/src/main/java/com/example/data/local/Entities.kt` | Modify | Add `isMuted` to `ChatEntity`. |
| `/app/src/main/java/com/example/data/local/BitChatDao.kt` | Modify | Add DAO queries for group role update, member removal, group info update, mute toggle, and ownership update. |
| `/app/src/main/java/com/example/data/local/BitChatDatabase.kt` | Modify | Increment database schema version to 10. |
| `/app/src/main/java/com/example/data/repository/BitChatRepository.kt` | Modify | Implement group authorization, member moderation (add/remove), admin promote/demote, atomic owner transfer, group settings, mute controls, report abuse logging, and server system events. |
| `/app/src/main/java/com/example/ui/viewmodel/BitChatViewModel.kt` | Modify | Expose group moderation methods, mute toggle, role management, and group settings. |
| `/app/src/main/java/com/example/ui/screens/GroupInfoScreen.kt` | Modify | Add role badges, member action dialogs (promote, demote, remove, transfer ownership), add member sheet, permissions editor, mute toggle, and report dialog. |
| `/app/src/main/java/com/example/ui/screens/ChatDetailScreen.kt` | Modify | Integrate group permissions check (e.g. `onlyAdminsCanMessage`) to restrict input when configured. |
| `/app/src/test/java/com/example/GroupModerationTest.kt` | Create | Unit test suite verifying group roles, permissions matrix, owner transfer, member removal, and mute logic. |
| `/PLAN/08_M3_GROUP_HARDENING_IMPLEMENTATION_REPORT.md` | Create | Full implementation report for Milestone 3. |
| `/PLAN/08_M3_GROUP_HARDENING_VERIFICATION_REPORT.md` | Create | Full verification and test matrix report for Milestone 3. |
| `/PLAN/CHANGELOG.md` | Modify | Update with Milestone 3 entry. |
