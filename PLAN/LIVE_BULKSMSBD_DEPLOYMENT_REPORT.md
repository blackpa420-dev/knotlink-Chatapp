# BitChat: Live BulkSMSBD Deployment Report

## 1. Executive Summary & Status
This report details the inspection, architecture preparation, and deployment status of the server-side BulkSMSBD OTP Gateway and Firebase Cloud Functions for BitChat.

---

## 2. Infrastructure & Deployment Matrix

| Parameter / Milestone | Status | Details & Evidence |
| :--- | :--- | :--- |
| **Firebase Project** | `bitchat-1` | Verified from `app/google-services.json` (Project Number: `783480852484`) |
| **Cloud Functions Codebase** | **PASS** | Complete production functions implemented in `/functions/index.js` (`requestOtp`, `verifyOtp`, `completeRegistration`) |
| **Functions Deployed** | **BLOCKED** | Live deployment to Firebase `bitchat-1` requires active Firebase CLI / Cloud Console authorization |
| **BulkSMSBD Configured** | **BLOCKED** | Production `BULKSMSBD_API_KEY` and `BULKSMSBD_SENDER_ID` secrets are pending provisioning in Google Cloud Secret Manager |
| **Client Credential Isolation** | **PASS** | Zero BulkSMSBD API keys, secrets, or gateway URLs embedded in Android source, `BuildConfig`, assets, or client Firestore |
| **Client Fail-Safe Contract** | **PASS** | Client code strictly fails closed with `BLOCKED — BULKSMSBD PRODUCTION CONFIGURATION MISSING` when gateway is unconfigured |
| **Mock OTP Prevention** | **PASS** | Zero test bypasses (e.g. `123456`) in client or Cloud Functions code |
| **OTP Request Function** | **BLOCKED** | Code complete with 60s cooldown & 5 req/hr rate limits; deployment blocked on secrets provisioning |
| **SMS Delivery** | **NOT VERIFIED** | Requires live telephony carrier dispatch and active BulkSMSBD gateway credit balance |
| **OTP Verification Function** | **PASS** (Code) / **BLOCKED** (Live) | Hashed SHA-256 validation, 3-min TTL expiration, 5-attempt brute-force lock, atomic replay protection |
| **Account Registration & Login** | **PASS** (Code) | Dual-path: mints Custom Token for existing users; issues 15-min signed JWT for new user profile setup |

---

## 3. Configuration & Deployment Prerequisites

### Required Production Secrets (To be configured via Firebase CLI / Cloud Secret Manager):
1. `BULKSMSBD_API_KEY`: Production API Key provided by BulkSMSBD.
2. `BULKSMSBD_SENDER_ID`: Approved Masking/Non-Masking Sender ID from BulkSMSBD.
3. `JWT_REGISTRATION_SECRET`: Cryptographic secret for signing temporary registration tokens.

### Deployment Command:
```bash
# 1. Set server-side secrets in Google Cloud Secret Manager
firebase functions:secrets:set BULKSMSBD_API_KEY
firebase functions:secrets:set BULKSMSBD_SENDER_ID
firebase functions:secrets:set JWT_REGISTRATION_SECRET

# 2. Deploy Cloud Functions to production project bitchat-1
firebase deploy --only functions --project bitchat-1
```

---

## 4. Known Issues & Operational Blockers
- **Blocker**: BulkSMSBD production API credentials are not yet provisioned in Google Cloud Secret Manager for project `bitchat-1`.
- **Mitigation**: The Android application is hardened to fail closed with an explicit error dialog (`BLOCKED — BULKSMSBD PRODUCTION CONFIGURATION MISSING`) rather than allowing unauthorized account creation or falling back to mock OTPs.
