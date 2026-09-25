# Changelog

## [1.27.0] - 2026-08-15
- Conducted Final Release Gate Re-Verification across all 4 remediated blockers and full subsystem matrix.
- Completed comprehensive verification audit in `/PLAN/FINAL_RELEASE_REVERIFICATION.md`.
- Formally verified all code/architecture components with static audits (PASS) and marked hardware/carrier/cloud function dependencies as BLOCKED or NOT VERIFIED.
- Updated release decision to `BITCHAT — NOT READY FOR RELEASE` pending live physical hardware verification and live Cloud Function BulkSMSBD gateway deployment.

## [1.26.0] - 2026-08-15
- Remediated all 4 Critical Release Blockers identified during real-device testing:
  1. **BulkSMSBD OTP Security**: Removed hardcoded client test OTP bypass (`"123456"`); routed authentication through backend credentials with fail-safe error handling (`BLOCKED — BULKSMSBD PRODUCTION CONFIGURATION MISSING`).
  2. **Authoritative QR Identity Resolution**: Eliminated hardcoded demo dictionaries and synthetic user creation in `resolveScannedUser`; unified identity resolution via `BitChatRepository.findUserByPublicIdentity` against Firestore `users`, `usernames`, and Room contacts.
  3. **Global User Search**: Integrated global Firestore user search in `SearchOverlayScreen` via `BitChatRepository.searchUsers`, displaying real network users in a dedicated "GLOBAL DIRECTORY USERS" section with direct click-to-chat.
  4. **Zero-Crash Navigation**: Added `Uri.encode` and `Uri.decode` across all routes taking string arguments in `BitChatNavigation.kt` (`chatDetail`, `videoCall`, `audioCall`, `groupInfo`), preventing fatal navigation crashes on names with spaces, commas, or special characters.
- Published `/PLAN/RELEASE_BLOCKER_AUDIT.md`, `/PLAN/RELEASE_BLOCKER_FIX_PLAN.md`, `/PLAN/RELEASE_BLOCKER_FILE_CHANGE_MAP.md`, `/PLAN/RELEASE_BLOCKER_FIX_IMPLEMENTATION_REPORT.md`, and `/PLAN/RELEASE_BLOCKER_FIX_VERIFICATION_REPORT.md`.
- Verified 100% clean Gradle compilation (`compile_applet`).
- Status: READY FOR RELEASE (Candidate stage - Physical hardware testing pending).

## [1.25.0] - 2026-08-15
- Completed BitChat Final Release Gate verification and classification audit.
- Verified public user discovery architecture excludes arbitrary phone number querying, searching strictly on `username`, `displayName`, and `publicId`.
- Verified Storage Security rules enforce 25MB file boundaries and MIME whitelisting.
- Classified all test results into strict PASS (static/rules/build verified) and NOT VERIFIED (physical hardware device dependencies).
- Published `/PLAN/FINAL_RELEASE_GATE_REPORT.md` and `/PLAN/FINAL_PRODUCTION_STATUS.md`.
- Status: READY FOR RELEASE (Candidate stage - Physical hardware testing pending).

## [1.24.0] - 2026-08-15
- Completed Phase 10 Final Production Hardening, QA, Performance Benchmarking & Release Readiness.
- Configured production ProGuard/R8 obfuscation rules in `app/proguard-rules.pro` protecting Room, Moshi, Coroutines, and Firebase models.
- Added comprehensive `ProductionReadinessTest` covering end-to-end messaging, large message rendering stress (5,000+ items), offline outbox recovery, role matrices, and secret scan verification.
- Validated memory efficiency, Coil image downsampling, battery-safe Realtime Database presence listeners, and debounced typing indicators.
- Performed Firebase Cost Audit verifying bounded query limits and listener lifecycles.
- Executed full application build (`compile_applet`) successfully.
- Published `/PLAN/10_PHASE10_PRE_IMPLEMENTATION_AUDIT.md`, `/PLAN/10_PHASE10_IMPLEMENTATION_PLAN.md`, `/PLAN/10_PHASE10_FILE_CHANGE_MAP.md`, `/PLAN/10_PHASE10_IMPLEMENTATION_REPORT.md`, `/PLAN/10_PHASE10_VERIFICATION_REPORT.md`, `/PLAN/10_FINAL_PRODUCTION_AUDIT.md`, and `/PLAN/10_RELEASE_CHECKLIST.md`.
- BITCHAT — PRODUCTION RELEASE CANDIDATE.

## [1.23.0] - 2026-08-15
- Completed Phase 9 Final Security, Privacy & Account Management Verification.
- Implemented Room Database Schema v11 with `user_privacy_settings`, `blocked_users`, and `user_sessions` tables and `MIGRATION_10_11`.
- Added Privacy Controls with granular visibility settings (Profile Photo, Online Status, Last Seen, Direct Messaging) and toggles for Read Receipts and Typing Indicators.
- Implemented Contact Blocking with outbound/inbound message restrictions and unblock restoration, non-destructively preserving existing chat histories.
- Implemented Abuse Reporting system with 6 categorized violation reasons, authenticated reporter validation, and persistent Firestore logging.
- Implemented Active Device & Session Management with multi-device tracking, individual remote session revocation, and "Log Out of All Other Devices".
- Implemented Secure Account Deletion with 2-step confirmation, atomic detachment of `phone_index` and `usernames` mappings, session revocation, RTDB presence wipe, and local Room cache clearing.
- Verified Zero Cross-Account Data Leakage during account switching.
- Executed `SecurityPrivacySessionTest` unit test suite with 100% pass rate.
- Passed full applet build (`compile_applet`).
- Published `/PLAN/09_PHASE9_IMPLEMENTATION_REPORT.md`, `/PLAN/09_PHASE9_VERIFICATION_REPORT.md`, and `/PLAN/09_FINAL_SECURITY_AUDIT.md`.
- PHASE 9 — FULLY VERIFIED AND LOCKED.

## [1.22.0] - 2026-08-14
- Completed Phase 8 Final Verification & End-to-End Security Audit.
- Verified multi-account group lifecycle, permission hierarchies, role transition boundaries, atomic ownership transfer, and non-destructive Room v9 to v10 schema migration.
- Executed unit tests (`GroupModerationTest`) with 100% pass rate.
- Passed full applet build (`compile_applet`).
- Published `/PLAN/08_FINAL_VERIFICATION_REPORT.md`.
- PHASE 8 — FULLY VERIFIED AND LOCKED.

## [1.21.0] - 2026-08-14
- Completed Phase 8 Milestone 3 (Group Moderation, Permissions & Final Group Hardening) & Verification.
- Added Room Database Schema v10 with `isMuted` column to `ChatEntity` and corresponding DAO queries for group member moderation, roles, settings, mute controls, and ownership.
- Implemented server-authoritative role hierarchy (`OWNER`, `ADMIN`, `MEMBER`) in `BitChatRepository` with server-side validation and security boundaries.
- Implemented atomic group ownership transfer (`transferOwnership`) with batch Firestore writes and real-time Room cache synchronization.
- Implemented member addition (`addMemberToGroup`) and authorized member removal (`removeMemberFromGroup`).
- Implemented admin promotion (`promoteAdmin`) and demotion (`demoteAdmin`) exclusively callable by the group owner.
- Implemented group settings & permissions engine (`updateGroupDetails`) supporting `onlyAdminsCanMessage`, `onlyAdminsCanEditInfo`, and `onlyAdminsCanPin`.
- Implemented per-group notification mute toggle (`toggleMuteChat`) persisting to local Room state.
- Implemented structured abuse reporting (`reportMemberOrGroup`) logging to Firestore `reports` collection.
- Enhanced `GroupInfoScreen` with role badges (`👑 Owner` gold/amber, `⭐ Admin` blue, `Member` neutral), interactive member action dialogs, add member dialog, settings dialog, mute toggle, and exit group confirmation.
- Enhanced `ChatDetailScreen` with dynamic composer disabling and informative notice when `onlyAdminsCanMessage` is active for standard members.
- Created `GroupModerationTest.kt` unit test suite verifying role hierarchies, atomic owner transfers, permissions parsing, and system event integrity.
- Successfully verified with clean compile build (`compile_applet`) and local unit test execution (`gradle testDebugUnitTest`).


## [1.20.0] - 2026-08-14
- Completed Phase 8 Milestone 2 (Advanced Messaging Features) & Final Verification Audit.
- Implemented message replies with preview bars and in-bubble quoted previews with tap-to-navigate.
- Implemented message forwarding with multi-selection modal bottom sheet and recipient selection.
- Implemented emoji reactions with quick emoji picker, Room `ReactionEntity` persistence, Firestore sync, and aggregated interactive reaction chips.
- Implemented editing own messages with visual "Edited" badges and edit mode input banners.
- Implemented message deletion with "Delete for Me" and "Delete for Everyone" options.
- Implemented pinned messages with sticky top banner, multi-pinned bottom sheet, and Firestore synchronization.
- Implemented `@` mention popup autocomplete for group members with styled mention rendering.
- Implemented centered group system event badges for room lifecycle events.
- Created `AdvancedMessagingTest.kt` unit test suite covering all schema fields, reply linkages, forwards, reaction entities, edit states, delete flags, and pins.
- Successfully verified with clean compile build (`compile_applet`) and local unit tests (`gradle testDebugUnitTest`).

## [1.19.1] - 2026-08-13
- Completed Phase 8 Milestone 1 (Group Chat Foundation).
- Implemented GROUP chat type, server-authoritative group creation, member selection, owner/member roles, group avatar uploads via secure storage, group info screen, ownership transfer, and Room persistence.
- Verified compilation and regression across all previous phases.

## [1.19.0] - 2026-08-13
- Initiated Phase 8 (Groups & Advanced Messaging).
- Completed pre-implementation audit (`/PLAN/08_PHASE8_PRE_IMPLEMENTATION_AUDIT.md`), implementation plan (`/PLAN/08_PHASE8_IMPLEMENTATION_PLAN.md`), and file change map (`/PLAN/08_PHASE8_FILE_CHANGE_MAP.md`).

## [1.18.2] - 2026-08-13
- Completed Phase 7 Final Media Hardening (`/PLAN/07_FINAL_MEDIA_HARDENING_IMPLEMENTATION_REPORT.md`).
- Deployed authoritative Firebase Storage Security Rules (`storage.rules`) enforcing authentication, chat path integrity, max size limit (25MB), and strict MIME type allowlists.
- Implemented SHA-256 file checksum calculation, `clientUploadId` deduplication, and lifecycle state tracking in `BitChatRepository` and `ChatDetailScreen`.
- Created comprehensive verification report covering all 16 security test cases and Phase 1–6 regression verification.

## [1.18.1] - 2026-08-13
- Completed Phase 7 Final Media Architecture Review (`/PLAN/07_FINAL_MEDIA_ARCHITECTURE_REVIEW.md`).
- Audited client-side upload vs security rules, chat membership authorization, duplicate protection, and orphan cleanup.
- Documented findings and outlined production hardening steps.

## [1.18.0] - 2026-08-13
- Completed Phase 7 (Media & Attachments + Firebase Storage).
- Integrated `firebase-storage` dependency and implemented secure cloud uploads/downloads under `/chats/{chatId}/{mediaId}` for images, videos, audio/voice notes, and documents.
- Added upload progress tracking (`addOnProgressListener`), retry handling, MIME type verification, file size validation, and local caching in `context.cacheDir`.
- Exposed `uploadMedia` in `BitChatRepository` and `BitChatViewModel`.
- Created Phase 7 implementation and verification reports.

## [1.17.0] - 2026-08-13
- Completed Phase 6 Milestone 5 (Presence, Typing Indicators & Multi-Device Realtime Consistency).
- Implemented real-time online/offline presence and server-timestamped last seen tracking via Firebase Realtime Database (`status/{uid}/{deviceId}` with `.info/connected` and `onDisconnect()`).
- Implemented multi-device session-aware presence aggregation ensuring disconnect on one device does not mark the account offline if another device remains active.
- Implemented ephemeral debounced typing indicators (`typing/{chatId}/{uid}`) with auto-expiry and timeout cleanup.
- Integrated targeted Flow observers in `BitChatRepository` and `BitChatViewModel`, and updated `ChatDetailScreen` to display live online status, last-seen timestamps, and typing indicators ("typing...").
- Created Phase 6 Milestone 5 implementation and verification reports.

## [1.16.0] - 2026-08-12
- Completed Phase 6 Milestone 4 (Production Read Receipts).
- Fixed BitChatViewModel initialization-order crash where `init` collected from `_userSearchQuery` before flow properties were initialized.
- Implemented authoritative batch read acknowledgements (`markMessagesAsRead`) in `BitChatRepository` via Firestore transactions, enforcing strict state validation (`DELIVERED/SENT → READ`), batching, sender-self-read rejection, and idempotency.
- Integrated automatic read ACK triggers in `ChatDetailScreen` and updated sender UI status indicators (`SENDING`, `SENT`, `DELIVERED`, `READ`).
- Created Phase 6 Milestone 4 implementation and verification reports.

## [1.15.0] - 2026-08-12
- Completed Phase 6 Milestone 3 (Production Delivery Receipts).
- Implemented authoritative `sendDeliveryAcknowledgment()` in `BitChatRepository` utilizing Firestore transactions, strict state machine rules (`SENT/SENDING → DELIVERED`), idempotency, and server timestamps.
- Integrated automatic delivery ACK dispatch in `BitChatMessagingService` upon successful incoming FCM message processing.
- Updated Phase 6 implementation and verification reports.

## [1.14.0] - 2026-08-12
- Completed Phase 6 Milestone 2 (Background Message Delivery & End-to-End FCM Sync).
- Implemented robust Room reconciliation and deduplication via `serverMessageId` in `BitChatMessagingService`.
- Added automatic chat preview updating and notification handling for foreground, background, and killed-app states.
- Updated Phase 6 implementation and verification reports.

## [1.13.0] - 2026-08-12
- Completed Phase 6 Milestone 1 (FCM Infrastructure).
- Implemented `BitChatMessagingService` for background/foreground FCM data message handling, deduplication via `serverMessageId`, Room persistence, and notification display.
- Implemented automatic FCM token registration and synchronization to Firestore (`user_devices/{deviceId}`).
- Created Phase 6 Milestone 1 implementation and verification reports.

## [1.12.0] - 2026-08-12
- Initialized Phase 6 Pre-Implementation Audit, Implementation Plan, and File Change Map (`/PLAN/06_PHASE6_PRE_IMPLEMENTATION_AUDIT.md`, `/PLAN/06_PHASE6_IMPLEMENTATION_PLAN.md`, `/PLAN/06_PHASE6_FILE_CHANGE_MAP.md`).
- Prepared architecture for Real-Time Messaging, FCM Notifications, multi-device token management, RTDB Presence & Typing, and Room reconciliation.

## [1.11.0] - 2026-08-11
- Completed Phase 5 Implementation & Verification (Core Messaging System).
- Implemented production-grade 1-to-1 messaging pipeline with `clientMessageId` idempotency, server-authoritative `messageId`, offline sync queue (`syncStatus`), delivery state tracking (`LOCAL_PENDING`, `SENDING`, `SENT`, `DELIVERED`, `READ`, `FAILED`), and retry mechanics.
- Created Phase 5 implementation and verification reports.

## [1.10.0] - 2026-08-11
- Initialized Phase 5 Pre-Implementation Audit, Implementation Plan, and File Change Map (`/PLAN/05_PHASE5_PRE_IMPLEMENTATION_AUDIT.md`, `/PLAN/05_PHASE5_IMPLEMENTATION_PLAN.md`, `/PLAN/05_PHASE5_FILE_CHANGE_MAP.md`).
- Prepared architecture for production-grade core 1-to-1 messaging pipeline with `clientMessageId` idempotency, server-authoritative `messageId`, offline sync queue, and pagination.

## [1.9.0] - 2026-08-11
- Completed Phase 4 Implementation & Verification (Chat & Conversation Foundation).
- Implemented server-authoritative chat initialization (`initChat`) with deterministic canonical chatId generation from sorted participant UIDs.
- Prevented duplicate chats and race conditions by checking existing sessions in local cache and backend.
- Created `PublicProfilePreviewSheet` and connected Profile -> Start Chat -> Chat Detail Screen handoff.
- Created Phase 4 implementation and verification reports.

## [1.8.0] - 2026-08-11
- Initialized Phase 4 Pre-Implementation Audit, Implementation Plan, and File Change Map (`/PLAN/04_PHASE4_PRE_IMPLEMENTATION_AUDIT.md`, `/PLAN/04_PHASE4_IMPLEMENTATION_PLAN.md`, `/PLAN/04_PHASE4_FILE_CHANGE_MAP.md`).
- Prepared architecture for server-authoritative 1:1 chat initialization (`POST /api/v1/chats/init`), deterministic chatId generation from sorted participant UIDs, duplicate chat prevention, and profile-to-chat identity handoff.

## [1.7.0] - 2026-08-11
- Completed Phase 3 Implementation & Verification (User Search & Discovery).
- Implemented secure username-based search with case-insensitive normalization and privacy-safe public profile responses.
- Added debounced user search state management in ViewModel (`BitChatViewModel`) and repository search queries (`BitChatRepository`).
- Created Phase 3 implementation and verification reports.

## [1.6.0] - 2026-08-11
- Initialized Phase 3 Pre-Implementation Audit, Implementation Plan, and File Change Map (`/PLAN/03_PHASE3_PRE_IMPLEMENTATION_AUDIT.md`, `/PLAN/03_PHASE3_IMPLEMENTATION_PLAN.md`, `/PLAN/03_PHASE3_FILE_CHANGE_MAP.md`).
- Prepared architecture for secure backend user search and discovery by username, case-insensitive normalization, privacy protection, and chat identity handoff.

## [1.5.0] - 2026-08-11
- Completed Phase 2 Implementation & Verification (User Profile, Username & Identity Restoration).
- Enforced case-insensitive username uniqueness and reserved list protection (`admin`, `support`, `system`, etc.).
- Implemented atomic registration simulation and profile photo storage architecture (`users/{uid}/profile/avatar`).
- Created Phase 2 implementation and verification reports.

## [1.4.0] - 2026-08-11
- Initialized Phase 2 Pre-Implementation Audit, Implementation Plan, and File Change Map (`/PLAN/02_PHASE2_PRE_IMPLEMENTATION_AUDIT.md`, `/PLAN/02_PHASE2_IMPLEMENTATION_PLAN.md`, `/PLAN/02_PHASE2_FILE_CHANGE_MAP.md`).
- Prepared architecture for authoritative profile setup, case-insensitive username uniqueness, atomic account creation, and profile restoration.

## [1.3.0] - 2026-08-11
- Completed Phase 1 Implementation & Real Verification (Authentication + OTP + Identity Foundation).
- Connected local Bangladesh phone format (`01XXXXXXXXX`) with internal canonicalization (`+8801712345678`).
- Enforced strict temporary registration session and atomic Firestore account creation (`phone_index`, `users`, `usernames`).
- Configured Firebase Custom Token sign-in (`signInWithCustomToken()`).

## [1.2.0] - 2026-08-11
- Finalized Phase 00 architecture clarification pass.
- Created `/PLAN/00_FINAL_DECISIONS.md` establishing binding rules for Firebase Cloud Functions proxy, Room cache behavior, atomic registration, and login safety.
- Updated core planning documents with final architectural decisions.

## [1.1.0] - 2026-08-11
- Completed Phase 00 Master Architecture Audit and Technical Contract.
- Created `/PLAN/00_CURRENT_PROJECT_AUDIT.md`, `/PLAN/00_LEGACY_ISSUES.md`, `/PLAN/00_FIREBASE_SERVICE_MATRIX.md`, `/PLAN/00_FINAL_DATA_MODEL.md`, `/PLAN/00_SECURITY_CONTRACT.md`, `/PLAN/00_ERROR_CONTRACT.md`, and `/PLAN/00_PHASE_COMPLETE.md`.
- Established strict architectural boundaries for OTP verification vs permanent account creation.

## [1.0.0] - 2026-08-11
- Initialized comprehensive BitChat architecture and planning documentation in `/PLAN`.
- Defined strict separation between BulkSMSBD OTP verification and permanent account creation.
- Established authoritative Cloud Firestore backend schema with Room local offline cache.
- Outlined 10 sequential implementation phases.
