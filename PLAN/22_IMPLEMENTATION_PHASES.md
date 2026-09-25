# 10-Phase Implementation Roadmap

1. **Phase 1**: Authentication + OTP + Identity Foundation (BulkSMSBD proxy, phone normalization, session tokens).
2. **Phase 2**: Registration + Profile + Username (Atomic account creation, username uniqueness index).
3. **Phase 3**: Firebase Database + Room Cache + Sync Foundation (Firestore schema, Room offline cache entities).
4. **Phase 4**: Search + User Discovery + Chat Creation (Deterministic chat ID, participant validation).
5. **Phase 5**: Messaging + Message Persistence + Inbox (Message pipeline, idempotency, inbox sync).
6. **Phase 6**: Realtime Presence + Typing + Delivery/Read States (RTDB presence hooks, typing indicators).
7. **Phase 7**: FCM Notifications + Background Synchronization (Cloud Functions trigger, local notification dispatch).
8. **Phase 8**: Media + Attachments + Firebase Storage (Image/video/voice upload, download, and caching).
9. **Phase 9**: Security + Privacy + Multi-Device + Offline Robustness (Rate limiting, security rules, offline queue).
10. **Phase 10**: Production Hardening + Testing + Performance + Release (Robolectric tests, performance tuning, final audit).
