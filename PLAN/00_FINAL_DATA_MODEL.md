# Final Data Model & Firestore Schema (Authoritative Source of Truth)

## 1. `/phone_index/{canonicalPhone}`
- `uid`: String (Permanent user ID)
- `createdAt`: Timestamp
- **Purpose**: Maps normalized E.164 phone numbers to unique permanent user accounts to prevent duplicate creation.
- **Access**: Server-side Cloud Functions creation only; read-restricted.

## 2. `/users/{uid}`
- `uid`: String (Required)
- `phoneNumber`: String (Required, E.164)
- `fullName`: String (Required)
- `username`: String (Required, lowercase unique)
- `profilePhotoUrl`: String (Optional)
- `bio`: String (Optional)
- `createdAt`: Timestamp (Required)
- `updatedAt`: Timestamp (Required)
- `fcmToken`: String (Optional)
- **Access**: Owner write, authenticated read.

## 3. `/usernames/{username}`
- `uid`: String (Required)
- `createdAt`: Timestamp (Required)
- **Purpose**: Global uniqueness index for username claiming.

## 4. `/chats/{chatId}`
- `chatId`: String (Deterministic compound hash)
- `participantIds`: Array<String>
- `lastMessageText`: String
- `lastMessageTimestamp`: Timestamp
- `updatedAt`: Timestamp

## 5. `/chats/{chatId}/messages/{messageId}`
- `messageId`: String (Server ID or client UUID)
- `senderId`: String
- `text`: String
- `timestamp`: Timestamp
- `status`: String (`PENDING`, `SENT`, `DELIVERED`, `READ`)
- `attachments`: Array<Map>

---

## Room Cache Data Flows
- **Authoritative Source**: Cloud Firestore / Firebase Backend.
- **Room Role**: Local cache + offline queue only. Room never blindly overwrites newer server data.
- **Read Flow**: `Server/Firebase -> Repository -> Room -> UI`
- **Write Flow**: `UI -> Repository -> Server/Firebase -> Confirmed Result -> Room Update -> UI`
- **Offline Write Flow**: `UI -> Repository -> Room (PENDING status) -> Network available -> Server -> Confirmation -> Room (SYNCED status)`
