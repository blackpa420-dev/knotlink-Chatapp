# Login Flow

## 1. Existing User Sign-In Process

```mermaid
sequenceDiagram
    participant User
    participant App
    participant CloudFunction as Secure Backend
    participant Firestore as Firestore DB

    User->>App: Enter Registered Phone Number
    App->>CloudFunction: Request Login OTP(phoneNumber)
    CloudFunction-->>App: OTP Sent via BulkSMSBD
    
    User->>App: Enter OTP
    App->>CloudFunction: Verify Login OTP(phoneNumber, code)
    CloudFunction->>Firestore: Lookup Canonical Phone -> UID
    Firestore-->>CloudFunction: Found Existing UID & Profile
    CloudFunction-->>App: Returns Firebase Custom Auth Token + User Profile
    
    App->>App: Authenticate Firebase Auth Session
    App->>Firestore: Fetch Contacts, Chats, and Delta Messages
    App->>App: Populate Room Local Cache
    App-->>User: Navigate Directly to Home Screen
```

## 2. Edge Case: Unregistered Number Attempting Login
- If the phone number lookup yields no existing user record during a login attempt:
  - The backend returns a specific status code (`ACCOUNT_NOT_FOUND`).
  - The app displays: **"Account not found. Create a new account."**
  - The user is smoothly redirected to the registration flow without creating ghost records.
