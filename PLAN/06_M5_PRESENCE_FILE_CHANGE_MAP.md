# Phase 6 Milestone 5 File Change Map

## Files Created / Planned
- `/PLAN/06_M5_PRESENCE_PRE_IMPLEMENTATION_AUDIT.md` (Created)
- `/PLAN/06_M5_PRESENCE_IMPLEMENTATION_PLAN.md` (Created)
- `/PLAN/06_M5_PRESENCE_FILE_CHANGE_MAP.md` (Created)
- `/PLAN/06_M5_PRESENCE_IMPLEMENTATION_REPORT.md` (Pending implementation)
- `/PLAN/06_M5_PRESENCE_VERIFICATION_REPORT.md` (Pending implementation)

## Files to Modify (Authoritative Android Code - Pending Implementation)
- `app/src/main/java/com/example/data/repository/BitChatRepository.kt`: Add RTDB presence and typing management methods (`updatePresence`, `observePresence`, `setTyping`, `observeTyping`).
- `app/src/main/java/com/example/ui/viewmodel/BitChatViewModel.kt`: Expose presence and typing flows and actions.
- `app/src/main/java/com/example/ui/screens/ChatDetailScreen.kt`: Integrate presence header and typing indicator display + text change typing triggers.
- `app/src/main/java/com/example/MainActivity.kt` or Application lifecycle: Initialize presence tracking on login/foreground.

## Files that must NOT Change
- Authentication flow.
- Profile setup.
- Username search.
- Chat initialization (`initChat`).
- Message sending/receiving, FCM, and Read Receipts pipeline (Milestones 1–4).
