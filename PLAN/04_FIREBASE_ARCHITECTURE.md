# Firebase Architecture & Responsibilities

## 1. Service Matrix

| Firebase Service | Purpose | Data Stored | Access Rules |
| :--- | :--- | :--- | :--- |
| **Firebase Auth** | Identity verification & session tokens | UID, Auth Providers, Custom Claims | Server-side / Client authenticated |
| **Cloud Firestore** | Permanent authoritative data storage | Users, Usernames, Chats, Messages, Contacts | Strict Security Rules based on participants |
| **Realtime Database (RTDB)** | Ephemeral high-frequency state | Presence (Online/Offline), Typing indicators | Read/Write restricted to authenticated users |
| **Firebase Storage** | Media attachments storage | Images, Voice notes, Videos, Documents | Authenticated write, owner/participant read |
| **Cloud Messaging (FCM)** | Push notifications | Payload metadata for background sync | Server-to-client token delivery |
| **Cloud Functions** | Secure backend logic & BulkSMSBD proxy | OTP generation, validation, account creation | Server-only execution |

---

## 2. Service Separation Rationale
- **Firestore** handles structured, queryable permanent records (messages, chats, profiles) with robust security rules.
- **RTDB** handles ephemeral data (typing, presence) because Firestore write amplification and indexing costs are prohibitive for rapid, high-frequency state updates.
- **Cloud Functions** insulate the client from raw API keys (BulkSMSBD) and enforce atomic multi-document writes during registration.
