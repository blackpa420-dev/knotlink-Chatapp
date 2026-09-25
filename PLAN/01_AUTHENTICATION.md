# Authentication Architecture

## 1. Core Authentication Components

BitChat implements a custom hybrid authentication architecture that uses **BulkSMSBD** for SMS OTP delivery while leveraging **Firebase Authentication** and **Firestore** for secure session management.

### State Machine & Transitions

```mermaid
stateDiagram-v2
    [*] --> IDLE
    IDLE --> OTP_REQUESTED: Enter Phone Number
    OTP_REQUESTED --> OTP_SENT: BulkSMSBD API Success
    OTP_SENT --> OTP_VERIFIED: User Enters Correct OTP
    
    OTP_VERIFIED --> CHECKING_ACCOUNT: Query Canonical Phone Mapping
    
    CHECKING_ACCOUNT --> EXISTING_USER: Phone Exists in Firestore
    CHECKING_ACCOUNT --> NEW_USER: Phone Not Found
    
    EXISTING_USER --> FIREBASE_AUTH_SIGNIN: Mint Custom Token / Session
    FIREBASE_AUTH_SIGNIN --> AUTHENTICATED: Sync Profile & Chats
    
    NEW_USER --> TEMPORARY_REGISTRATION_SESSION: Issue Verified Session Token
    TEMPORARY_REGISTRATION_SESSION --> PROFILE_SETUP: Enter Name & Username
    PROFILE_SETUP --> USERNAME_CHECK: Validate Uniqueness
    USERNAME_CHECK --> ACCOUNT_CREATION: Atomic Firestore + Auth Write
    ACCOUNT_CREATION --> AUTHENTICATED: Enter App
    
    ANY --> FAILURE_STATE: Error / Timeout / Rate Limit
```

---

## 2. Detailed Authentication States

- **IDLE**: User is on the phone input screen.
- **OTP_REQUESTED**: Client sends normalized phone number to secure backend proxy.
- **OTP_SENT**: Backend generates 6-digit OTP, stores temporary hash in Redis/Firestore with a 3-minute TTL, and calls BulkSMSBD.
- **OTP_VERIFIED**: User submits 6-digit code. Backend verifies hash and returns a short-lived temporary registration session JWT (for new users) or authenticates directly (for existing users).
- **ACCOUNT_CREATED**: Permanent Firebase Auth UID minted, Firestore user profile created, username index claimed atomically.
- **AUTHENTICATED**: Active user session established with valid refresh tokens and FCM bindings.
