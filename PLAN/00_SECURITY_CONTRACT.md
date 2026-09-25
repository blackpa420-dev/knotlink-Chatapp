# Security Contract & Access Rules

## 1. Firestore Security Rules Principles
- **Users**: Users can only modify their own user document (`request.auth.uid == userId`).
- **Usernames**: Username reservation is strictly controlled via Cloud Functions / atomic transactions.
- **Chats & Messages**: Read and write access is restricted exclusively to authenticated users whose UID is present in the `participantIds` array of the chat document.
- **Storage**: Media files under `/chats/{chatId}/` are readable only by verified chat participants.
- **BulkSMSBD**: Credentials reside exclusively on the server side. No API keys or secrets exist within the Android APK.
