# Phase 8 Milestone 2: Advanced Messaging Features - File Change Map

## Files to Create
1. `/PLAN/08_M2_ADVANCED_MESSAGING_PRE_IMPLEMENTATION_AUDIT.md` (Created)
2. `/PLAN/08_M2_ADVANCED_MESSAGING_IMPLEMENTATION_PLAN.md` (Created)
3. `/PLAN/08_M2_ADVANCED_MESSAGING_FILE_CHANGE_MAP.md` (Created)
4. `/PLAN/08_M2_ADVANCED_MESSAGING_IMPLEMENTATION_REPORT.md` (To be created upon completion)
5. `/PLAN/08_M2_ADVANCED_MESSAGING_VERIFICATION_REPORT.md` (To be created upon completion)
6. `/firestore.rules` (Authoritative security rules covering chat, member, reaction, message edit/delete/pin rules)

## Files to Modify
1. `app/src/main/java/com/example/data/local/Entities.kt`: Extend `MessageEntity`, add `ReactionEntity`, `PinnedMessageEntity`.
2. `app/src/main/java/com/example/data/local/BitChatDao.kt`: Add DAOs for reactions, pins, edit/delete queries, and message search.
3. `app/src/main/java/com/example/data/local/BitChatDatabase.kt`: Register new entities and bump Room version to 9.
4. `app/src/main/java/com/example/data/repository/BitChatRepository.kt`: Implement reply, forward, reaction, edit, delete, pin, mention resolution, and system events.
5. `app/src/main/java/com/example/ui/viewmodel/BitChatViewModel.kt`: Expose methods and reactive flows for Milestone 2 features.
6. `app/src/main/java/com/example/ui/screens/ChatDetailScreen.kt`: Implement long-press action menu, reaction picker, reaction chips, reply preview bar, quote bubble, forward dialog, in-composer edit mode, deleted placeholders, pinned banner/sheet, mention auto-complete, and system event pills.
7. `/PLAN/CHANGELOG.md`: Record Milestone 2 progress and completion.
