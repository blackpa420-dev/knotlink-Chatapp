# Phase 6 Milestone 5 Implementation Plan: Presence, Typing Indicators & Multi-Device Realtime Consistency

## 1. Objectives
- Implement real-time online/offline presence and last-seen tracking via Firebase Realtime Database (`status/{uid}/{deviceId}` and aggregated `status/{uid}`).
- Implement debounced ephemeral typing indicators (`typing/{chatId}/{uid}`).
- Support multi-device session handling (device-specific presence nodes so disconnecting one device does not mark the user offline if another device remains connected).
- Integrate Flow-based observers in repository and view model for targeted chat participants only (no global listeners).
- Update `ChatDetailScreen` header and message input area to display online status, last seen, and typing indicators ("typing...").

## 2. Step-by-Step Execution Plan
1. **Presence Repository & Manager**:
   - Create `PresenceRepository` or methods in `BitChatRepository` utilizing Firebase Realtime Database.
   - Set up `.info/connected` hook and `onDisconnect()` for session-aware presence (`status/{uid}/{deviceId}`).
2. **Typing Indicator Manager**:
   - Implement `setTyping(chatId, isTyping)` and `observeTyping(chatId)` for ephemeral typing state (`typing/{chatId}/{uid}`).
3. **ViewModel Integration**:
   - Expose presence and typing flows for active chat partners in `BitChatViewModel`.
4. **UI Integration (`ChatDetailScreen`)**:
   - Display online/last-seen status in chat header.
   - Display "typing..." indicator when partner is typing.
   - Trigger typing state changes on text input changes with debounce/clear.
5. **Testing & Verification**:
   - Test online/offline transitions, multi-device presence, typing timeout/clear on send, and regression of Phases 1–5 & Milestones 1–4.
