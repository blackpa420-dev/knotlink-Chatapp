# Messaging Pipeline

## 1. Message Lifecycle

```mermaid
sequenceDiagram
    participant Sender
    participant RoomCache as Local Room Cache
    participant Firestore as Firestore DB
    participant Receiver

    Sender->>RoomCache: 1. Compose & Save (Status: PENDING, clientId)
    RoomCache-->>Sender: Update UI Immediately
    Sender->>Firestore: 2. Write Message Document (/chats/{id}/messages/{msgId})
    Firestore-->>Sender: 3. Acknowledge Write (Status: SENT)
    Sender->>RoomCache: Update Status to SENT
    
    Firestore->>Receiver: 4. Realtime Snapshot / FCM Trigger
    Receiver->>RoomCache: Save Incoming Message (Status: DELIVERED)
    Receiver->>Firestore: Update Status to READ (when viewed)
```

- **Retry Policy**: Exponential backoff retry mechanism for failed pending messages stored in Room.
- **Duplicate Prevention**: Client-generated `clientId` acts as an idempotent key.
