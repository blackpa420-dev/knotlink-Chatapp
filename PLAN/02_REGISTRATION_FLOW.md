# User Registration Flow

## 1. Step-by-Step Registration Lifecycle

```mermaid
sequenceDiagram
    participant User
    participant App
    participant CloudFunction as Secure Backend
    participant BulkSMSBD as BulkSMSBD API
    participant Firestore as Firestore DB

    User->>App: Enter Phone Number (+880...)
    App->>CloudFunction: Request OTP(phoneNumber)
    CloudFunction->>BulkSMSBD: Send SMS API Request
    BulkSMSBD-->>CloudFunction: Delivery Success
    CloudFunction-->>App: OTP Sent Successfully (200 OK)
    
    User->>App: Enter 6-digit OTP
    App->>CloudFunction: Verify OTP(phoneNumber, otpCode)
    CloudFunction-->>App: Verified (Returns Temporary Registration Token)
    
    Note over App,User: USER IS VERIFIED BUT NO PERMANENT ACCOUNT EXISTS YET

    User->>App: Enter Profile Info (Name, Username, Photo)
    App->>CloudFunction: Check Username Availability(username)
    CloudFunction-->>App: Available
    
    User->>App: Tap "Create Account"
    App->>CloudFunction: Complete Registration(token, profileData)
    CloudFunction->>Firestore: 1. Mint Auth UID<br/>2. Create User Document<br/>3. Claim Username Index<br/>4. Store FCM Token
    Firestore-->>CloudFunction: Transaction Success
    CloudFunction-->>App: Permanent Session Established + Custom Auth Token
    App->>App: Initialize Local Room Cache & Start Sync
    App-->>User: Navigate to Home Screen
```

## 2. Abort / Abandonment Policy
- If the user closes the app or abandons registration after OTP verification:
  - **No Firestore User document** is created.
  - **No Firebase Auth UID** is created.
  - The temporary registration token expires automatically after 15 minutes.
  - Re-opening the app requires restarting verification from phone entry.
