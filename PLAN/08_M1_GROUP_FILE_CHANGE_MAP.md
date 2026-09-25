# Phase 8 Milestone 1: Group Chat Foundation - File Change Map

## Files to Create
1. `/PLAN/08_M1_GROUP_PRE_IMPLEMENTATION_AUDIT.md` (Created)
2. `/PLAN/08_M1_GROUP_IMPLEMENTATION_PLAN.md` (Created)
3. `/PLAN/08_M1_GROUP_FILE_CHANGE_MAP.md` (Created)
4. `/app/src/main/java/com/example/ui/screens/CreateGroupScreen.kt` (To be created)
5. `/app/src/main/java/com/example/ui/screens/GroupInfoScreen.kt` (To be created)
6. `/PLAN/08_M1_GROUP_IMPLEMENTATION_REPORT.md` (To be created upon completion)
7. `/PLAN/08_M1_GROUP_VERIFICATION_REPORT.md` (To be created upon completion)

## Files to Modify
1. `app/src/main/java/com/example/data/local/Entities.kt`: Add group type fields, ownerUid, permissions, and group member entity.
2. `app/src/main/java/com/example/data/local/BitChatDao.kt`: Add DAOs for group members and group chat queries.
3. `app/src/main/java/com/example/data/repository/BitChatRepository.kt`: Implement group creation, member management, and ownership transfer.
4. `app/src/main/java/com/example/ui/viewmodel/BitChatViewModel.kt`: Expose group actions and state.
5. `app/src/main/java/com/example/ui/screens/ChatsScreen.kt`: Add "New Group" button and group navigation.
6. `app/src/main/java/com/example/navigation/BitChatNavigation.kt`: Add navigation routes for group creation and group info.
7. `/PLAN/CHANGELOG.md`: Record Milestone 1 progress.
