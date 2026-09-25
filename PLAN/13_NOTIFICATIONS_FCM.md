# Notifications & FCM Architecture

## 1. Push Notification Pipeline
- **Foreground**: Handled seamlessly by active Firestore snapshots/RTDB listeners.
- **Background / Killed**: Cloud Functions intercept new message creation in Firestore and trigger FCM via Firebase Admin SDK.
- **Payload**: Contains sender name, chat ID, and message snippet. The Android FCM service receives the payload, updates the local Room cache, and displays a high-priority system notification.
