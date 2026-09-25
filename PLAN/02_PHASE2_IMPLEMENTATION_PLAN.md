# Phase 2 Implementation Plan

## 1. Objectives
- Implement robust profile setup and username validation connecting the Phase 1 registration flow to authoritative Firestore documents and Firebase Auth.
- Enforce case-insensitive global username uniqueness via `usernames/{normalizedUsername}`.
- Implement atomic account creation (`phone_index`, `users`, `usernames`) during final registration completion.
- Enable secure profile photo upload to Firebase Storage (`users/{uid}/profile/avatar`).
- Implement existing user profile restoration upon sign-in with Firebase Custom Tokens (`signInWithCustomToken()`).

## 2. Step-by-Step Execution Phases
1. **Repository & API Gateway Expansion**:
   - Add methods in `BitChatRepository` and remote data source for username checking, profile updating, and registration completion (`/api/v1/users/register`, `/api/v1/usernames/check`).
2. **ViewModel & State Management**:
   - Update `BitChatViewModel` to handle username availability validation with debouncing, profile photo selection & upload, and atomic registration execution.
3. **UI Integration**:
   - Connect `RegisterIdentityScreen` and `ProfileSettingsScreen` to repository actions while preserving existing layouts, typography, and styling.
4. **Room Cache Synchronization**:
   - Sync authoritative server profile data into local Room `UserIdentityEntity` cache upon successful login or registration.
5. **Security Rules & Validation**:
   - Verify Firestore Security Rules and Storage rules enforce ownership and prevent unauthorized identity mutations.
