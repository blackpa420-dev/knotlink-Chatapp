# Phase 3 Pre-Implementation Audit: User Search & Discovery

## 1. Current Search & Discovery Implementation
- **UI**: Currently local chat/contact filtering exists in `ChatsScreen`, `ContactsScreen`, and `ChatDetailScreen`. There is no dedicated global user search/discovery screen connecting to authoritative backend username indexes (`usernames/{normalizedUsername}`).
- **Repository**: `BitChatRepository` has local contact lists and mock users, but lacks production backend search querying (`GET /api/v1/users/search?q=...`).
- **ViewModel**: `BitChatViewModel` handles local `_searchQuery` state and mock simulation, needing connection to authoritative remote search.

## 2. Security & Privacy Requirements
- Search query must be normalized case-insensitively.
- Only public profile fields (`uid`, `username`, `displayName`, `photoUrl`, `bio`) must be returned.
- Phone numbers, emails, and private settings must NEVER be exposed via search.
- Rate limiting and query validation (min 2 characters, max 32 characters) must be enforced.

## 3. Architecture & Navigation Handoff
- Search results must resolve to authoritative UIDs identical to Phase 1 auth and Phase 2 profile restoration.
- Tapping a search result opens the public profile preview and prepares the identity handoff for chat creation (Phase 4).
