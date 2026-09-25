# BitChat Release Readiness Checklist

## 1. Build & Compilation
- [x] Debug build compiles cleanly with zero errors (`compile_applet` PASS)
- [x] Release build configuration defined with ProGuard rules (`app/proguard-rules.pro`)
- [x] Room SQLite schema migrations verified up to v11 (`MIGRATION_10_11`)
- [x] Unit test suites passing with 100% success rate

## 2. Security & Secrets
- [x] Zero hardcoded API keys or credentials in client repository
- [x] Server-side OTP isolation via Cloud Functions
- [x] Firestore security rules deployed and active
- [x] Storage rules enforcing 25MB limits and MIME whitelisting
- [x] FileProvider configured securely (`exported="false"`)
- [x] Two-step account deletion purging identity maps atomically

## 3. UI, UX & Accessibility
- [x] Material 3 design system with custom dark aesthetics
- [x] Consistent 48dp minimum touch targets across all interactive elements
- [x] Clear empty states for chats, contacts, blocked users, and device sessions
- [x] Smooth keyboard adjustment (`adjustResize`) and glassmorphic navigation

## 4. Performance & Reliability
- [x] Large message rendering tested up to 5,000+ items
- [x] Coil image caching and downsampling to prevent OOM
- [x] Real-time presence and debounced typing indicators
- [x] Offline outbox queue with seamless sync on reconnection

## 5. Play Store Readiness
- [x] Unique application ID: `com.aistudio.bitchat.qvxkpz`
- [x] Target SDK 36 (Android 15 / 16 Preview compatible)
- [x] Min SDK 24 (Android 7.0+)
- [x] Custom adaptive launcher icon configured
