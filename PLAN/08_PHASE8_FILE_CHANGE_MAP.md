# Phase 8 File Change Map

## Files to Create
1. `/PLAN/08_PHASE8_PRE_IMPLEMENTATION_AUDIT.md` (Created)
2. `/PLAN/08_PHASE8_IMPLEMENTATION_PLAN.md` (Created)
3. `/PLAN/08_PHASE8_FILE_CHANGE_MAP.md` (Created)
4. `/PLAN/08_PHASE8_IMPLEMENTATION_REPORT.md` (To be created upon completion)
5. `/PLAN/08_PHASE8_VERIFICATION_REPORT.md` (To be created upon completion)

## Files to Modify
1. `app/src/main/java/com/example/data/local/Entities.kt`: Add group type, roles, permissions, reply reference, edit/delete state, reaction fields, and pinned message state.
2. `app/src/main/java/com/example/data/repository/BitChatRepository.kt`: Implement group chat creation, member administration, reaction management, edit/delete/pin message actions, and system event handling.
3. `app/src/main/java/com/example/ui/viewmodel/BitChatViewModel.kt`: Expose group and advanced messaging actions.
4. `app/src/main/java/com/example/ui/screens/ChatDetailScreen.kt` & associated UI screens: Add group management views, reply preview, message action menus, reaction picker, pinned message banner, and system event bubbles.
5. `/PLAN/CHANGELOG.md`: Record Phase 8 progress and completion.
