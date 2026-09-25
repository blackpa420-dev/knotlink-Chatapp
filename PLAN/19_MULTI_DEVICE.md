# Multi-Device Login & Session Management

## 1. Multi-Device Architecture
- Users can log in across multiple Android devices using the same canonical phone number and Firebase Auth session.
- **FCM Tokens**: Each device maintains its own FCM token registered in a sub-collection or array in Firestore to ensure notifications reach all active client instances.
- **Logout**: Revoking session clears local Room cache and invalidates device-specific FCM binding.
