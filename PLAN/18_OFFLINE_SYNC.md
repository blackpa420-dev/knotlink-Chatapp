# Offline Synchronization

## 1. Offline Capability
- **Read Operations**: Served directly from local Room database cache.
- **Write Operations**: Queued in Room `PendingMessage` table when offline; automatically flushed and synced to Firestore when network connectivity is re-established via WorkManager / Coroutines.
