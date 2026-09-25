# Phase 8 Milestone 1: Group Chat Foundation - Pre-Implementation Audit

## 1. Executive Summary
Milestone 1 establishes the foundational infrastructure for Group Chats in BitChat:
- Supporting `DIRECT` and `GROUP` chat types in Room and Firestore.
- Server-authoritative group creation with member selection, owner assignment (`OWNER`), and role-based permissions (`OWNER`, `ADMIN`, `MEMBER`).
- Group avatar upload utilizing the Phase 7 secure Firebase Storage pipeline.
- Group listing and chat opening without modifying existing DIRECT chat workflows.

## 2. Scope & Boundaries (Milestone 1)
- **Included**: Group creation, member selection (using Phase 3 search), group avatar, owner/admin/member roles, membership management, owner transfer policy, permissions foundation, Room schema extensions for groups, multi-device group sync, and security rule enforcement.
- **Excluded**: Reply, forward, reactions, message edit/delete, pin, mentions, E2EE, channels, stories, calls (reserved for subsequent milestones).

## 3. Data Model & Architecture
- **Chat Document (`/chats/{chatId}`)**: `type = "GROUP"`, `title`, `description`, `avatarUrl`, `ownerUid`, `participantUids`, `createdAt`, `updatedAt`, `permissions`.
- **Group Members Subcollection (`/chats/{chatId}/members/{uid}`)**: `uid`, `role` (`OWNER`, `ADMIN`, `MEMBER`), `joinedAt`, `status`.
- **Maximum Group Size**: Bounded at 256 participants for performance and Firestore document size safety.
