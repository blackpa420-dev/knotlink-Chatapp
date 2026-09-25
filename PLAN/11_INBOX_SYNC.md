# Inbox & Chat List Synchronization

## 1. Sync Strategy
- **Targeted Subscriptions**: The app only queries and listens to chat documents where `participantIds` contains the current user's UID (`array-contains`).
- **No Global Listeners**: Never listens to the entire `/chats` collection.
- **Room Sync**: Realtime chat metadata updates are mirrored instantly into the local Room database to power the high-performance inbox UI list.
