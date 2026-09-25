# Phase 8 Pre-Implementation Audit: Groups & Advanced Messaging

## 1. Executive Summary
Phase 8 introduces robust group chat creation, membership administration, role permissions, advanced messaging features (replies, forwards, reactions, message editing, deletion, pinned messages, mentions, and system events), and secure group notification management while maintaining full backward compatibility with existing 1-to-1 chats and security contracts established in Phases 1–7.

## 2. Existing Architecture Analysis
- **Chats & Messages**: Stored in Firestore under `/chats/{chatId}` and `/chats/{chatId}/messages/{msgId}`, with local caching in Room (`ChatEntity`, `MessageEntity`).
- **Media & Security**: Firebase Storage rules, SHA-256 checksums, and client upload deduplication are fully operational.
- **Identities**: Users authenticate via Google Sign-In or phone OTP, establishing verified UIDs and usernames.

## 3. Phase 8 Requirements Checklist
1. **Group Creation**: Choose name, avatar, members; server-authoritative creation and participant validation.
2. **Member Management & Roles**: OWNER, ADMIN, MEMBER roles with strict authorization rules.
3. **Leave Group & Ownership Transfer**: Secure leave handling and deterministic owner transfer if owner leaves.
4. **Group Permissions**: Configurable permissions (`sendMessages`, `sendMedia`, `addMembers`, `editGroupInfo`, `pinMessages`).
5. **Advanced Messaging**:
   - Reply (`replyToMessageId`, preview quote)
   - Forward (re-targeting message with new sender metadata)
   - Reactions (normalized emoji reactions per message/user)
   - Edit Message (sender authorization, `isEdited`, `updatedAt`)
   - Delete Message (`DELETE_FOR_ME`, `DELETE_FOR_EVERYONE`)
   - Pinned Messages (pin/unpin reference list)
   - Mentions (`@username` normalization and member verification)
   - System Events (group creation, member join/leave, role changes, title/avatar updates)
6. **FCM & Room**: Group notifications, muted group handling, Room schema extensions without destructive migrations.
