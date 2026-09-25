# Firestore Database Schema

## 1. Collections & Documents

### `/users/{userId}`
- `uid`: String (Firebase Auth UID)
- `phoneNumber`: String (Normalized E.164 canonical format)
- `fullName`: String
- `username`: String (Lowercase unique handle)
- `profilePhotoUrl`: String
- `bio`: String
- `createdAt`: Timestamp
- `updatedAt`: Timestamp
- `fcmToken`: String

### `/usernames/{username}`
- `uid`: String (Owner user ID)
- `createdAt`: Timestamp

### `/chats/{chatId}`
- `chatId`: String (Deterministic compound hash of sorted participant UIDs)
- `participantIds`: Array<String>
- `participantPhones`: Array<String>
- `createdAt`: Timestamp
- `lastMessageText`: String
- `lastMessageTimestamp`: Timestamp
- `lastMessageSenderId`: String
- `updatedAt`: Timestamp

### `/chats/{chatId}/messages/{messageId}`
- `messageId`: String (UUID v4 or server generated)
- `senderId`: String
- `text`: String
- `timestamp`: Timestamp
- `status`: String (`PENDING`, `SENT`, `DELIVERED`, `READ`)
- `attachments`: Array<Map> (url, type, size, mimeType)

### `/contacts/{userId}/userContacts/{contactId}`
- `contactUserId`: String
- `phoneNumber`: String
- `savedName`: String
- `addedAt`: Timestamp
