# User Profile Management

## 1. Profile Data Ownership & Lifecycle
- **Creation**: Created atomically during the final step of registration.
- **Modification**: Only editable by the authenticated owner via Cloud Firestore rules (`request.auth.uid == resource.data.uid`).
- **Read Access**: Readable by authenticated users participating in shared chats or via contact discovery.
- **Caching**: Cached locally in Room `UserEntity` to ensure instant loading when opening chat details or profiles.
