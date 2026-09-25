# Room Database Cache Architecture

## 1. Entities & Design
- **UserEntity**: Caches user profiles locally.
- **ChatEntity**: Caches chat metadata and last message preview.
- **MessageEntity**: Caches message history with client ID, server ID, and sync status (`PENDING`, `SYNCED`, `FAILED`).
- **AttachmentEntity**: Caches media attachment metadata and local file paths.
- **SyncStateEntity**: Tracks pagination cursors and last sync timestamps.
- **Rule**: Room is strictly an offline cache. Server timestamps and IDs supersede local temporary values upon synchronization.
