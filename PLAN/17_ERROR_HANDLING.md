# Error Handling & Resilience

## 1. Strategies
- **Network Failures**: Automatic fallback to Room local cache with visual offline banner.
- **OTP Delivery Failure**: Graceful error messaging with retry countdown timer.
- **Transaction Rollbacks**: Firestore atomic operations ensure partial writes never create corrupted user or chat records.
