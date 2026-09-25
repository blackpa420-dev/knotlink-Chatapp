# Security & Protection

## 1. Security Measures
- **BulkSMSBD Protection**: API credentials reside exclusively in secure server-side environment variables / Cloud Functions. No API keys are bundled inside the Android APK.
- **OTP Brute-Force Rate Limiting**: Redis/Firestore rate-limiting restricts OTP requests to 3 per phone number per 15 minutes. Incorrect OTP attempts are capped at 5 tries before lockout.
- **Firestore Security Rules**: Strict role-based and participant-based rules guarantee users can only read/write messages in chats where their UID is present in `participantIds`.
- **Temporary Registration TTL**: Uncompleted temporary registration sessions expire automatically after 15 minutes.
