# Firebase Service Matrix

| Firebase Product | Purpose | Client Access | Backend Access | Permanent / Temporary | Realtime? |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **Firebase Auth** | Session management & custom token auth | Authenticated user sessions | Admin SDK full access | Permanent | No |
| **Cloud Firestore** | Authoritative data store (users, chats, messages, phone_index) | Read/Write via strict security rules | Admin SDK full access | Permanent | Yes (Snapshots) |
| **Realtime Database** | Ephemeral presence and typing indicators | Read/Write presence & typing nodes | Admin SDK full access | Ephemeral | Yes |
| **Firebase Storage** | Media attachments (images, voice, video) | Authenticated upload/read | Admin SDK full access | Permanent | No |
| **Cloud Messaging (FCM)** | Background push notifications | Receive push tokens / payloads | Server-to-client trigger | Ephemeral | No |
| **Cloud Functions (Option A)** | Secure BulkSMSBD proxy, OTP verification, & atomic registration | Callable HTTPS endpoints | Full system execution | Server-side logic | No |
