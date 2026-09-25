# Phase 8 Milestone 2: Advanced Messaging Features - Pre-Implementation Audit

## 1. Executive Summary
Milestone 2 advances BitChat's messaging architecture to support enterprise-grade, real-time messaging interactions across both DIRECT and GROUP conversations. This audit evaluates the data model, Room schema, Firestore structures, UI components, and security authorization boundaries required for:
1. Message Replies (`replyToMessageId`, quote preview, scroll-to-original)
2. Message Forwarding (creating new messages with authentic sender identity)
3. Normalized Message Reactions (per-user emoji reactions subcollection)
4. Message Editing (sender-authoritative editing, `isEdited`, `editedAt`)
5. Message Deletion (`DELETE_FOR_ME` vs `DELETE_FOR_EVERYONE` state flags)
6. Message Pinning (pinned references collection/state, banner & pinned list)
7. Group Mentions (`@username` autocomplete, member UID resolution, visual styling)
8. Group System Events (`GROUP_CREATED`, `MEMBER_JOINED`, `MEMBER_LEFT`, `MEMBER_REMOVED`, `ADMIN_PROMOTED`, `ADMIN_DEMOTED`, `GROUP_TITLE_CHANGED`, `GROUP_AVATAR_CHANGED`, `MESSAGE_PINNED`)

## 2. Server-Authoritative Group Milestone 1 Verification
Milestone 1 group management was audited to ensure full server-authoritativeness:
- **Group Creation**: Generates Firestore document under `/chats/{chatId}` with server timestamp and participant list.
- **Membership**: Stored in subcollection `/chats/{chatId}/members/{uid}` with `role` (`OWNER`, `ADMIN`, `MEMBER`).
- **Owner UID & Transfer**: Enforced on the document `ownerUid` and member document roles; leaving owner deterministically transfers ownership to an existing participant.

## 3. Scope & Boundaries (Milestone 2)
- **Included**: Replies, forwards, reactions, edits, deletions, pins, mentions, system events, and complete M3 UI states.
- **Excluded**: E2EE (Signal Protocol), voice/video calls, stories, channels, disappearing timers (scheduled for future milestones).
