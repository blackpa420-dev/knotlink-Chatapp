# Phase 3 Implementation Plan: User Search & Discovery

## 1. Objectives
- Implement production-grade backend and repository support for user search and discovery by username.
- Ensure case-insensitive normalization (`zabir`, `Zabir`, `ZABIR` -> `zabir`).
- Prevent leakage of sensitive data (phone numbers, emails, UIDs).
- Build the User Search screen and public profile preview with debounced search queries and safe local Room caching.
- Prepare identity handoff for Phase 4 chat creation.

## 2. Step-by-Step Execution Plan
1. **Repository & API Integration**: Add `searchUsers(query: String)` in `BitChatRepository` querying authoritative backend/Cloud Functions and Firestore username indexes.
2. **ViewModel Extension**: Add debounced search state management in `BitChatViewModel` with loading, results, empty, and error states.
3. **Search UI**: Create or enhance the Search screen with a clean search bar, debounced input, and user result cards showing avatar, display name, and username.
4. **Public Profile Preview**: Build a safe public profile viewer showing public bio, display name, username, and a "Start Chat" handoff action.
5. **Testing & Verification**: Verify exact username search, case insensitivity, no-results handling, privacy enforcement, and rate limiting.
